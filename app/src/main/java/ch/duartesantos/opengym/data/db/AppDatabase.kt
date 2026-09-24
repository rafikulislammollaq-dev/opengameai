package ch.duartesantos.opengym.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ExerciseEntity::class,
        WorkoutSessionEntity::class,
        ExerciseSetEntity::class,
        RoutineEntity::class,
        BodyWeightEntity::class,
        UserSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun exerciseSetDao(): ExerciseSetDao
    abstract fun routineDao(): RoutineDao
    abstract fun bodyWeightDao(): BodyWeightDao
    abstract fun userSettingsDao(): UserSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "opengym_database.db"
                )
                    .addCallback(DatabaseCallback(context.applicationContext))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val context: Context
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    val database = getDatabase(context)
                    // Prepopulate Exercises
                    database.exerciseDao().insertExercises(PrepopulatedData.defaultExercises)
                    // Prepopulate Routines
                    database.routineDao().insertRoutines(PrepopulatedData.defaultRoutines)
                    // Prepopulate Weight Entries
                    database.bodyWeightDao().insertWeights(PrepopulatedData.getInitialWeightEntries())
                    // Prepopulate Initial Settings
                    database.userSettingsDao().insertSettings(
                        UserSettingsEntity(
                            id = 1,
                            isKg = true,
                            defaultRestSeconds = 90,
                            accentColorHex = "#A3E635",
                            targetWeightKg = 75.0f
                        )
                    )
                }
            }
        }
    }
}
