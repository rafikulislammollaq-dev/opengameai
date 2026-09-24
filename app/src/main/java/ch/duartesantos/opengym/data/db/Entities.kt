package ch.duartesantos.opengym.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import ch.duartesantos.opengym.data.model.*

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val primaryMuscle: MuscleGroup,
    val secondaryMuscles: List<MuscleGroup>,
    val equipment: Equipment,
    val instructions: String,
    val isFavorite: Boolean,
    val isCustom: Boolean
) {
    fun toDomain(): Exercise = Exercise(
        id = id,
        name = name,
        primaryMuscle = primaryMuscle,
        secondaryMuscles = secondaryMuscles,
        equipment = equipment,
        instructions = instructions,
        isFavorite = isFavorite,
        isCustom = isCustom
    )

    companion object {
        fun fromDomain(e: Exercise): ExerciseEntity = ExerciseEntity(
            id = e.id,
            name = e.name,
            primaryMuscle = e.primaryMuscle,
            secondaryMuscles = e.secondaryMuscles,
            equipment = e.equipment,
            instructions = e.instructions,
            isFavorite = e.isFavorite,
            isCustom = e.isCustom
        )
    }
}

@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val routineId: String?,
    val startTimeEpochMs: Long,
    val endTimeEpochMs: Long?,
    val durationSeconds: Long,
    val totalVolumeKg: Float,
    val totalSets: Int,
    val prCount: Int,
    val isFinished: Boolean,
    val notes: String
) {
    fun toDomain(): WorkoutSession = WorkoutSession(
        id = id,
        name = name,
        routineId = routineId,
        startTimeEpochMs = startTimeEpochMs,
        endTimeEpochMs = endTimeEpochMs,
        durationSeconds = durationSeconds,
        totalVolumeKg = totalVolumeKg,
        totalSets = totalSets,
        prCount = prCount,
        isFinished = isFinished,
        notes = notes
    )

    companion object {
        fun fromDomain(s: WorkoutSession): WorkoutSessionEntity = WorkoutSessionEntity(
            id = s.id,
            name = s.name,
            routineId = s.routineId,
            startTimeEpochMs = s.startTimeEpochMs,
            endTimeEpochMs = s.endTimeEpochMs,
            durationSeconds = s.durationSeconds,
            totalVolumeKg = s.totalVolumeKg,
            totalSets = s.totalSets,
            prCount = s.prCount,
            isFinished = s.isFinished,
            notes = s.notes
        )
    }
}

@Entity(tableName = "exercise_sets")
data class ExerciseSetEntity(
    @PrimaryKey val id: String,
    val workoutSessionId: String,
    val exerciseId: String,
    val setNumber: Int,
    val weightKg: Float,
    val reps: Int,
    val rpe: Float?,
    val type: SetType,
    val isCompleted: Boolean,
    val restTimeSeconds: Int,
    val isPr: Boolean
) {
    fun toDomain(): ExerciseSet = ExerciseSet(
        id = id,
        workoutSessionId = workoutSessionId,
        exerciseId = exerciseId,
        setNumber = setNumber,
        weightKg = weightKg,
        reps = reps,
        rpe = rpe,
        type = type,
        isCompleted = isCompleted,
        restTimeSeconds = restTimeSeconds,
        isPr = isPr
    )

    companion object {
        fun fromDomain(s: ExerciseSet): ExerciseSetEntity = ExerciseSetEntity(
            id = s.id,
            workoutSessionId = s.workoutSessionId,
            exerciseId = s.exerciseId,
            setNumber = s.setNumber,
            weightKg = s.weightKg,
            reps = s.reps,
            rpe = s.rpe,
            type = s.type,
            isCompleted = s.isCompleted,
            restTimeSeconds = s.restTimeSeconds,
            isPr = s.isPr
        )
    }
}

@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey val id: String,
    val name: String,
    val dayOfWeek: Int,
    val description: String,
    val targetDurationMinutes: Int = 45,
    val exerciseIds: List<String>
) {
    fun toDomain(): Routine = Routine(
        id = id,
        name = name,
        dayOfWeek = dayOfWeek,
        description = description,
        targetDurationMinutes = targetDurationMinutes,
        exerciseIds = exerciseIds
    )

    companion object {
        fun fromDomain(r: Routine): RoutineEntity = RoutineEntity(
            id = r.id,
            name = r.name,
            dayOfWeek = r.dayOfWeek,
            description = r.description,
            targetDurationMinutes = r.targetDurationMinutes,
            exerciseIds = r.exerciseIds
        )
    }
}

@Entity(tableName = "body_weight_entries")
data class BodyWeightEntity(
    @PrimaryKey val id: String,
    val timestampEpochMs: Long,
    val weightKg: Float,
    val bodyFatPercent: Float?,
    val note: String
) {
    fun toDomain(): BodyWeightEntry = BodyWeightEntry(
        id = id,
        timestampEpochMs = timestampEpochMs,
        weightKg = weightKg,
        bodyFatPercent = bodyFatPercent,
        note = note
    )

    companion object {
        fun fromDomain(b: BodyWeightEntry): BodyWeightEntity = BodyWeightEntity(
            id = b.id,
            timestampEpochMs = b.timestampEpochMs,
            weightKg = b.weightKg,
            bodyFatPercent = b.bodyFatPercent,
            note = b.note
        )
    }
}

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val isKg: Boolean = true,
    val defaultRestSeconds: Int = 90,
    val accentColorHex: String = "#A3E635",
    val targetWeightKg: Float? = null
) {
    fun toDomain(): UserSettings = UserSettings(
        isKg = isKg,
        defaultRestSeconds = defaultRestSeconds,
        accentColorHex = accentColorHex,
        targetWeightKg = targetWeightKg
    )

    companion object {
        fun fromDomain(s: UserSettings): UserSettingsEntity = UserSettingsEntity(
            id = 1,
            isKg = s.isKg,
            defaultRestSeconds = s.defaultRestSeconds,
            accentColorHex = s.accentColorHex,
            targetWeightKg = s.targetWeightKg
        )
    }
}
