package ch.duartesantos.opengym.ai.analyzer

import ch.duartesantos.opengym.ai.geometry.PoseGeometry
import ch.duartesantos.opengym.ai.model.*

class SquatFormAnalyzer {

    fun analyzeFrame(frame: PoseFrame): List<CoachFeedback> {
        val feedbacks = mutableListOf<CoachFeedback>()

        val leftShoulder = frame.getLandmark(MediaPipeLandmarkIndices.LEFT_SHOULDER)
        val leftHip = frame.getLandmark(MediaPipeLandmarkIndices.LEFT_HIP)
        val rightHip = frame.getLandmark(MediaPipeLandmarkIndices.RIGHT_HIP)
        val leftKnee = frame.getLandmark(MediaPipeLandmarkIndices.LEFT_KNEE)
        val rightKnee = frame.getLandmark(MediaPipeLandmarkIndices.RIGHT_KNEE)
        val leftAnkle = frame.getLandmark(MediaPipeLandmarkIndices.LEFT_ANKLE)
        val rightAnkle = frame.getLandmark(MediaPipeLandmarkIndices.RIGHT_ANKLE)

        // 1. Torso Angle Check
        val torsoLean = PoseGeometry.calculateTorsoInclination(leftShoulder, leftHip)
        if (torsoLean != null && torsoLean > 45f) {
            feedbacks.add(
                CoachFeedback(
                    message = "Excessive Forward Lean (%.0f°)".format(torsoLean),
                    correction = "Keep chest tall, brace core, and look slightly forward",
                    severity = CoachSeverity.WARNING
                )
            )
        }

        // 2. Knee Cave / Valgus Check
        if (leftHip != null && rightHip != null && leftKnee != null && rightKnee != null && leftAnkle != null && rightAnkle != null) {
            val hipWidth = PoseGeometry.distance2D(leftHip, rightHip) ?: 1f
            val kneeWidth = PoseGeometry.distance2D(leftKnee, rightKnee) ?: 1f
            val ankleWidth = PoseGeometry.distance2D(leftAnkle, rightAnkle) ?: 1f

            if (kneeWidth < (ankleWidth * 0.75f) && kneeWidth < (hipWidth * 0.85f)) {
                feedbacks.add(
                    CoachFeedback(
                        message = "Knees Caving Inward (Valgus)",
                        correction = "Drive knees outward in line with your toes",
                        severity = CoachSeverity.CRITICAL
                    )
                )
            }
        }

        // 3. Symmetry Check
        val leftKneeAngle = PoseGeometry.calculateAngle2D(leftHip, leftKnee, leftAnkle)
        val rightKneeAngle = PoseGeometry.calculateAngle2D(rightHip, rightKnee, rightAnkle)
        if (leftKneeAngle != null && rightKneeAngle != null) {
            val diff = kotlin.math.abs(leftKneeAngle - rightKneeAngle)
            if (diff > 18f) {
                feedbacks.add(
                    CoachFeedback(
                        message = "Asymmetric Loading (%.0f° diff)".format(diff),
                        correction = "Distribute weight evenly between both feet",
                        severity = CoachSeverity.INFO
                    )
                )
            }
        }

        return feedbacks
    }
}
