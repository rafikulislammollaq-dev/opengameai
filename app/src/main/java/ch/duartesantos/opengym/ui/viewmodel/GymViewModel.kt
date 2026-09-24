package ch.duartesantos.opengym.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ch.duartesantos.opengym.data.model.*
import ch.duartesantos.opengym.data.repository.GymRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class WorkoutExerciseGroup(
    val exercise: Exercise,
    val sets: List<ExerciseSet>
)

class GymViewModel(
    private val repository: GymRepository
) : ViewModel() {

    val settings: StateFlow<UserSettings> = repository.settingsState

    val exercises: StateFlow<List<Exercise>> = repository.allExercises
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val routines: StateFlow<List<Routine>> = repository.allRoutines
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedWorkouts: StateFlow<List<WorkoutSession>> = repository.allCompletedWorkouts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeSession: StateFlow<WorkoutSession?> = repository.activeWorkoutSession
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val bodyWeights: StateFlow<List<BodyWeightEntry>> = repository.allWeightEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active workout sets grouped by exercise
    val activeWorkoutExercises: StateFlow<List<WorkoutExerciseGroup>> = combine(
        activeSession,
        exercises
    ) { session, exerciseList ->
        session to exerciseList
    }.flatMapLatest { (session, exerciseList) ->
        if (session == null) {
            flowOf(emptyList())
        } else {
            repository.getSetsForWorkout(session.id).map { sets ->
                val grouped = sets.groupBy { it.exerciseId }
                // Preserve order of exercises
                grouped.mapNotNull { (exId, exSets) ->
                    val exercise = exerciseList.find { it.id == exId }
                        ?: Exercise(exId, "Unknown Exercise", MuscleGroup.FULL_BODY)
                    WorkoutExerciseGroup(exercise, exSets.sortedBy { it.setNumber })
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Rest timer state
    private val _restTimerRemaining = MutableStateFlow(0)
    val restTimerRemaining: StateFlow<Int> = _restTimerRemaining.asStateFlow()

    private val _restTimerTotal = MutableStateFlow(0)
    val restTimerTotal: StateFlow<Int> = _restTimerTotal.asStateFlow()

    private var restTimerJob: Job? = null

    // Workout duration elapsed timer
    private val _activeWorkoutDurationSeconds = MutableStateFlow(0L)
    val activeWorkoutDurationSeconds: StateFlow<Long> = _activeWorkoutDurationSeconds.asStateFlow()
    private var durationJob: Job? = null

    // Last completed workout summary for post-workout celebration dialog
    private val _finishedWorkoutSummary = MutableStateFlow<WorkoutSession?>(null)
    val finishedWorkoutSummary: StateFlow<WorkoutSession?> = _finishedWorkoutSummary.asStateFlow()

    init {
        // Monitor active workout duration
        viewModelScope.launch {
            activeSession.collect { session ->
                if (session != null) {
                    startDurationTimer(session.startTimeEpochMs)
                } else {
                    durationJob?.cancel()
                    _activeWorkoutDurationSeconds.value = 0L
                }
            }
        }
    }

    private fun startDurationTimer(startTimeMs: Long) {
        durationJob?.cancel()
        durationJob = viewModelScope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                _activeWorkoutDurationSeconds.value = ((now - startTimeMs) / 1000).coerceAtLeast(0)
                delay(1000)
            }
        }
    }

    fun startRestTimer(seconds: Int) {
        restTimerJob?.cancel()
        _restTimerTotal.value = seconds
        _restTimerRemaining.value = seconds

        restTimerJob = viewModelScope.launch {
            while (_restTimerRemaining.value > 0) {
                delay(1000)
                _restTimerRemaining.value -= 1
            }
        }
    }

    fun addRestSeconds(seconds: Int) {
        _restTimerRemaining.value += seconds
        _restTimerTotal.value = maxOf(_restTimerTotal.value, _restTimerRemaining.value)
    }

    fun skipRestTimer() {
        restTimerJob?.cancel()
        _restTimerRemaining.value = 0
    }

    // WORKOUT ACTIONS
    fun startWorkoutFromRoutine(routine: Routine) {
        viewModelScope.launch {
            repository.startWorkout(routine)
        }
    }

    fun startQuickWorkout() {
        viewModelScope.launch {
            repository.startWorkout(null, "Quick Workout")
        }
    }

    fun addExerciseToActiveWorkout(exerciseId: String) {
        val session = activeSession.value ?: return
        viewModelScope.launch {
            repository.addExerciseToWorkout(session.id, exerciseId, 3)
        }
    }

    fun addSet(exerciseId: String, prevSet: ExerciseSet? = null) {
        val session = activeSession.value ?: return
        viewModelScope.launch {
            repository.addSetToExercise(session.id, exerciseId, prevSet)
        }
    }

    fun updateSet(set: ExerciseSet) {
        viewModelScope.launch {
            repository.updateSet(set)
        }
    }

    fun toggleSetCompleted(set: ExerciseSet) {
        val newStatus = !set.isCompleted
        val updated = set.copy(isCompleted = newStatus)
        viewModelScope.launch {
            repository.updateSet(updated)
            if (newStatus && set.restTimeSeconds > 0) {
                startRestTimer(set.restTimeSeconds)
            }
        }
    }

    fun deleteSet(set: ExerciseSet) {
        viewModelScope.launch {
            repository.deleteSet(set)
        }
    }

    fun removeExerciseFromActiveWorkout(exerciseId: String) {
        val session = activeSession.value ?: return
        viewModelScope.launch {
            repository.removeExerciseFromWorkout(session.id, exerciseId)
        }
    }

    fun finishWorkout(notes: String = "") {
        val session = activeSession.value ?: return
        viewModelScope.launch {
            val result = repository.finishActiveWorkout(session.id, notes)
            _finishedWorkoutSummary.value = result
            skipRestTimer()
        }
    }

    fun clearFinishedWorkoutSummary() {
        _finishedWorkoutSummary.value = null
    }

    fun discardWorkout() {
        val session = activeSession.value ?: return
        viewModelScope.launch {
            repository.discardActiveWorkout(session.id)
            skipRestTimer()
        }
    }

    fun deleteCompletedWorkout(workoutId: String) {
        viewModelScope.launch {
            repository.deleteCompletedWorkout(workoutId)
        }
    }

    // BODY WEIGHT
    fun logBodyWeight(weightValue: Float, bodyFat: Float? = null, note: String = "") {
        viewModelScope.launch {
            val weightKg = GymCalculators.weightToKg(weightValue, settings.value.isKg)
            repository.logBodyWeight(weightKg, bodyFat, note)
        }
    }

    fun deleteWeightEntry(id: String) {
        viewModelScope.launch {
            repository.deleteWeightEntry(id)
        }
    }

    // EXERCISES
    fun toggleFavoriteExercise(exerciseId: String) {
        viewModelScope.launch {
            repository.toggleFavoriteExercise(exerciseId)
        }
    }

    fun createCustomExercise(
        name: String,
        muscle: MuscleGroup,
        equipment: Equipment,
        instructions: String
    ) {
        viewModelScope.launch {
            val ex = Exercise(
                id = "custom_" + System.currentTimeMillis(),
                name = name,
                primaryMuscle = muscle,
                equipment = equipment,
                instructions = instructions,
                isCustom = true
            )
            repository.saveExercise(ex)
        }
    }

    // ROUTINES
    fun saveRoutine(
        id: String?,
        name: String,
        dayOfWeek: Int,
        description: String,
        exerciseIds: List<String>
    ) {
        viewModelScope.launch {
            val routine = Routine(
                id = id ?: ("routine_" + System.currentTimeMillis()),
                name = name,
                dayOfWeek = dayOfWeek,
                description = description,
                exerciseIds = exerciseIds
            )
            repository.saveRoutine(routine)
        }
    }

    fun deleteRoutine(routineId: String) {
        viewModelScope.launch {
            repository.deleteRoutine(routineId)
        }
    }

    // SETTINGS
    fun updateUnit(isKg: Boolean) {
        viewModelScope.launch {
            repository.updateSettings(settings.value.copy(isKg = isKg))
        }
    }

    fun updateAccentColor(hex: String) {
        viewModelScope.launch {
            repository.updateSettings(settings.value.copy(accentColorHex = hex))
        }
    }

    fun updateDefaultRest(seconds: Int) {
        viewModelScope.launch {
            repository.updateSettings(settings.value.copy(defaultRestSeconds = seconds))
        }
    }

    fun updateTargetWeight(weightValue: Float?) {
        viewModelScope.launch {
            val kg = weightValue?.let { GymCalculators.weightToKg(it, settings.value.isKg) }
            repository.updateSettings(settings.value.copy(targetWeightKg = kg))
        }
    }

    // Today's suggested routine
    fun getTodayDayOfWeek(): Int {
        val cal = Calendar.getInstance()
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
    }

    // Helper to calculate muscle set count in past 7 days
    fun getPastWeekMuscleSetCount(): Map<MuscleGroup, Int> {
        val oneWeekAgo = System.currentTimeMillis() - (7 * 86_400_000L)
        val recentWorkouts = completedWorkouts.value.filter { it.startTimeEpochMs >= oneWeekAgo }
        val allEx = exercises.value.associateBy { it.id }

        val map = mutableMapOf<MuscleGroup, Int>()
        // Calculate approximate distribution based on workout volume
        recentWorkouts.forEach { w ->
            // Heuristic sets distribution
            val r = routines.value.find { it.id == w.routineId }
            r?.exerciseIds?.forEach { exId ->
                val ex = allEx[exId]
                if (ex != null) {
                    map[ex.primaryMuscle] = (map[ex.primaryMuscle] ?: 0) + 3
                }
            }
        }
        if (map.isEmpty()) {
            // Default sample distribution
            map[MuscleGroup.CHEST] = 12
            map[MuscleGroup.BACK] = 14
            map[MuscleGroup.SHOULDERS] = 8
            map[MuscleGroup.QUADS] = 10
            map[MuscleGroup.HAMSTRINGS] = 6
            map[MuscleGroup.BICEPS] = 6
            map[MuscleGroup.TRICEPS] = 6
            map[MuscleGroup.CORE] = 4
        }
        return map
    }
}

class GymViewModelFactory(private val repository: GymRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GymViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GymViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
