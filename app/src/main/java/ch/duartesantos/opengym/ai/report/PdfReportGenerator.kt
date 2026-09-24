package ch.duartesantos.opengym.ai.report

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import ch.duartesantos.opengym.ai.model.SquatDepth
import ch.duartesantos.opengym.ai.model.SquatRepData
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    /**
     * Generates a detailed PDF report for a squat analysis set.
     * Returns the generated file Uri for viewing or sharing.
     */
    fun generateSquatReport(
        context: Context,
        exerciseName: String = "Barbell Back Squat",
        reps: List<SquatRepData>
    ): File? {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 dimensions in points: 595 x 842
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // Paints
            val bgPaint = Paint().apply {
                color = Color.rgb(248, 249, 250)
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, 595f, 842f, bgPaint)

            // Header Background Banner
            val headerPaint = Paint().apply {
                color = Color.rgb(18, 18, 22)
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, 595f, 100f, headerPaint)

            // Brand Accent Stripe
            val accentPaint = Paint().apply {
                color = Color.rgb(168, 85, 247) // Purple Accent
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 96f, 595f, 100f, accentPaint)

            // App Title & Header Text
            val titlePaint = Paint().apply {
                color = Color.WHITE
                textSize = 20f
                isFakeBoldText = true
                isAntiAlias = true
            }
            canvas.drawText("OpenGym AI Biomechanics", 30f, 45f, titlePaint)

            val subTitlePaint = Paint().apply {
                color = Color.rgb(200, 200, 210)
                textSize = 12f
                isAntiAlias = true
            }
            val dateFormat = SimpleDateFormat("EEEE, MMMM dd, yyyy - HH:mm", Locale.getDefault())
            canvas.drawText("Squat Movement & Depth Analysis Report • ${dateFormat.format(Date())}", 30f, 70f, subTitlePaint)

            // Scorecard Summary Box
            val cardBg = Paint().apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
            val cardBorder = Paint().apply {
                color = Color.rgb(226, 232, 240)
                style = Paint.Style.STROKE
                strokeWidth = 1.5f
            }

            val summaryRect = RectF(30f, 120f, 565f, 210f)
            canvas.drawRoundRect(summaryRect, 12f, 12f, cardBg)
            canvas.drawRoundRect(summaryRect, 12f, 12f, cardBorder)

            val passedReps = reps.count { it.depth == SquatDepth.PARALLEL || it.depth == SquatDepth.DEEP }
            val avgScore = if (reps.isNotEmpty()) reps.map { it.formScore }.average().toInt() else 0
            val avgAngle = if (reps.isNotEmpty()) reps.map { it.minKneeAngle }.average().toFloat() else 0f
            val totalDurationSec = (reps.sumOf { it.durationMs } / 100.0) / 10.0

            val metricLabelPaint = Paint().apply {
                color = Color.rgb(100, 116, 139)
                textSize = 10f
                isAntiAlias = true
            }
            val metricValPaint = Paint().apply {
                color = Color.rgb(15, 23, 42)
                textSize = 22f
                isFakeBoldText = true
                isAntiAlias = true
            }
            val greenValPaint = Paint().apply {
                color = Color.rgb(34, 197, 94)
                textSize = 22f
                isFakeBoldText = true
                isAntiAlias = true
            }

            // Metric 1: Total Reps
            canvas.drawText("TOTAL REPS", 55f, 150f, metricLabelPaint)
            canvas.drawText("${reps.size}", 55f, 182f, metricValPaint)

            // Metric 2: Valid Depth
            canvas.drawText("PARALLEL DEPTH", 175f, 150f, metricLabelPaint)
            canvas.drawText("$passedReps / ${reps.size}", 175f, 182f, greenValPaint)

            // Metric 3: Avg Score
            canvas.drawText("AVG FORM SCORE", 310f, 150f, metricLabelPaint)
            canvas.drawText("$avgScore%", 310f, 182f, metricValPaint)

            // Metric 4: Avg Knee Flexion
            canvas.drawText("AVG KNEE ANGLE", 440f, 150f, metricLabelPaint)
            canvas.drawText("%.0f°".format(avgAngle), 440f, 182f, metricValPaint)

            // Table Section Header
            val sectionHeadingPaint = Paint().apply {
                color = Color.rgb(30, 41, 59)
                textSize = 14f
                isFakeBoldText = true
                isAntiAlias = true
            }
            canvas.drawText("REPETITION BY REPETITION BREAKDOWN", 30f, 240f, sectionHeadingPaint)

            // Table Header Bar
            val tableHeaderBg = Paint().apply {
                color = Color.rgb(241, 245, 249)
                style = Paint.Style.FILL
            }
            val tableHeaderRect = RectF(30f, 255f, 565f, 280f)
            canvas.drawRoundRect(tableHeaderRect, 6f, 6f, tableHeaderBg)

            val tableHeadText = Paint().apply {
                color = Color.rgb(71, 85, 105)
                textSize = 9.5f
                isFakeBoldText = true
                isAntiAlias = true
            }

            canvas.drawText("REP #", 42f, 272f, tableHeadText)
            canvas.drawText("MIN ANGLE", 92f, 272f, tableHeadText)
            canvas.drawText("DEPTH CALL", 172f, 272f, tableHeadText)
            canvas.drawText("ROM FLEXION", 282f, 272f, tableHeadText)
            canvas.drawText("DURATION", 382f, 272f, tableHeadText)
            canvas.drawText("FORM SCORE", 465f, 272f, tableHeadText)

            // Table Rows
            var rowY = 302f
            val cellText = Paint().apply {
                color = Color.rgb(30, 41, 59)
                textSize = 9f
                isAntiAlias = true
            }
            val passText = Paint().apply {
                color = Color.rgb(22, 163, 74)
                textSize = 9f
                isFakeBoldText = true
                isAntiAlias = true
            }
            val failText = Paint().apply {
                color = Color.rgb(220, 38, 38)
                textSize = 9f
                isFakeBoldText = true
                isAntiAlias = true
            }
            val rowDivider = Paint().apply {
                color = Color.rgb(241, 245, 249)
                strokeWidth = 1f
            }

            for (rep in reps.take(12)) {
                canvas.drawText("Rep ${rep.repNumber}", 42f, rowY, cellText)
                canvas.drawText("%.0f°".format(rep.minKneeAngle), 92f, rowY, cellText)

                val isPass = rep.depth == SquatDepth.PARALLEL || rep.depth == SquatDepth.DEEP
                canvas.drawText(
                    rep.depth.displayName.substringBefore("("),
                    172f,
                    rowY,
                    if (isPass) passText else failText
                )

                canvas.drawText("%.0f°".format(rep.maxFlexionRom), 282f, rowY, cellText)
                canvas.drawText("%.1fs".format(rep.durationMs / 1000f), 382f, rowY, cellText)

                val scoreColor = if (rep.formScore >= 80) passText else failText
                canvas.drawText("${rep.formScore}%", 465f, rowY, scoreColor)

                // Feedback row below if exists
                if (rep.coachingFeedback.isNotEmpty()) {
                    val feedbackPaint = Paint().apply {
                        color = Color.rgb(100, 116, 139)
                        textSize = 8f
                        isAntiAlias = true
                    }
                    canvas.drawText("⤷ ${rep.coachingFeedback.first()}", 92f, rowY + 12f, feedbackPaint)
                    rowY += 14f
                }

                canvas.drawLine(30f, rowY + 6f, 565f, rowY + 6f, rowDivider)
                rowY += 22f
            }

            // Coaching Recommendations Card at bottom
            val adviceBoxTop = 640f
            val adviceRect = RectF(30f, adviceBoxTop, 565f, 775f)
            canvas.drawRoundRect(adviceRect, 10f, 10f, cardBg)
            canvas.drawRoundRect(adviceRect, 10f, 10f, cardBorder)

            canvas.drawText("AI COACHING INSIGHTS & ACTION ITEMS", 45f, adviceBoxTop + 25f, sectionHeadingPaint)

            val advicePaint = Paint().apply {
                color = Color.rgb(51, 65, 85)
                textSize = 9.5f
                isAntiAlias = true
            }

            val depthRate = if (reps.isNotEmpty()) (passedReps * 100) / reps.size else 0
            val depthAdvice = if (depthRate >= 80) {
                "• Squat Depth: Excellent powerlifting standard depth achieved in $depthRate% of reps."
            } else {
                "• Squat Depth: Target deeper knee flexion; push the hip crease below top of knee."
            }
            canvas.drawText(depthAdvice, 45f, adviceBoxTop + 50f, advicePaint)

            val valgusCount = reps.count { it.kneeValgusDetected }
            val valgusAdvice = if (valgusCount == 0) {
                "• Knee Alignment: Optimal hip-knee-ankle tracking preserved on every repetition."
            } else {
                "• Knee Cave Detected: Knees collapsed inward on $valgusCount rep(s). Drive knees out over toes."
            }
            canvas.drawText(valgusAdvice, 45f, adviceBoxTop + 72f, advicePaint)

            val tempoAdvice = "• Rep Cadence: Average rep tempo was %.1fs with smooth eccentric control.".format(
                if (reps.isNotEmpty()) (reps.map { it.durationMs }.average() / 1000.0) else 2.0
            )
            canvas.drawText(tempoAdvice, 45f, adviceBoxTop + 94f, advicePaint)

            // Footer
            val footerPaint = Paint().apply {
                color = Color.rgb(148, 163, 184)
                textSize = 8.5f
                isAntiAlias = true
            }
            canvas.drawText("Generated by OpenGym AI • Private on-device biomechanical analytics", 30f, 810f, footerPaint)

            pdfDocument.finishPage(page)

            // Save PDF to cache/reports directory
            val reportsDir = File(context.cacheDir, "reports")
            if (!reportsDir.exists()) {
                reportsDir.mkdirs()
            }
            val fileName = "Squat_Analysis_${System.currentTimeMillis()}.pdf"
            val file = File(reportsDir, fileName)
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDocument.close()

            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun sharePdf(context: Context, pdfFile: File) {
        val fileUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra(Intent.EXTRA_SUBJECT, "OpenGym AI Squat Analysis Report")
            putExtra(Intent.EXTRA_TEXT, "Here is my squat form analysis and rep count report generated by OpenGym AI.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Share Squat Report PDF")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun viewPdf(context: Context, pdfFile: File) {
        val fileUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            sharePdf(context, pdfFile)
        }
    }
}
