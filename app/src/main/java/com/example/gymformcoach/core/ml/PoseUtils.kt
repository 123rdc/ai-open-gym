package com.example.gymformcoach.core.ml

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

object PoseUtils {
    /**
     * Calculates the angle between three landmarks.
     * @return Angle in degrees [0, 180]
     */
    fun calculateAngle(
        first: NormalizedLandmark,
        middle: NormalizedLandmark,
        last: NormalizedLandmark
    ): Double {
        val angle = Math.toDegrees(
            (atan2(last.y() - middle.y(), last.x() - middle.x()) -
                    atan2(first.y() - middle.y(), first.x() - middle.x())).toDouble()
        )
        var result = abs(angle)
        if (result > 180) {
            result = 360 - result
        }
        return result
    }

    /**
     * Calculates the distance between two landmarks.
     */
    fun getDistance(first: NormalizedLandmark, second: NormalizedLandmark): Float {
        return sqrt((first.x() - second.x()).pow(2) + (first.y() - second.y()).pow(2))
    }

    /**
     * Checks if a point is "below" another in screen coordinates (Y increases downwards).
     */
    fun isBelow(first: NormalizedLandmark, second: NormalizedLandmark): Boolean {
        return first.y() > second.y()
    }

    /** §18.1: minimum MediaPipe visibility score to trust a landmark for rep counting. */
    const val VISIBILITY_THRESHOLD = 0.6f

    /**
     * §18.1: bad landmarks corrupt every downstream layer (rep counts, metrics,
     * LLM analysis, progression, PRs), so confidence is checked before any of
     * that runs, not after. Landmarks without a visibility score at all (some
     * MediaPipe configurations don't populate it) are treated as confident
     * rather than blocking rep counting on a signal that was never available.
     */
    fun isConfident(landmark: NormalizedLandmark, threshold: Float = VISIBILITY_THRESHOLD): Boolean {
        val visibility = runCatching { landmark.visibility() }.getOrNull()
        return visibility == null || !visibility.isPresent || visibility.get() >= threshold
    }

    fun allConfident(vararg landmarks: NormalizedLandmark, threshold: Float = VISIBILITY_THRESHOLD): Boolean =
        landmarks.all { isConfident(it, threshold) }
}
