package ch.duartesantos.opengym.data.repository

import android.content.Context
import ch.duartesantos.opengym.data.db.*
import ch.duartesantos.opengym.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class GymRepository(
    private val database: AppDatabase,
    private val context: Context
) {
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    private val exerciseDao = database.exerciseDao()
    private val workoutSessionDao = database.workoutSessionDao()
    private val exerciseSetDao = database.exerciseSetDao()
    private val routineDao = database.routineDao()
    private val bodyWeightDao = database.bodyWeightDao()
    private val userSettingsDao = database.userSettingsDao()

    // SETTINGS FLOW
    private val _settingsState = MutableStateFlow(UserSettings())
    val settingsState: StateFlow<UserSettings> = _settingsState.asStateFlow()

    init {
        repositoryScope.launch {
            userSettingsDao.getSettings().collect { entity ->
                if (entity != null) {
                    _settingsState.value = entity.toDomain()
                } else {
                    val default = UserSettings()
                    userSettingsDao.insertSettings(UserSettingsEntity.fromDomain(default))
                    _settingsState.value = default
                }
            }
        }
    }

    suspend fun updateSettings(settings: UserSettings) {
        userSettingsDao.insertSettings(UserSettingsEntity.fromDomain(settings))
        _settingsState.value = settings
    }

    // EXERCISES
    val allExercises: Flow<List<Exercise>> = exerciseDao.getAllExercises()
        .map { list -> list.map { it.toDomain() } }

    suspend fun toggleFavoriteExercise(exerciseId: String) {
        val ex = exerciseDao.getExerciseById(exerciseId) ?: return
        exerciseDao.updateFavorite(exerciseId, !ex.isFavorite)
    }

    suspend fun saveExercise(exercise: Exercise) {
        exerciseDao.insertExercise(ExerciseEntity.fromDomain(exercise))
    }

    // ROUTINES
    val allRoutines: Flow<List<Routine>> = routineDao.getAllRoutines()
        .map { list -> list.map { it.toDomain() } }

    suspend fun saveRoutine(routine: Routine) {
        routineDao.insertRoutine(RoutineEntity.fromDomain(routine))
    }

    suspend fun deleteRoutine(routineId: String) {
        routineDao.deleteRoutineById(routineId)
    }

    // WORKOUTS
    val allCompletedWorkouts: Flow<List<WorkoutSession>> = workoutSessionDao.getAllCompletedWorkouts()
        .map { list -> list.map { it.toDomain() } }

    val activeWorkoutSession: Flow<WorkoutSession?> = workoutSessionDao.getActiveWorkout()
        .map { it?.toDomain() }

    fun getSetsForWorkout(workoutId: String): Flow<List<ExerciseSet>> =
        exerciseSetDao.getSetsForWorkout(workoutId)
            .map { list -> list.map { it.toDomain() } }

    suspend fun startWorkout(routine: Routine?, customName: String? = null): WorkoutSession {
        val workoutId = UUID.randomUUID().toString()
        val name = customName ?: (routine?.name ?: "Quick Workout")
        val session = WorkoutSession(
            id = workoutId,
            name = name,
            routineId = routine?.id,
            startTimeEpochMs = System.currentTimeMillis(),
            isFinished = false
        )
        workoutSessionDao.insertWorkout(WorkoutSessionEntity.fromDomain(session))

        // If started from routine, seed sets for each exercise in the routine
        routine?.exerciseIds?.forEach { exId ->
            val defaultSets = listOf(
                ExerciseSet(workoutSessionId = workoutId, exerciseId = exId, setNumber = 1, weightKg = 60f, reps = 10),
                ExerciseSet(workoutSessionId = workoutId, exerciseId = exId, setNumber = 2, weightKg = 60f, reps = 10),
                ExerciseSet(workoutSessionId = workoutId, exerciseId = exId, setNumber = 3, weightKg = 60f, reps = 8)
            )
            exerciseSetDao.insertSets(defaultSets.map { ExerciseSetEntity.fromDomain(it) })
        }

        return session
    }

    suspend fun addExerciseToWorkout(workoutId: String, exerciseId: String, defaultSets: Int = 3) {
        val existingSets = exerciseSetDao.getSetsForWorkoutSync(workoutId).filter { it.exerciseId == exerciseId }
        val startNumber = existingSets.size + 1
        val newSets = (0 until defaultSets).map { index ->
            ExerciseSet(
                workoutSessionId = workoutId,
                exerciseId = exerciseId,
                setNumber = startNumber + index,
                weightKg = 50f,
                reps = 10
            )
        }
        exerciseSetDao.insertSets(newSets.map { ExerciseSetEntity.fromDomain(it) })
    }

    suspend fun addSetToExercise(workoutId: String, exerciseId: String, previousSet: ExerciseSet? = null) {
        val existingSets = exerciseSetDao.getSetsForWorkoutSync(workoutId).filter { it.exerciseId == exerciseId }
        val nextNumber = (existingSets.maxOfOrNull { it.setNumber } ?: 0) + 1
        val newSet = ExerciseSet(
            workoutSessionId = workoutId,
            exerciseId = exerciseId,
            setNumber = nextNumber,
            weightKg = previousSet?.weightKg ?: 60f,
            reps = previousSet?.reps ?: 10,
            type = SetType.NORMAL
        )
        exerciseSetDao.insertSet(ExerciseSetEntity.fromDomain(newSet))
    }

    suspend fun updateSet(set: ExerciseSet) {
        exerciseSetDao.updateSet(ExerciseSetEntity.fromDomain(set))
    }

    suspend fun deleteSet(set: ExerciseSet) {
        exerciseSetDao.deleteSet(ExerciseSetEntity.fromDomain(set))
    }

    suspend fun removeExerciseFromWorkout(workoutId: String, exerciseId: String) {
        exerciseSetDao.deleteSetsForExercise(workoutId, exerciseId)
    }

    suspend fun finishActiveWorkout(workoutId: String, notes: String): WorkoutSession? {
        val entity = workoutSessionDao.getWorkoutById(workoutId) ?: return null
        val sets = exerciseSetDao.getSetsForWorkoutSync(workoutId)
        val completedSets = sets.filter { it.isCompleted }

        val totalVolume = completedSets.sumOf { (it.weightKg * it.reps).toDouble() }.toFloat()
        val endTime = System.currentTimeMillis()
        val durationSeconds = ((endTime - entity.startTimeEpochMs) / 1000).coerceAtLeast(0)

        var prCount = 0
        // Detect personal records (PRs)
        completedSets.forEach { s ->
            val bestPrevious = exerciseSetDao.getBestSetForExercise(s.exerciseId)
            val currentOneRm = GymCalculators.calculateOneRm(s.weightKg, s.reps)
            val bestOneRm = bestPrevious?.let { GymCalculators.calculateOneRm(it.weightKg, it.reps) } ?: 0f
            if (currentOneRm > bestOneRm && currentOneRm > 0f) {
                prCount++
                exerciseSetDao.updateSet(s.copy(isPr = true))
            }
        }

        val finishedSession = entity.copy(
            isFinished = true,
            endTimeEpochMs = endTime,
            durationSeconds = durationSeconds,
            totalVolumeKg = totalVolume,
            totalSets = completedSets.size,
            prCount = prCount,
            notes = notes
        )
        workoutSessionDao.updateWorkout(finishedSession)
        return finishedSession.toDomain()
    }

    suspend fun discardActiveWorkout(workoutId: String) {
        exerciseSetDao.deleteSetsForWorkout(workoutId)
        workoutSessionDao.deleteWorkoutById(workoutId)
    }

    suspend fun deleteCompletedWorkout(workoutId: String) {
        exerciseSetDao.deleteSetsForWorkout(workoutId)
        workoutSessionDao.deleteWorkoutById(workoutId)
    }

    // BODY WEIGHT
    val allWeightEntries: Flow<List<BodyWeightEntry>> = bodyWeightDao.getAllWeightEntries()
        .map { list -> list.map { it.toDomain() } }

    suspend fun logBodyWeight(weightKg: Float, bodyFat: Float? = null, note: String = "") {
        val entry = BodyWeightEntity(
            id = UUID.randomUUID().toString(),
            timestampEpochMs = System.currentTimeMillis(),
            weightKg = weightKg,
            bodyFatPercent = bodyFat,
            note = note
        )
        bodyWeightDao.insertWeight(entry)
    }

    suspend fun deleteWeightEntry(id: String) {
        bodyWeightDao.deleteWeightById(id)
    }
}
