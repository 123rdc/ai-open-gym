package com.example.gymformcoach.core.export

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import java.io.FileOutputStream
import java.io.IOException

/**
 * §17B.3: renders the plan directly onto a PDF canvas rather than
 * screenshotting UI - a screen-sized screenshot prints badly, and the output
 * needs to work on paper regardless of the app's current theme (light
 * background, legible at A4, no accent-color-dependent information).
 */
class PlanPdfDocumentAdapter(private val share: PlanShare) : PrintDocumentAdapter() {

    // A4 at 72dpi-equivalent PDF points.
    private val pageWidth = 595
    private val pageHeight = 842

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback,
        extras: android.os.Bundle?
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback.onLayoutCancelled()
            return
        }
        val info = PrintDocumentInfo.Builder("workout_plan.pdf")
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(1)
            .build()
        callback.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback
    ) {
        val document = PdfDocument()
        try {
            val page = document.startPage(
                PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            )
            draw(page.canvas)
            document.finishPage(page)
            document.writeTo(FileOutputStream(destination.fileDescriptor))
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: IOException) {
            callback.onWriteFailed(e.message)
        } finally {
            document.close()
        }
    }

    private fun draw(canvas: Canvas) {
        canvas.drawColor(Color.WHITE)
        val margin = 40f
        var y = margin

        val titlePaint = Paint().apply { color = Color.BLACK; textSize = 22f; isFakeBoldText = true }
        canvas.drawText("Workout Plan", margin, y + 20f, titlePaint)
        y += 45f

        val labelPaint = Paint().apply { color = Color.DKGRAY; textSize = 11f }
        canvas.drawText(
            "Shared " + java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault()).format(java.util.Date(share.sharedAt)),
            margin, y, labelPaint
        )
        y += 30f

        val headingPaint = Paint().apply { color = Color.BLACK; textSize = 15f; isFakeBoldText = true }
        val bodyPaint = Paint().apply { color = Color.BLACK; textSize = 12f }
        val smallPaint = Paint().apply { color = Color.DKGRAY; textSize = 10f }

        share.routines.forEach { routine ->
            if (y > pageHeight - 80f) return@forEach // single page for now; overflow is truncated rather than crashing
            canvas.drawText(routine.name, margin, y, headingPaint)
            y += 18f
            if (routine.description.isNotBlank()) {
                canvas.drawText(routine.description, margin, y, smallPaint)
                y += 14f
            }

            val groupLabels = routine.exercises.mapNotNull { it.supersetGroupId }.distinct()
                .withIndex().associate { (i, id) -> id to "Superset ${('A' + i)}" }

            routine.exercises.sortedBy { it.orderIndex }.forEach { exercise ->
                val groupTag = exercise.supersetGroupId?.let { " [${groupLabels[it]}]" } ?: ""
                canvas.drawText(
                    "  • ${exercise.exerciseId} - ${exercise.targetSets}×${exercise.targetReps} @ ${exercise.targetWeightKg}kg$groupTag",
                    margin, y, bodyPaint
                )
                y += 16f
            }
            y += 12f
        }
    }
}
