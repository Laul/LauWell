package com.laul.lauwell.core.di

import android.content.Context
import androidx.credentials.CredentialManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.laul.lauwell.BuildConfig
import com.laul.lauwell.core.auth.AuthRepository
import com.laul.lauwell.core.auth.FirebaseAuthRepository

/**
 * Manual dependency injection: the one place that builds the app's process-wide singletons
 * (see PLAN.md → Decisions → Dependency injection).
 *
 * Created once in [com.laul.lauwell.LauWellApplication.onCreate]. Everything is `by lazy`, so a
 * dependency is only built the first time something asks for it. Classes never reach in here
 * themselves: they receive what they need through their constructor, and ViewModels get it via
 * [appViewModel].
 */
class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    val credentialManager: CredentialManager by lazy { CredentialManager.create(appContext) }

    val authRepository: AuthRepository by lazy {
        FirebaseAuthRepository(firebaseAuth, credentialManager, BuildConfig.GOOGLE_WEB_CLIENT_ID)
    }
}
