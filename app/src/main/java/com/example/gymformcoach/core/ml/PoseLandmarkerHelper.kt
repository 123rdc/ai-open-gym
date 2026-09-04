package com.example.gymformcoach.core.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

class PoseLandmarkerHelper(
    private val context: Context,
    private val listener: LandmarkerListener
) {
    private var poseLandmarker: PoseLandmarker? = null

    init {
        setupPoseLandmarker()
    }

    private fun setupPoseLandmarker() {
        val baseOptionsBuilder = BaseOptions.builder()
            .setModelAssetPath("pose_landmarker_lite.task")

        try {
            baseOptionsBuilder.setDelegate(Delegate.GPU)
            val options = createOptions(baseOptionsBuilder.build())
            poseLandmarker = PoseLandmarker.createFromOptions(context, options)
        } catch (e: Exception) {
            Log.w(TAG, "GPU delegate not supported, falling back to CPU")
            try {
                baseOptionsBuilder.setDelegate(Delegate.CPU)
                val options = createOptions(baseOptionsBuilder.build())
                poseLandmarker = PoseLandmarker.createFromOptions(context, options)
            } catch (e2: Exception) {
                listener.onError("Pose landmarker failed to initialize: ${e2.message}")
                Log.e(TAG, "MediaPipe failed to load the task with error: " + e2.message)
            }
        }
    }

    private fun createOptions(baseOptions: BaseOptions): PoseLandmarker.PoseLandmarkerOptions {
        return PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setResultListener(this::returnLivestreamResult)
            .setErrorListener(this::returnLivestreamError)
            .build()
    }

    fun detectLiveStream(imageProxy: ImageProxy, isFrontCamera: Boolean) {
        val frameTime = SystemClock.uptimeMillis()

        // Copy out RGB bits from the frame to a bitmap
        val bitmapBuffer = Bitmap.createBitmap(
            imageProxy.width,
            imageProxy.height,
            Bitmap.Config.ARGB_8888
        )
        imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }
        imageProxy.close()

        val matrix = Matrix().apply {
            // Rotate the frame received from the camera to be in the same orientation as it'll be shown
            postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())

            // flip image if user use front camera
            if (isFrontCamera) {
                postScale(
                    -1f,
                    1f,
                    imageProxy.width.toFloat() / 2,
                    imageProxy.height.toFloat() / 2
                )
            }
        }
        val rotatedBitmap = Bitmap.createBitmap(
            bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height,
            matrix, true
        )

        val mpImage = BitmapImageBuilder(rotatedBitmap).build()
        poseLandmarker?.detectAsync(mpImage, frameTime)
    }

    private fun returnLivestreamResult(
        result: PoseLandmarkerResult,
        input: MPImage
    ) {
        val finishTimeMs = SystemClock.uptimeMillis()
        val inferenceTime = finishTimeMs - result.timestampMs()

        listener.onResults(
            result,
            inferenceTime,
            input.height,
            input.width
        )
    }

    private fun returnLivestreamError(error: RuntimeException) {
        listener.onError(error.message ?: "An unknown error has occurred")
    }

    fun close() {
        poseLandmarker?.close()
        poseLandmarker = null
    }

    interface LandmarkerListener {
        fun onError(error: String)
        fun onResults(
            result: PoseLandmarkerResult,
            inferenceTime: Long,
            inputImageHeight: Int,
            inputImageWidth: Int
        )
    }

    companion object {
        private const val TAG = "PoseLandmarkerHelper"
    }
}
