package ch.duartesantos.opengym.ai.filters

import ch.duartesantos.opengym.ai.model.PoseLandmark
import kotlin.math.PI
import kotlin.math.abs

/**
 * 1€ Filter (One Euro Filter)
 * Direct port of frontend/src/ai/filters/OneEuroFilter.js
 *
 * Designed for noisy real-time signals:
 * - strong smoothing when movement is slow
 * - low latency when movement becomes fast
 */
class OneEuroFilter(
    val minCutoff: Float = 1.0f,
    val beta: Float = 0.005f,
    val dCutoff: Float = 1.0f
) {
    private var xPrev: Float? = null
    private var dxPrev: Float = 0f
    private var tPrev: Long? = null

    private fun alpha(cutoff: Float, frequency: Float): Float {
        val tau = 1.0f / (2.0f * PI.toFloat() * cutoff)
        val te = 1.0f / frequency
        return 1.0f / (1.0f + tau / te)
    }

    fun filter(value: Float, timestampMs: Long): Float {
        if (!value.isFinite()) {
            return xPrev ?: value
        }

        val prevX = xPrev
        val prevT = tPrev

        if (prevT == null || prevX == null) {
            xPrev = value
            tPrev = timestampMs
            dxPrev = 0f
            return value
        }

        val dtSeconds = (timestampMs - prevT) / 1000.0f
        if (!dtSeconds.isFinite() || dtSeconds <= 0f) {
            return prevX
        }

        val frequency = 1.0f / dtSeconds
        val rawDerivative = (value - prevX) / dtSeconds
        val derivativeAlpha = alpha(dCutoff, frequency)
        val derivative = derivativeAlpha * rawDerivative + (1.0f - derivativeAlpha) * dxPrev

        val cutoff = minCutoff + beta * abs(derivative)
        val valueAlpha = alpha(cutoff, frequency)
        val filtered = valueAlpha * value + (1.0f - valueAlpha) * prevX

        xPrev = filtered
        dxPrev = derivative
        tPrev = timestampMs

        return filtered
    }

    fun reset() {
        xPrev = null
        dxPrev = 0f
        tPrev = null
    }
}

class OneEuroLandmarkFilter(
    minCutoff: Float = 1.0f,
    beta: Float = 0.005f,
    dCutoff: Float = 1.0f
) {
    private val filterX = OneEuroFilter(minCutoff, beta, dCutoff)
    private val filterY = OneEuroFilter(minCutoff, beta, dCutoff)
    private val filterZ = OneEuroFilter(minCutoff, beta, dCutoff)

    fun filter(landmark: PoseLandmark?, timestampMs: Long): PoseLandmark? {
        if (landmark == null) return null

        return landmark.copy(
            x = filterX.filter(landmark.x, timestampMs),
            y = filterY.filter(landmark.y, timestampMs),
            z = filterZ.filter(landmark.z, timestampMs)
        )
    }

    fun reset() {
        filterX.reset()
        filterY.reset()
        filterZ.reset()
    }
}

class PoseLandmarkFilter {
    private val filters = Array(33) { OneEuroLandmarkFilter() }

    fun filter(landmarks: Map<Int, PoseLandmark>, timestampMs: Long): Map<Int, PoseLandmark> {
        val filteredMap = mutableMapOf<Int, PoseLandmark>()
        for ((idx, lm) in landmarks) {
            if (idx in 0 until 33) {
                val filtered = filters[idx].filter(lm, timestampMs)
                if (filtered != null) {
                    filteredMap[idx] = filtered
                }
            } else {
                filteredMap[idx] = lm
            }
        }
        return filteredMap
    }

    fun reset() {
        for (f in filters) {
            f.reset()
        }
    }
}
