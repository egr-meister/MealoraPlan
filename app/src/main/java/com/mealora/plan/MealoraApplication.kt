package com.mealora.plan

import android.app.Application
import com.mealora.plan.data.MealoraRepository

/**
 * Application entry point. Owns the single repository instance. There is no
 * dependency-injection framework — a simple manual container keeps the app
 * stable and easy to reason about.
 */
class MealoraApplication : Application() {

    lateinit var repository: MealoraRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = MealoraRepository(applicationContext)
    }
}
