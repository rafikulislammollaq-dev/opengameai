package ch.duartesantos.opengym.ai.coach

import ch.duartesantos.opengym.ai.model.CoachFeedback
import ch.duartesantos.opengym.ai.model.CoachSeverity

data class RepEvaluationContext(
    val exercise: String = "squat",
    val accepted: Boolean = true,
    val romDegrees: Float = 0f,
    val minRomDegrees: Float = 60f,
    val maxTorsoLeanDegrees: Float = 0f,
    val torsoLeanAttentionDegrees: Float = 35f,
    val maxKneeAsymmetryDegrees: Float = 0f,
    val symmetryAttentionDegrees: Float = 12f,
    val durationMs: Long = 2000L,
    val fastRepDurationMs: Long = 600L,
    val slowRepDurationMs: Long = 6000L,
    val kneeValgusDetected: Boolean = false
)

object ExerciseCoachingBook {

    /**
     * Evaluates a completed rep according to openGym's coaching book rules.
     * Port of frontend/src/ai/coach/ExerciseCoachingBook.js
     */
    fun evaluateSquatRep(ctx: RepEvaluationContext): List<CoachFeedback> {
        val feedbacks = mutableListOf<CoachFeedback>()

        // 1. Depth rule
        if (ctx.accepted && ctx.romDegrees < ctx.minRomDegrees) {
            feedbacks.add(
                CoachFeedback(
                    message = "Depth was a little shallow (%.0f° ROM).".format(ctx.romDegrees),
                    correction = "Next rep: descend slightly farther while staying controlled.",
                    severity = CoachSeverity.WARNING
                )
            )
        }

        // 2. Torso lean rule
        if (ctx.accepted && ctx.maxTorsoLeanDegrees > ctx.torsoLeanAttentionDegrees) {
            feedbacks.add(
                CoachFeedback(
                    message = "Forward torso lean increased to %.0f°.".format(ctx.maxTorsoLeanDegrees),
                    correction = "Next rep: brace your trunk and keep the torso more stable.",
                    severity = CoachSeverity.WARNING
                )
            )
        }

        // 3. Knee asymmetry rule
        if (ctx.accepted && ctx.maxKneeAsymmetryDegrees > ctx.symmetryAttentionDegrees) {
            feedbacks.add(
                CoachFeedback(
                    message = "Left-right knee-angle difference reached %.0f°.".format(ctx.maxKneeAsymmetryDegrees),
                    correction = "Next rep: try to keep both legs moving through a more even path.",
                    severity = CoachSeverity.INFO
                )
            )
        }

        // 4. Knee Valgus rule
        if (ctx.kneeValgusDetected) {
            feedbacks.add(
                CoachFeedback(
                    message = "Knees caved inward on ascent.",
                    correction = "Next rep: aggressively push knees outward in line with toes.",
                    severity = CoachSeverity.CRITICAL
                )
            )
        }

        // 5. Tempo / Rushed Rep rule
        if (ctx.accepted && ctx.durationMs < ctx.fastRepDurationMs) {
            feedbacks.add(
                CoachFeedback(
                    message = "The rep was fast (%.1fs).".format(ctx.durationMs / 1000f),
                    correction = "Next rep: slow the movement slightly and keep the descent controlled.",
                    severity = CoachSeverity.INFO
                )
            )
        } else if (ctx.accepted && ctx.durationMs > ctx.slowRepDurationMs) {
            feedbacks.add(
                CoachFeedback(
                    message = "Slow rep tempo (%.1fs).".format(ctx.durationMs / 1000f),
                    correction = "Good control! Ensure you don't grind excessively if fatigue sets in.",
                    severity = CoachSeverity.INFO
                )
            )
        }

        // 6. Good form rule if no issues
        if (feedbacks.isEmpty() && ctx.accepted) {
            feedbacks.add(
                CoachFeedback(
                    message = "Solid rep! Excellent depth & torso control.",
                    correction = "Maintain this exact cadence into the next repetition.",
                    severity = CoachSeverity.GOOD
                )
            )
        }

        return feedbacks
    }
}
