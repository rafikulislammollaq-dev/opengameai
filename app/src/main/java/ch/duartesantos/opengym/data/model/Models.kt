package ch.duartesantos.opengym.data.model

import java.util.UUID

enum class MuscleGroup(val displayName: String) {
    CHEST("Chest"),
    BACK("Back"),
    SHOULDERS("Shoulders"),
    BICEPS("Biceps"),
    TRICEPS("Triceps"),
    FOREARMS("Forearms"),
    QUADS("Quads"),
    HAMSTRINGS("Hamstrings"),
    GLUTES("Glutes"),
    CALVES("Calves"),
    CORE("Core"),
    FULL_BODY("Full Body"),
    CARDIO("Cardio")
}

enum class Equipment(val displayName: String) {
    BARBELL("Barbell"),
    DUMBBELL("Dumbbell"),
    MACHINE("Machine"),
    CABLE("Cable"),
    BODYWEIGHT("Bodyweight"),
    SMITH_MACHINE("Smith Machine"),
    KETTLEBELL("Kettlebell"),
    BANDS("Bands"),
    OTHER("Other")
}

enum class SetType(val badge: String, val displayName: String) {
    NORMAL("N", "Normal"),
    WARMUP("W", "Warmup"),
    DROP("D", "Drop Set"),
    FAILURE("F", "Failure")
}

data class Exercise(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val primaryMuscle: MuscleGroup,
    val secondaryMuscles: List<MuscleGroup> = emptyList(),
    val equipment: Equipment = Equipment.BARBELL,
    val instructions: String = "",
    val isFavorite: Boolean = false,
    val isCustom: Boolean = false
)

data class ExerciseSet(
    val id: String = UUID.randomUUID().toString(),
    val workoutSessionId: String,
    val exerciseId: String,
    val setNumber: Int,
    val weightKg: Float = 0f,
    val reps: Int = 0,
    val rpe: Float? = null,
    val type: SetType = SetType.NORMAL,
    val isCompleted: Boolean = false,
    val restTimeSeconds: Int = 90,
    val isPr: Boolean = false
)

data class WorkoutSession(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Workout",
    val routineId: String? = null,
    val startTimeEpochMs: Long = System.currentTimeMillis(),
    val endTimeEpochMs: Long? = null,
    val durationSeconds: Long = 0L,
    val totalVolumeKg: Float = 0f,
    val totalSets: Int = 0,
    val prCount: Int = 0,
    val isFinished: Boolean = false,
    val notes: String = ""
)

data class Routine(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val dayOfWeek: Int = 1, // 1 = Monday, 7 = Sunday
    val description: String = "",
    val targetDurationMinutes: Int = 45,
    val exerciseIds: List<String> = emptyList()
)

data class BodyWeightEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestampEpochMs: Long = System.currentTimeMillis(),
    val weightKg: Float,
    val bodyFatPercent: Float? = null,
    val note: String = ""
)

data class UserSettings(
    val isKg: Boolean = true,
    val defaultRestSeconds: Int = 90,
    val accentColorHex: String = "#A3E635",
    val targetWeightKg: Float? = null
)

object GymCalculators {
    private const val KG_TO_LBS = 2.2046226218f

    fun kgToLbs(kg: Float): Float = kg * KG_TO_LBS

    fun lbsToKg(lbs: Float): Float = lbs / KG_TO_LBS

    fun kgToUserUnit(kg: Float, isKg: Boolean): Float = if (isKg) kg else kgToLbs(kg)

    fun formatWeight(kg: Float, isKg: Boolean): String {
        val converted = if (isKg) kg else kgToLbs(kg)
        val unit = if (isKg) "kg" else "lbs"
        return if (converted % 1.0f == 0f) {
            "${converted.toInt()} $unit"
        } else {
            "%.1f $unit".format(converted)
        }
    }

    fun formatWeightValue(kg: Float, isKg: Boolean): Float {
        return if (isKg) kg else kgToLbs(kg)
    }

    fun weightToKg(value: Float, isKg: Boolean): Float {
        return if (isKg) value else lbsToKg(value)
    }

    /**
     * Epley 1RM formula: 1RM = Weight * (1 + Reps / 30)
     */
    fun calculateOneRm(weightKg: Float, reps: Int): Float {
        if (reps <= 0 || weightKg <= 0f) return 0f
        if (reps == 1) return weightKg
        return weightKg * (1f + reps / 30f)
    }

    fun calculateOneRepMax(weightKg: Float, reps: Int): Float = calculateOneRm(weightKg, reps)

    /**
     * Percentage breakdown of a 1RM
     */
    fun calculateOneRmTable(oneRmKg: Float): List<Pair<Int, Float>> {
        val percentages = listOf(100, 95, 90, 85, 80, 75, 70, 65, 60, 50)
        return percentages.map { pct ->
            pct to (oneRmKg * (pct / 100f))
        }
    }
}
