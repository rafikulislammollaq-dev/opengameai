package ch.duartesantos.opengym.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun getAllExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :id LIMIT 1")
    suspend fun getExerciseById(id: String): ExerciseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<ExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ExerciseEntity)

    @Update
    suspend fun updateExercise(exercise: ExerciseEntity)

    @Query("UPDATE exercises SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: String, isFavorite: Boolean)

    @Delete
    suspend fun deleteExercise(exercise: ExerciseEntity)
}

@Dao
interface WorkoutSessionDao {
    @Query("SELECT * FROM workout_sessions WHERE isFinished = 1 ORDER BY startTimeEpochMs DESC")
    fun getAllCompletedWorkouts(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions WHERE isFinished = 0 LIMIT 1")
    fun getActiveWorkout(): Flow<WorkoutSessionEntity?>

    @Query("SELECT * FROM workout_sessions WHERE id = :id LIMIT 1")
    suspend fun getWorkoutById(id: String): WorkoutSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: WorkoutSessionEntity)

    @Update
    suspend fun updateWorkout(workout: WorkoutSessionEntity)

    @Query("DELETE FROM workout_sessions WHERE id = :id")
    suspend fun deleteWorkoutById(id: String)
}

@Dao
interface ExerciseSetDao {
    @Query("SELECT * FROM exercise_sets WHERE workoutSessionId = :workoutId ORDER BY setNumber ASC")
    fun getSetsForWorkout(workoutId: String): Flow<List<ExerciseSetEntity>>

    @Query("SELECT * FROM exercise_sets WHERE workoutSessionId = :workoutId ORDER BY setNumber ASC")
    suspend fun getSetsForWorkoutSync(workoutId: String): List<ExerciseSetEntity>

    @Query("SELECT * FROM exercise_sets WHERE exerciseId = :exerciseId AND isCompleted = 1 ORDER BY (weightKg * (1 + reps/30.0)) DESC LIMIT 1")
    suspend fun getBestSetForExercise(exerciseId: String): ExerciseSetEntity?

    @Query("SELECT * FROM exercise_sets WHERE exerciseId = :exerciseId AND isCompleted = 1")
    fun getAllSetsForExercise(exerciseId: String): Flow<List<ExerciseSetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(set: ExerciseSetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSets(sets: List<ExerciseSetEntity>)

    @Update
    suspend fun updateSet(set: ExerciseSetEntity)

    @Delete
    suspend fun deleteSet(set: ExerciseSetEntity)

    @Query("DELETE FROM exercise_sets WHERE workoutSessionId = :workoutId AND exerciseId = :exerciseId")
    suspend fun deleteSetsForExercise(workoutId: String, exerciseId: String)

    @Query("DELETE FROM exercise_sets WHERE workoutSessionId = :workoutId")
    suspend fun deleteSetsForWorkout(workoutId: String)
}

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routines ORDER BY dayOfWeek ASC, name ASC")
    fun getAllRoutines(): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM routines WHERE id = :id LIMIT 1")
    suspend fun getRoutineById(id: String): RoutineEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutines(routines: List<RoutineEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: RoutineEntity)

    @Update
    suspend fun updateRoutine(routine: RoutineEntity)

    @Query("DELETE FROM routines WHERE id = :id")
    suspend fun deleteRoutineById(id: String)
}

@Dao
interface BodyWeightDao {
    @Query("SELECT * FROM body_weight_entries ORDER BY timestampEpochMs DESC")
    fun getAllWeightEntries(): Flow<List<BodyWeightEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeight(entry: BodyWeightEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeights(entries: List<BodyWeightEntity>)

    @Query("DELETE FROM body_weight_entries WHERE id = :id")
    suspend fun deleteWeightById(id: String)
}

@Dao
interface UserSettingsDao {
    @Query("SELECT * FROM user_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<UserSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: UserSettingsEntity)
}
