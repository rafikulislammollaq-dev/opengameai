package ch.duartesantos.opengym

import android.app.Application
import ch.duartesantos.opengym.data.db.AppDatabase
import ch.duartesantos.opengym.data.repository.GymRepository

class OpenGymApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: GymRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)
        repository = GymRepository(database, this)
    }
}
