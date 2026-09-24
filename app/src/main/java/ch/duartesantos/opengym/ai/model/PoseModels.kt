package ch.duartesantos.opengym.ai.model

import kotlinx.serialization.Serializable

@Serializable
data class PoseLandmark(
    val id: Int,
    val name: String,
    val x: Float, // 0.0 to 1.0 normalized
    val y: Float, // 0.0 to 1.0 normalized
    val z: Float = 0f,
    val visibility: Float = 1.0f
)

@Serializable
data class PoseFrame(
    val timestampMs: Long = System.currentTimeMillis(),
    val landmarks: Map<Int, PoseLandmark>
) {
    fun getLandmark(id: Int): PoseLandmark? = landmarks[id]
}

enum class RepPhase(val displayName: String, val badgeColorHex: String) {
    INIT("INITIALIZING", "#8E8E93"),
    READY("READY (STANDING)", "#32D74B"),
    DESCENDING("DESCENDING", "#0A84FF"),
    BOTTOM("BOTTOM DEPTH", "#BF5AF2"),
    ASCENDING("DRIVING UP", "#FF9F0A"),
    LOCKED_OUT("REP COMPLETED", "#32D74B")
}

enum class SquatDepth(val displayName: String, val scoreMultiplier: Float) {
    DEEP("Below Parallel (Deep)", 1.0f),
    PARALLEL("Parallel (At Depth)", 0.95f),
    SHALLOW("Above Parallel (Shallow)", 0.65f),
    QUARTER("Quarter Squat", 0.35f)
}

enum class CoachSeverity {
    GOOD,
    INFO,
    WARNING,
    CRITICAL
}

data class CoachFeedback(
    val message: String,
    val correction: String,
    val severity: CoachSeverity,
    val timestampMs: Long = System.currentTimeMillis()
)

data class SquatRepData(
    val repNumber: Int,
    val durationMs: Long,
    val minKneeAngle: Float,
    val maxFlexionRom: Float,
    val depth: SquatDepth,
    val formScore: Int, // 0 - 100
    val torsoLeanAngle: Float,
    val kneeValgusDetected: Boolean,
    val coachingFeedback: List<String>
)

object MediaPipeLandmarkIndices {
    const val NOSE = 0
    const val LEFT_EYE = 2
    const val RIGHT_EYE = 5
    const val LEFT_SHOULDER = 11
    const val RIGHT_SHOULDER = 12
    const val LEFT_ELBOW = 13
    const val RIGHT_ELBOW = 14
    const val LEFT_WRIST = 15
    const val RIGHT_WRIST = 16
    const val LEFT_HIP = 23
    const val RIGHT_HIP = 24
    const val LEFT_KNEE = 25
    const val RIGHT_KNEE = 26
    const val LEFT_ANKLE = 27
    const val RIGHT_ANKLE = 28
    const val LEFT_HEEL = 29
    const val RIGHT_HEEL = 30
    const val LEFT_FOOT_INDEX = 31
    const val RIGHT_FOOT_INDEX = 32
}
