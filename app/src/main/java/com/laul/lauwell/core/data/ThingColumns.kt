package com.laul.lauwell.core.data

import androidx.room.ColumnInfo
import com.laul.lauwell.core.thing.Product
import com.laul.lauwell.core.thing.Resource
import com.laul.lauwell.core.thing.Thing

// The shared columns of the thing tables (db-schema/schema-tables.md → 4), embedded by each kind's entity,
// which keeps its own `@PrimaryKey val id: String` and adds its own columns:
//
//   @Entity(tableName = "medication")
//   data class MedicationEntity(
//       @PrimaryKey val id: String,
//       @Embedded val thing: ThingColumns,
//       @Embedded val product: ProductColumns,
//       val strength: String?, …
//   )
//
// A Products table embeds ProductColumns, a Resources table ResourceColumns, never both. Times are epoch
// milliseconds; column names match Postgres.

/** Columns every thing table has. [deletedAt] is only for a thing created by mistake that nothing points to. */
data class ThingColumns(
    val name: String,
    val note: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "edited_at") val editedAt: Long? = null,
    @ColumnInfo(name = "not_used_since") val notUsedSince: Long? = null,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
)

/** The Products family's columns. */
data class ProductColumns(
    val brand: String? = null,
    @ColumnInfo(name = "reference_code") val referenceCode: String? = null,
    val form: String? = null,
)

/** The Resources family's columns. */
data class ResourceColumns(
    val type: String? = null,
    val address: String? = null,
    val phone: String? = null,
    val website: String? = null,
    @ColumnInfo(name = "contact_person") val contactPerson: String? = null,
)

internal fun thingColumns(thing: Thing) = ThingColumns(
    name = thing.name,
    note = thing.note,
    createdAt = thing.createdAt.toEpochMilli(),
    editedAt = thing.editedAt?.toEpochMilli(),
    notUsedSince = thing.notUsedSince?.toEpochMilli(),
)

internal fun productColumns(product: Product) = ProductColumns(product.brand, product.referenceCode, product.form)

internal fun resourceColumns(resource: Resource) =
    ResourceColumns(resource.type, resource.address, resource.phone, resource.website, resource.contactPerson)
