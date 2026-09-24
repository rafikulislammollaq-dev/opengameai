package ch.duartesantos.opengym.ai.analyzer

import ch.duartesantos.opengym.ai.coach.ExerciseCoachingBook
import ch.duartesantos.opengym.ai.coach.RepEvaluationContext
import ch.duartesantos.opengym.ai.filters.PoseLandmarkFilter
import ch.duartesantos.opengym.ai.geometry.PoseGeometry
import ch.duartesantos.opengym.ai.model.*
import kotlin.math.abs

class SquatAnalyzer {

    var currentPhase: RepPhase = RepPhase.INIT
        private set

    var totalReps: Int = 0
        private set

    var standingBaselineAngle: Float = 175.0f
        private set

    var currentKneeAngle: Float = 175.0f
        private set

    var currentRom: Float = 0.0f
        private set

    var minKneeAngleInRep: Float = 180.0f
        private set

    var maxTorsoLeanInRep: Float = 0.0f
        private set

    var maxAsymmetryInRep: Float = 0.0f
        private set

    var kneeValgusDetectedInRep: Boolean = false
        private set

    private var repStartTimeMs: Long = 0L
    private var baselineSamplesCount: Int = 0
    private var baselineAccumulator: Float = 0f

    private val poseFilter = PoseLandmarkFilter()

    // Callbacks for UI
    var onRepCompleted: ((SquatRepData) -> Unit)? = null
    var onPhaseChanged: ((RepPhase) -> Unit)? = null

    fun reset() {
        currentPhase = RepPhase.INIT
        totalReps = 0
        standingBaselineAngle = 175.0f
        currentKneeAngle = 175.0f
        currentRom = 0f
        minKneeAngleInRep = 180.0f
        maxTorsoLeanInRep = 0f
        maxAsymmetryInRep = 0f
        kneeValgusDetectedInRep = false
        repStartTimeMs = 0L
        baselineSamplesCount = 0
        baselineAccumulator = 0f
        poseFilter.reset()
    }

    fun processFrame(rawFrame: PoseFrame): RepPhase {
        // Apply 1-Euro smoothing filter
        val smoothedLandmarks = poseFilter.filter(rawFrame.landmarks, rawFrame.timestampMs)
        val frame = rawFrame.copy(landmarks = smoothedLandmarks)

        val leftHip = frame.getLandmark(MediaPipeLandmarkIndices.LEFT_HIP)
        val leftKnee = frame.getLandmark(MediaPipeLandmarkIndices.LEFT_KNEE)
        val leftAnkle = frame.getLandmark(MediaPipeLandmarkIndices.LEFT_ANKLE)

        val rightHip = frame.getLandmark(MediaPipeLandmarkIndices.RIGHT_HIP)
        val rightKnee = frame.getLandmark(MediaPipeLandmarkIndices.RIGHT_KNEE)
        val rightAnkle = frame.getLandmark(MediaPipeLandmarkIndices.RIGHT_ANKLE)

        val leftShoulder = frame.getLandmark(MediaPipeLandmarkIndices.LEFT_SHOULDER)

        val leftKneeAngle = PoseGeometry.calculateAngle2D(leftHip, leftKnee, leftAnkle)
        val rightKneeAngle = PoseGeometry.calculateAngle2D(rightHip, rightKnee, rightAnkle)

        val avgKnee = when {
            leftKneeAngle != null && rightKneeAngle != null -> (leftKneeAngle + rightKneeAngle) / 2f
            leftKneeAngle != null -> leftKneeAngle
            rightKneeAngle != null -> rightKneeAngle
            else -> return currentPhase
        }

        currentKneeAngle = avgKnee

        val torsoLean = PoseGeometry.calculateTorsoInclination(leftShoulder, leftHip) ?: 0f

        // Check valgus in frame
        if (leftHip != null && rightHip != null && leftKnee != null && rightKnee != null && leftAnkle != null && rightAnkle != null) {
            val kneeDist = PoseGeometry.distance2D(leftKnee, rightKnee) ?: 1f
            val ankleDist = PoseGeometry.distance2D(leftAnkle, rightAnkle) ?: 1f
            if (kneeDist < ankleDist * 0.72f) {
                kneeValgusDetectedInRep = true
            }
        }

        // Asymmetry in frame
        if (leftKneeAngle != null && rightKneeAngle != null) {
            val asym = abs(leftKneeAngle - rightKneeAngle)
            if (asym > maxAsymmetryInRep) {
                maxAsymmetryInRep = asym
            }
        }

        // Calibrate baseline
        if (baselineSamplesCount < 15 && avgKnee > 160f) {
            baselineAccumulator += avgKnee
            baselineSamplesCount++
            standingBaselineAngle = baselineAccumulator / baselineSamplesCount
            if (currentPhase == RepPhase.INIT && baselineSamplesCount >= 5) {
                setPhase(RepPhase.READY)
            }
        }

        currentRom = (standingBaselineAngle - avgKnee).coerceAtLeast(0f)
        val now = frame.timestampMs

        when (currentPhase) {
            RepPhase.INIT -> {
                if (avgKnee >= 165f) {
                    setPhase(RepPhase.READY)
                }
            }
            RepPhase.READY -> {
                minKneeAngleInRep = avgKnee
                maxTorsoLeanInRep = torsoLean
                maxAsymmetryInRep = 0f
                kneeValgusDetectedInRep = false
                if (avgKnee < 155f) {
                    repStartTimeMs = now
                    setPhase(RepPhase.DESCENDING)
                }
            }
            RepPhase.DESCENDING -> {
                if (avgKnee < minKneeAngleInRep) {
                    minKneeAngleInRep = avgKnee
                }
                if (torsoLean > maxTorsoLeanInRep) {
                    maxTorsoLeanInRep = torsoLean
                }

                if (avgKnee <= 105f) {
                    setPhase(RepPhase.BOTTOM)
                } else if (avgKnee > minKneeAngleInRep + 8f && avgKnee < 140f) {
                    setPhase(RepPhase.ASCENDING)
                }
            }
            RepPhase.BOTTOM -> {
                if (avgKnee < minKneeAngleInRep) {
                    minKneeAngleInRep = avgKnee
                }
                if (torsoLean > maxTorsoLeanInRep) {
                    maxTorsoLeanInRep = torsoLean
                }

                if (avgKnee > minKneeAngleInRep + 6f) {
                    setPhase(RepPhase.ASCENDING)
                }
            }
            RepPhase.ASCENDING -> {
                if (torsoLean > maxTorsoLeanInRep) {
                    maxTorsoLeanInRep = torsoLean
                }

                if (avgKnee >= 165f) {
                    totalReps++
                    val duration = (now - repStartTimeMs).coerceAtLeast(300L)
                    val maxRom = standingBaselineAngle - minKneeAngleInRep
                    val depth = when {
                        minKneeAngleInRep <= 85f -> SquatDepth.DEEP
                        minKneeAngleInRep <= 98f -> SquatDepth.PARALLEL
                        minKneeAngleInRep <= 125f -> SquatDepth.SHALLOW
                        else -> SquatDepth.QUARTER
                    }

                    // Run the Exercise Coaching Book rules
                    val coachingFeedbacks = ExerciseCoachingBook.evaluateSquatRep(
                        RepEvaluationContext(
                            exercise = "squat",
                            accepted = true,
                            romDegrees = maxRom,
                            minRomDegrees = 75f,
                            maxTorsoLeanDegrees = maxTorsoLeanInRep,
                            torsoLeanAttentionDegrees = 35f,
                            maxKneeAsymmetryDegrees = maxAsymmetryInRep,
                            symmetryAttentionDegrees = 14f,
                            durationMs = duration,
                            kneeValgusDetected = kneeValgusDetectedInRep
                        )
                    )

                    var score = 100
                    if (depth == SquatDepth.SHALLOW || depth == SquatDepth.QUARTER) score -= 25
                    if (maxTorsoLeanInRep > 35f) score -= 15
                    if (kneeValgusDetectedInRep) score -= 20
                    if (maxAsymmetryInRep > 14f) score -= 10

                    val repData = SquatRepData(
                        repNumber = totalReps,
                        durationMs = duration,
                        minKneeAngle = minKneeAngleInRep,
                        maxFlexionRom = maxRom,
                        depth = depth,
                        formScore = score.coerceIn(0, 100),
                        torsoLeanAngle = maxTorsoLeanInRep,
                        kneeValgusDetected = kneeValgusDetectedInRep,
                        coachingFeedback = coachingFeedbacks.map { it.message }
                    )

                    onRepCompleted?.invoke(repData)
                    setPhase(RepPhase.LOCKED_OUT)
                }
            }
            RepPhase.LOCKED_OUT -> {
                if (avgKnee >= 165f) {
                    setPhase(RepPhase.READY)
                } else if (avgKnee < 155f) {
                    repStartTimeMs = now
                    minKneeAngleInRep = avgKnee
                    maxTorsoLeanInRep = torsoLean
                    maxAsymmetryInRep = 0f
                    kneeValgusDetectedInRep = false
                    setPhase(RepPhase.DESCENDING)
                }
            }
        }

        return currentPhase
    }

    private fun setPhase(newPhase: RepPhase) {
        if (currentPhase != newPhase) {
            currentPhase = newPhase
            onPhaseChanged?.invoke(newPhase)
        }
    }
}
