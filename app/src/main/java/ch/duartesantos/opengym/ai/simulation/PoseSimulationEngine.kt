package ch.duartesantos.opengym.ai.simulation

import ch.duartesantos.opengym.ai.model.*
import kotlin.math.cos
import kotlin.math.sin

enum class SquatSimulationScenario(val title: String, val description: String) {
    PERFECT_FORM("Perfect Form Squat", "Full parallel depth, upright torso, knees tracking outward"),
    SHALLOW_DEPTH("Shallow / Half Squat", "Stops before parallel depth — triggers depth coaching cue"),
    KNEE_VALGUS("Knee Cave (Valgus)", "Knees collapse inward on ascent — triggers knees out cue"),
    FORWARD_LEAN("Excessive Forward Lean", "Chest drops forward — triggers posture correction")
}

class PoseSimulationEngine {

    private var animationProgress: Float = 0f
    var currentScenario: SquatSimulationScenario = SquatSimulationScenario.PERFECT_FORM

    /**
     * Generates a 33-landmark PoseFrame corresponding to current phase of squat.
     * @param progress: 0.0 (standing) -> 1.0 (deep bottom) -> 2.0 (lockout completed)
     */
    fun generateFrame(progress: Float, scenario: SquatSimulationScenario = currentScenario): PoseFrame {
        // Squat depth factor [0.0 = standing, 1.0 = bottom depth]
        val depthFactor = if (progress <= 1.0f) {
            progress
        } else {
            (2.0f - progress).coerceAtLeast(0f)
        }

        val maxKneeDrop = when (scenario) {
            SquatSimulationScenario.PERFECT_FORM -> 0.22f
            SquatSimulationScenario.SHALLOW_DEPTH -> 0.11f // Shallow!
            SquatSimulationScenario.KNEE_VALGUS -> 0.20f
            SquatSimulationScenario.FORWARD_LEAN -> 0.21f
        }

        val kneeFlexion = depthFactor * maxKneeDrop
        val hipDrop = depthFactor * (maxKneeDrop * 1.35f)

        val torsoLeanX = when (scenario) {
            SquatSimulationScenario.FORWARD_LEAN -> depthFactor * 0.12f
            else -> depthFactor * 0.04f
        }

        val kneeCaveOffset = when (scenario) {
            SquatSimulationScenario.KNEE_VALGUS -> if (progress > 1.0f) 0.06f * (2.0f - progress) else 0f
            else -> 0f
        }

        val landmarks = mutableMapOf<Int, PoseLandmark>()

        // Head
        landmarks[MediaPipeLandmarkIndices.NOSE] = PoseLandmark(0, "nose", 0.50f + torsoLeanX, 0.15f + hipDrop * 0.6f)

        // Shoulders
        val shoulderY = 0.28f + hipDrop * 0.7f
        landmarks[MediaPipeLandmarkIndices.LEFT_SHOULDER] = PoseLandmark(11, "leftShoulder", 0.42f + torsoLeanX, shoulderY)
        landmarks[MediaPipeLandmarkIndices.RIGHT_SHOULDER] = PoseLandmark(12, "rightShoulder", 0.58f + torsoLeanX, shoulderY)

        // Elbows & Wrists (Barbell front rack / high bar hold)
        landmarks[MediaPipeLandmarkIndices.LEFT_ELBOW] = PoseLandmark(13, "leftElbow", 0.38f + torsoLeanX, shoulderY + 0.10f)
        landmarks[MediaPipeLandmarkIndices.RIGHT_ELBOW] = PoseLandmark(14, "rightElbow", 0.62f + torsoLeanX, shoulderY + 0.10f)
        landmarks[MediaPipeLandmarkIndices.LEFT_WRIST] = PoseLandmark(15, "leftWrist", 0.42f + torsoLeanX, shoulderY - 0.02f)
        landmarks[MediaPipeLandmarkIndices.RIGHT_WRIST] = PoseLandmark(16, "rightWrist", 0.58f + torsoLeanX, shoulderY - 0.02f)

        // Hips
        val hipY = 0.50f + hipDrop
        landmarks[MediaPipeLandmarkIndices.LEFT_HIP] = PoseLandmark(23, "leftHip", 0.44f, hipY)
        landmarks[MediaPipeLandmarkIndices.RIGHT_HIP] = PoseLandmark(24, "rightHip", 0.56f, hipY)

        // Knees (Flex forward and push down)
        val kneeY = 0.70f + kneeFlexion
        landmarks[MediaPipeLandmarkIndices.LEFT_KNEE] = PoseLandmark(25, "leftKnee", 0.42f + kneeCaveOffset, kneeY)
        landmarks[MediaPipeLandmarkIndices.RIGHT_KNEE] = PoseLandmark(26, "rightKnee", 0.58f - kneeCaveOffset, kneeY)

        // Ankles & Feet (Fixed on ground)
        landmarks[MediaPipeLandmarkIndices.LEFT_ANKLE] = PoseLandmark(27, "leftAnkle", 0.43f, 0.90f)
        landmarks[MediaPipeLandmarkIndices.RIGHT_ANKLE] = PoseLandmark(28, "rightAnkle", 0.57f, 0.90f)
        landmarks[MediaPipeLandmarkIndices.LEFT_HEEL] = PoseLandmark(29, "leftHeel", 0.42f, 0.92f)
        landmarks[MediaPipeLandmarkIndices.RIGHT_HEEL] = PoseLandmark(30, "rightHeel", 0.58f, 0.92f)
        landmarks[MediaPipeLandmarkIndices.LEFT_FOOT_INDEX] = PoseLandmark(31, "leftFoot", 0.40f, 0.94f)
        landmarks[MediaPipeLandmarkIndices.RIGHT_FOOT_INDEX] = PoseLandmark(32, "rightFoot", 0.60f, 0.94f)

        return PoseFrame(
            timestampMs = System.currentTimeMillis(),
            landmarks = landmarks
        )
    }
}
