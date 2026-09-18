package com.laul.lauwell

import android.app.Application

/**
 * Application entry point.
 *
 * Holds process-wide singletons (database, repositories) once they exist.
 * For now this just establishes the class so DI/wiring has a home.
 */
class LauWellApplication : Application()
