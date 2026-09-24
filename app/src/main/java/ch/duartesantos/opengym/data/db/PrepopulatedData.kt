package ch.duartesantos.opengym.data.db

import ch.duartesantos.opengym.data.model.Equipment
import ch.duartesantos.opengym.data.model.MuscleGroup

object PrepopulatedData {

    val defaultExercises = listOf(
        ExerciseEntity(
            id = "ex_barbell_bench_press",
            name = "Barbell Bench Press",
            primaryMuscle = MuscleGroup.CHEST,
            secondaryMuscles = listOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS),
            equipment = Equipment.BARBELL,
            instructions = "Lie on bench, grip bar slightly wider than shoulder width. Lower bar to mid-chest with control, then press upward explosively.",
            isFavorite = true,
            isCustom = false
        ),
        ExerciseEntity(
            id = "ex_incline_dumbbell_press",
            name = "Incline Dumbbell Press",
            primaryMuscle = MuscleGroup.CHEST,
            secondaryMuscles = listOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS),
            equipment = Equipment.DUMBBELL,
            instructions = "Set bench to 30-45 degrees. Press dumbbells up from chest level, converging slightly at the top.",
            isFavorite = true,
            isCustom = false
        ),
        ExerciseEntity(
            id = "ex_cable_flyes",
            name = "Cable Chest Fly",
            primaryMuscle = MuscleGroup.CHEST,
            secondaryMuscles = listOf(MuscleGroup.SHOULDERS),
            equipment = Equipment.CABLE,
            instructions = "Set pulleys at chest height. Bring handles together in a hugging motion with slight elbow bend.",
            isFavorite = false,
            isCustom = false
        ),
        ExerciseEntity(
            id = "ex_barbell_squat",
            name = "Barbell Back Squat",
            primaryMuscle = MuscleGroup.QUADS,
            secondaryMuscles = listOf(MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS, MuscleGroup.CORE),
            equipment = Equipment.BARBELL,
            instructions = "Rest bar on upper traps. Descend until hip crease is below top of knee (parallel depth). Drive through mid-foot to stand.",
            isFavorite = true,
            isCustom = false
        ),
        ExerciseEntity(
            id = "ex_leg_press",
            name = "Leg Press",
            primaryMuscle = MuscleGroup.QUADS,
            secondaryMuscles = listOf(MuscleGroup.GLUTES),
            equipment = Equipment.MACHINE,
            instructions = "Place feet shoulder-width on platform. Lower sled until knees are at 90 degrees, press back up without locking knees.",
            isFavorite = false,
            isCustom = false
        ),
        ExerciseEntity(
            id = "ex_romanian_deadlift",
            name = "Romanian Deadlift (RDL)",
            primaryMuscle = MuscleGroup.HAMSTRINGS,
            secondaryMuscles = listOf(MuscleGroup.GLUTES, MuscleGroup.BACK),
            equipment = Equipment.BARBELL,
            instructions = "Hinge at the hips, keeping back flat and slight knee bend. Lower bar along shins until hamstring stretch, then contract glutes to return.",
            isFavorite = true,
            isCustom = false
        ),
        ExerciseEntity(
            id = "ex_conventional_deadlift",
            name = "Barbell Deadlift",
            primaryMuscle = MuscleGroup.BACK,
            secondaryMuscles = listOf(MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.FOREARMS),
            equipment = Equipment.BARBELL,
            instructions = "Stand with feet hip-width under bar. Grip bar, flatten spine, pull chest tall, and drive floor away to lockout.",
            isFavorite = true,
            isCustom = false
        ),
        ExerciseEntity(
            id = "ex_pull_up",
            name = "Pull-Up",
            primaryMuscle = MuscleGroup.BACK,
            secondaryMuscles = listOf(MuscleGroup.BICEPS, MuscleGroup.FOREARMS),
            equipment = Equipment.BODYWEIGHT,
            instructions = "Hang with overhand grip wider than shoulders. Pull chest to bar while depressing shoulder blades.",
            isFavorite = true,
            isCustom = false
        ),
        ExerciseEntity(
            id = "ex_barbell_row",
            name = "Barbell Bent-Over Row",
            primaryMuscle = MuscleGroup.BACK,
            secondaryMuscles = listOf(MuscleGroup.BICEPS, MuscleGroup.SHOULDERS),
            equipment = Equipment.BARBELL,
            instructions = "Hinge forward 45 degrees with flat back. Pull bar toward lower ribcage, squeezing shoulder blades together.",
            isFavorite = true,
            isCustom = false
        ),
        ExerciseEntity(
            id = "ex_lat_pulldown",
            name = "Lat Pulldown",
            primaryMuscle = MuscleGroup.BACK,
            secondaryMuscles = listOf(MuscleGroup.BICEPS),
            equipment = Equipment.CABLE,
            instructions = "Grip wide bar, pull down to upper collarbone while leaning back slightly. Control the eccentric release.",
            isFavorite = false,
            isCustom = false
        ),
        ExerciseEntity(
            id = "ex_overhead_press",
            name = "Standing Overhead Press (OHP)",
            primaryMuscle = MuscleGroup.SHOULDERS,
            secondaryMuscles = listOf(MuscleGroup.TRICEPS, MuscleGroup.CORE),
            equipment = Equipment.BARBELL,
            instructions = "Rest bar on clavicle. Press bar vertically overhead while bracing core and glutes. Lock out overhead.",
            isFavorite = true,
            isCustom = false
        ),
        ExerciseEntity(
            id = "ex_lateral_raise",
            name = "Dumbbell Lateral Raise",
            primaryMuscle = MuscleGroup.SHOULDERS,
            secondaryMuscles = emptyList(),
            equipment = Equipment.DUMBBELL,
            instructions = "Raise dumbbells out to sides until arms are parallel with the floor. Lead with elbows.",
            isFavorite = true,
            isCustom = false
        ),
        ExerciseEntity(
            id = "ex_face_pull",
            name = "Cable Face Pull",
            primaryMuscle = MuscleGroup.SHOULDERS,
            secondaryMuscles = listOf(MuscleGroup.BACK),
            equipment = Equipment.CABLE,
            instructions = "Attach rope to high cable. Pull toward nose and rotate knuckles back for rear delt and rotator cuff strength.",
            isFavorite = false,
            isCustom = false
        ),
        ExerciseEntity(
            id = "ex_bicep_curl",
            name = "Dumbbell Bicep Curl",
            primaryMuscle = MuscleGroup.BICEPS,
            secondaryMuscles = listOf(MuscleGroup.FOREARMS),
            equipment = Equipment.DUMBBELL,
            instructions = "Curl dumbbells upward while supinating wrists. Squeeze peak contraction at the top.",
            isFavorite = true,
            isCustom = false
        ),
        ExerciseEntity(
            id = "ex_tricep_pushdown",
            name = "Cable Tricep Pushdown",
            primaryMuscle = MuscleGroup.TRICEPS,
            secondaryMuscles = emptyList(),
            equipment = Equipment.CABLE,
            instructions = "Pin elbows to ribs, push cable downward until arms are fully extended. Squeeze triceps.",
            isFavorite = true,
            isCustom = false
        ),
        ExerciseEntity(
            id = "ex_hanging_leg_raise",
            name = "Hanging Leg Raise",
            primaryMuscle = MuscleGroup.CORE,
            secondaryMuscles = emptyList(),
            equipment = Equipment.BODYWEIGHT,
            instructions = "Hang from bar, engage core to raise legs upward toward horizontal without swinging.",
            isFavorite = false,
            isCustom = false
        ),
        ExerciseEntity(
            id = "ex_standing_calf_raise",
            name = "Standing Calf Raise",
            primaryMuscle = MuscleGroup.CALVES,
            secondaryMuscles = emptyList(),
            equipment = Equipment.MACHINE,
            instructions = "Balls of feet on edge. Lower heels for deep stretch, drive up onto toes with a 1-second pause.",
            isFavorite = false,
            isCustom = false
        )
    )

    val defaultRoutines = listOf(
        RoutineEntity(
            id = "routine_push_day",
            name = "Push Day (Chest, Shoulders, Triceps)",
            dayOfWeek = 1, // Monday
            description = "Hypertrophy and strength for upper body pressing muscles.",
            exerciseIds = listOf(
                "ex_barbell_bench_press",
                "ex_incline_dumbbell_press",
                "ex_overhead_press",
                "ex_lateral_raise",
                "ex_tricep_pushdown"
            )
        ),
        RoutineEntity(
            id = "routine_pull_day",
            name = "Pull Day (Back, Biceps, Rear Delts)",
            dayOfWeek = 2, // Tuesday
            description = "Upper body pulling volume for back thickness and width.",
            exerciseIds = listOf(
                "ex_pull_up",
                "ex_barbell_row",
                "ex_lat_pulldown",
                "ex_face_pull",
                "ex_bicep_curl"
            )
        ),
        RoutineEntity(
            id = "routine_legs_day",
            name = "Legs & Core Day",
            dayOfWeek = 3, // Wednesday
            description = "Lower body quad, hamstring and core foundation.",
            exerciseIds = listOf(
                "ex_barbell_squat",
                "ex_romanian_deadlift",
                "ex_leg_press",
                "ex_standing_calf_raise",
                "ex_hanging_leg_raise"
            )
        ),
        RoutineEntity(
            id = "routine_upper_day",
            name = "Upper Power Day",
            dayOfWeek = 5, // Friday
            description = "Compound strength training for total upper body.",
            exerciseIds = listOf(
                "ex_barbell_bench_press",
                "ex_barbell_row",
                "ex_overhead_press",
                "ex_pull_up"
            )
        ),
        RoutineEntity(
            id = "routine_lower_day",
            name = "Lower Power & Deadlift",
            dayOfWeek = 6, // Saturday
            description = "Heavy deadlifts and squat auxiliary work.",
            exerciseIds = listOf(
                "ex_conventional_deadlift",
                "ex_barbell_squat",
                "ex_hanging_leg_raise"
            )
        )
    )

    fun getInitialWeightEntries(): List<BodyWeightEntity> {
        val now = System.currentTimeMillis()
        val oneDay = 86_400_000L
        return listOf(
            BodyWeightEntity("bw_1", now - (14 * oneDay), 78.5f, 15.2f, "Baseline weight"),
            BodyWeightEntity("bw_2", now - (10 * oneDay), 78.2f, 15.0f, "Post-workout"),
            BodyWeightEntity("bw_3", now - (7 * oneDay), 77.9f, 14.8f, "Morning fast"),
            BodyWeightEntity("bw_4", now - (3 * oneDay), 77.6f, 14.7f, "Rest day"),
            BodyWeightEntity("bw_5", now, 77.4f, 14.5f, "Target progression on track")
        )
    }
}
