package com.noto.app.util

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.noto.app.data.local.entity.Note
import java.io.OutputStream

/**
 * 100% Native, Zero-Dependency PDF Exporter.
 * Generates clean, Apple-minimalist, multi-page PDF documents from any Note
 * using Android's built-in android.graphics.pdf.PdfDocument.
 */
object PdfExporter {

    private const val PAGE_WIDTH = 595 // Standard A4 points (8.27 in * 72 pt/in)
    private const val PAGE_HEIGHT = 842 // Standard A4 points (11.69 in * 72 pt/in)
    private const val MARGIN = 50f
    private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2)

    fun exportNoteToPdf(note: Note, outputStream: OutputStream) {
        val document = PdfDocument()
        try {
            val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val metaPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        val bodyPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val lineHeight = 18f
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        var yCursor = MARGIN + 20f

        // Draw Title
        val titleText = if (note.title.isNotBlank()) note.title else "Untitled Note"
        val wrappedTitle = wrapText(titleText, titlePaint, CONTENT_WIDTH)
        for (line in wrappedTitle) {
            canvas.drawText(line, MARGIN, yCursor, titlePaint)
            yCursor += 26f
        }

        yCursor += 4f

        // Draw Meta (Date & Word Count)
        val metaText = "Noto • ${DateFormatter.formatEditor(note.updatedAt)} • ${note.wordCount} words • ${note.charCount} chars"
        canvas.drawText(metaText, MARGIN, yCursor, metaPaint)
        yCursor += 14f

        // Draw separator line
        canvas.drawLine(MARGIN, yCursor, PAGE_WIDTH - MARGIN, yCursor, linePaint)
        yCursor += 20f

        // Draw Body Text (with auto line wrap and multi-page break)
        val rawLines = note.body.lines()
        for (rawLine in rawLines) {
            val wrappedLines = if (rawLine.isBlank()) listOf("") else wrapText(rawLine, bodyPaint, CONTENT_WIDTH)
            for (line in wrappedLines) {
                if (yCursor + lineHeight > PAGE_HEIGHT - MARGIN) {
                    // Page full -> start new page
                    document.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    yCursor = MARGIN + 20f
                }

                if (line.isNotBlank()) {
                    canvas.drawText(line, MARGIN, yCursor, bodyPaint)
                }
                yCursor += lineHeight
            }
        }

        document.finishPage(page)
        document.writeTo(outputStream)
        } finally {
            document.close()
        }
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val width = paint.measureText(testLine)
            if (width <= maxWidth) {
                currentLine.append(if (currentLine.isEmpty()) word else " $word")
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString())
                    currentLine = StringBuilder(word)
                } else {
                    lines.add(word)
                }
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        return lines
    }
}
