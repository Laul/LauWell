-- LauWell Postgres schema, version 1: who you are, and what you log (acts).
-- Drafted with Claude, 2026-11-09, from docs/architecture/backend/db-schema/schema-tables.md sections 1 and 3,
-- which stay the reference: change a table there first. Thing tables (medication...) and detail tables come with
-- their feature's migration; sensor tables (reading, daily_summary, origin) with the readings sync task.
--
-- Security model (decision "Persistence", Identity):
--   * The cloud function connects as the login role lauwell_api, never as the owner, and per request runs
--       BEGIN; SELECT app.begin_request('<firebase uid>');  ...  COMMIT;
--     which maps the verified Firebase uid to our own users.id and pins it for the transaction.
--   * Row-level security limits every table to rows whose user_id is that id. Inserts get it by default.
--   * Child rows point at their record through (user_id, record_id), so a foreign key can never reach another
--     person's record (foreign key checks bypass row-level security).
--
-- Sync (decision "Sync: our own"): every change to a record or file takes the next number of one sequence
-- (change_seq). The phone asks for rows with change_seq above the last one it saw. Changes are serialized per
-- user (an advisory lock), so for one person the numbers commit in order and none is skipped. Deletions are soft
-- (deleted_at), so they travel as changes too. A change to a record's values or links renumbers the record.

-- Roles. lauwell_api gets its password outside the repo (Neon console), never in this file.
DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'lauwell_api') THEN
    CREATE ROLE lauwell_api LOGIN;
  END IF;
END
$$;

CREATE SCHEMA app;
GRANT USAGE ON SCHEMA app TO lauwell_api;

-- 1. Who you are ------------------------------------------------------------------------------------------

CREATE TABLE users (
  id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  created_at timestamptz NOT NULL DEFAULT now()
);

-- Changing the sign-in provider later only touches this table.
CREATE TABLE auth_identity (
  provider text NOT NULL,
  subject  text NOT NULL,
  user_id  uuid NOT NULL REFERENCES users (id),
  PRIMARY KEY (provider, subject)
);

-- The person this request acts for; null when begin_request was not called, so a forgotten call reads and
-- writes nothing.
CREATE FUNCTION app.current_user_id() RETURNS uuid
  LANGUAGE sql STABLE
  AS $$ SELECT nullif(current_setting('app.user_id', true), '')::uuid $$;

-- Maps a verified sign-in to users.id, creating the person on first sign-in, and pins it for this transaction.
-- Runs as the owner (security definer): lauwell_api cannot read or write users and auth_identity directly.
CREATE FUNCTION app.begin_request(firebase_uid text) RETURNS uuid
  LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, pg_temp
  AS $$
DECLARE
  uid uuid;
BEGIN
  IF coalesce(firebase_uid, '') = '' THEN
    RAISE EXCEPTION 'no signed-in user';
  END IF;
  SELECT user_id INTO uid FROM auth_identity WHERE provider = 'firebase' AND subject = firebase_uid;
  IF uid IS NULL THEN
    INSERT INTO users DEFAULT VALUES RETURNING id INTO uid;
    INSERT INTO auth_identity (provider, subject, user_id) VALUES ('firebase', firebase_uid, uid)
      ON CONFLICT (provider, subject) DO NOTHING;
    -- Two first requests at once: keep the identity that won, drop the spare user.
    SELECT user_id INTO STRICT uid FROM auth_identity WHERE provider = 'firebase' AND subject = firebase_uid;
  END IF;
  PERFORM set_config('app.user_id', uid::text, true);
  RETURN uid;
END
$$;
REVOKE ALL ON FUNCTION app.begin_request(text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION app.begin_request(text) TO lauwell_api;

-- Sync numbering ------------------------------------------------------------------------------------------

CREATE SEQUENCE app.change_seq;

-- Gives NEW the next change number, after waiting for any other open change by the same person.
CREATE FUNCTION app.number_change() RETURNS trigger
  LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, pg_temp
  AS $$
BEGIN
  PERFORM pg_advisory_xact_lock(hashtextextended(NEW.user_id::text, 0));
  NEW.change_seq := nextval('app.change_seq');
  RETURN NEW;
END
$$;

-- 3. What you log: acts -------------------------------------------------------------------------------------

-- Times are timestamptz; the phone stores epoch milliseconds. shape stores StorageShape's name, source its key.
CREATE TABLE record (
  id             uuid PRIMARY KEY,
  user_id        uuid NOT NULL DEFAULT app.current_user_id() REFERENCES users (id),
  metric_id      text NOT NULL,
  shape          text NOT NULL CHECK (shape IN ('Measurement', 'Event', 'Interval', 'StandingFact', 'CatalogueItem', 'Document', 'Derived')),
  time_utc       timestamptz NOT NULL,
  end_time_utc   timestamptz CHECK (end_time_utc >= time_utc),
  zone_offset_s  int CHECK (zone_offset_s BETWEEN -64800 AND 64800),
  source         text NOT NULL CHECK (source IN ('device', 'lab', 'manual', 'import')),
  recorded_at    timestamptz NOT NULL,
  edited_at      timestamptz,
  deleted_at     timestamptz,
  note           text,
  schema_version int NOT NULL,
  change_seq     bigint NOT NULL,
  UNIQUE (user_id, id)
);
CREATE INDEX record_by_metric_time ON record (user_id, metric_id, time_utc);
CREATE INDEX record_by_change ON record (user_id, change_seq);
CREATE TRIGGER record_number_change BEFORE INSERT OR UPDATE ON record
  FOR EACH ROW EXECUTE FUNCTION app.number_change();

CREATE TABLE record_value (
  user_id      uuid NOT NULL DEFAULT app.current_user_id(),
  record_id    uuid NOT NULL,
  quantity_key text NOT NULL,
  value        double precision NOT NULL CHECK (value <> 'NaN' AND abs(value) <> 'Infinity'),
  PRIMARY KEY (record_id, quantity_key),
  FOREIGN KEY (user_id, record_id) REFERENCES record (user_id, id) ON DELETE CASCADE
);

-- Context links, stored once on the record that points.
CREATE TABLE record_link (
  user_id  uuid NOT NULL DEFAULT app.current_user_id(),
  from_id  uuid NOT NULL,
  to_id    uuid NOT NULL,
  relation text NOT NULL,
  PRIMARY KEY (from_id, to_id, relation),
  FOREIGN KEY (user_id, from_id) REFERENCES record (user_id, id) ON DELETE CASCADE,
  FOREIGN KEY (user_id, to_id) REFERENCES record (user_id, id) ON DELETE CASCADE
);
CREATE INDEX record_link_to ON record_link (user_id, to_id);

-- Values and links travel with their record: any change to them renumbers the record. The trigger's argument
-- names the child's column that holds the record id.
CREATE FUNCTION app.renumber_record() RETURNS trigger
  LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, pg_temp
  AS $$
DECLARE
  child jsonb := to_jsonb(coalesce(NEW, OLD));
BEGIN
  UPDATE record SET change_seq = 0  -- replaced by the next number in record_number_change
    WHERE user_id = (child ->> 'user_id')::uuid AND id = (child ->> TG_ARGV[0])::uuid;
  RETURN NULL;
END
$$;
CREATE TRIGGER record_value_renumber AFTER INSERT OR UPDATE OR DELETE ON record_value
  FOR EACH ROW EXECUTE FUNCTION app.renumber_record('record_id');
CREATE TRIGGER record_link_renumber AFTER INSERT OR UPDATE OR DELETE ON record_link
  FOR EACH ROW EXECUTE FUNCTION app.renumber_record('from_id');

-- A file's metadata; its contents are an object in Cloud Storage.
CREATE TABLE file (
  id           uuid PRIMARY KEY,
  user_id      uuid NOT NULL DEFAULT app.current_user_id() REFERENCES users (id),
  record_id    uuid,
  storage_path text NOT NULL,
  mime_type    text NOT NULL,
  size_bytes   bigint NOT NULL CHECK (size_bytes >= 0),
  uploaded_at  timestamptz NOT NULL,
  deleted_at   timestamptz,
  change_seq   bigint NOT NULL,
  FOREIGN KEY (user_id, record_id) REFERENCES record (user_id, id)
);
CREATE INDEX file_by_change ON file (user_id, change_seq);
CREATE TRIGGER file_number_change BEFORE INSERT OR UPDATE ON file
  FOR EACH ROW EXECUTE FUNCTION app.number_change();

-- Row-level security ----------------------------------------------------------------------------------------
-- No DELETE for the API: acts and files are soft-deleted (deleted_at); only values and links are replaced.

ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE auth_identity ENABLE ROW LEVEL SECURITY;

ALTER TABLE record ENABLE ROW LEVEL SECURITY;
CREATE POLICY own ON record USING (user_id = app.current_user_id()) WITH CHECK (user_id = app.current_user_id());
GRANT SELECT, INSERT, UPDATE ON record TO lauwell_api;

ALTER TABLE record_value ENABLE ROW LEVEL SECURITY;
CREATE POLICY own ON record_value USING (user_id = app.current_user_id()) WITH CHECK (user_id = app.current_user_id());
GRANT SELECT, INSERT, UPDATE, DELETE ON record_value TO lauwell_api;

ALTER TABLE record_link ENABLE ROW LEVEL SECURITY;
CREATE POLICY own ON record_link USING (user_id = app.current_user_id()) WITH CHECK (user_id = app.current_user_id());
GRANT SELECT, INSERT, UPDATE, DELETE ON record_link TO lauwell_api;

ALTER TABLE file ENABLE ROW LEVEL SECURITY;
CREATE POLICY own ON file USING (user_id = app.current_user_id()) WITH CHECK (user_id = app.current_user_id());
GRANT SELECT, INSERT, UPDATE ON file TO lauwell_api;
