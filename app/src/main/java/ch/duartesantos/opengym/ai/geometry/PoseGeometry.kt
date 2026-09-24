package ch.duartesantos.opengym.ai.geometry

import ch.duartesantos.opengym.ai.model.PoseLandmark
import kotlin.math.*

object PoseGeometry {

    /**
     * Calculates angle at point B formed by line segments AB and CB in degrees.
     * Returns angle in range [0, 180].
     */
    fun calculateAngle2D(a: PoseLandmark?, b: PoseLandmark?, c: PoseLandmark?): Float? {
        if (a == null || b == null || c == null) return null

        val abx = a.x - b.x
        val aby = a.y - b.y

        val cbx = c.x - b.x
        val cby = c.y - b.y

        val magAB = sqrt(abx * abx + aby * aby)
        val magCB = sqrt(cbx * cbx + cby * cby)

        if (magAB == 0f || magCB == 0f) return null

        val dot = abx * cbx + aby * cby
        val cosine = (dot / (magAB * magCB)).coerceIn(-1.0f, 1.0f)

        return (acos(cosine) * (180.0 / Math.PI)).toFloat()
    }

    /**
     * Calculates 3D angle at point B formed by AB and CB.
     */
    fun calculateAngle3D(a: PoseLandmark?, b: PoseLandmark?, c: PoseLandmark?): Float? {
        if (a == null || b == null || c == null) return null

        val abx = a.x - b.x
        val aby = a.y - b.y
        val abz = a.z - b.z

        val cbx = c.x - b.x
        val cby = c.y - b.y
        val cbz = c.z - b.z

        val magAB = sqrt(abx * abx + aby * aby + abz * abz)
        val magCB = sqrt(cbx * cbx + cby * cby + cbz * cbz)

        if (magAB == 0f || magCB == 0f) return null

        val dot = abx * cbx + aby * cby + abz * cbz
        val cosine = (dot / (magAB * magCB)).coerceIn(-1.0f, 1.0f)

        return (acos(cosine) * (180.0 / Math.PI)).toFloat()
    }

    /**
     * Calculates torso inclination angle relative to vertical (0 = perfectly upright, 90 = horizontal).
     */
    fun calculateTorsoInclination(shoulder: PoseLandmark?, hip: PoseLandmark?): Float? {
        if (shoulder == null || hip == null) return null
        val dx = (shoulder.x - hip.x).toDouble()
        val dy = (hip.y - shoulder.y).toDouble() // In screen coords, y increases downwards

        val angleRad = atan2(abs(dx), dy)
        return (angleRad * (180.0 / Math.PI)).toFloat()
    }

    /**
     * Calculates distance between 2 landmarks in 2D space.
     */
    fun distance2D(a: PoseLandmark?, b: PoseLandmark?): Float? {
        if (a == null || b == null) return null
        val dx = a.x - b.x
        val dy = a.y - b.y
        return sqrt(dx * dx + dy * dy)
    }
}
