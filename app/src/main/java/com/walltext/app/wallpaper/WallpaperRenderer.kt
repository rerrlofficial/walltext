package com.walltext.app.wallpaper

import android.graphics.*
import com.walltext.app.model.*
import kotlin.math.max

object WallpaperRenderer {

    fun render(
        base: Bitmap,
        config: WallpaperConfig,
        dynamicIndex: Int = 0,
        textScale: Float = 1f
    ): Bitmap {

        val out =
            base.copy(
                Bitmap.Config.ARGB_8888,
                true
            )

        val canvas = Canvas(out)

        config.placeholders
            .take(5)
            .forEach { placeholder ->

                drawPlaceholder(
                    canvas = canvas,
                    placeholder = placeholder,
                    index = dynamicIndex,
                    width = out.width,
                    height = out.height,
                    textScale = textScale
                )
            }

        return out
    }

    private fun drawPlaceholder(
        canvas: Canvas,
        placeholder: TextPlaceholder,
        index: Int,
        width: Int,
        height: Int,
        textScale: Float
    ) {

        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {

                color = Color.WHITE

                alpha =
                    (255 * placeholder.opacity)
                        .toInt()
                        .coerceIn(0, 255)

                textSize =
                    placeholder.fontSizeSp * textScale

                typeface =
                    Typeface.create(
                        "sans",
                        Typeface.NORMAL
                    )
            }

        val text =
            placeholder
                .normalizedEntries()
                .let { entries ->

                    if (entries.isEmpty()) {

                        ""

                    } else {

                        val entryIndex =
                            if (
                                placeholder.mode ==
                                DisplayMode.DYNAMIC
                            ) {

                                index % entries.size

                            } else {

                                0
                            }

                        entries[entryIndex]
                    }
                }

        val left =
            (
                placeholder.x * width -
                    placeholder.width * width / 2f
                )
                .coerceIn(
                    0f,
                    width.toFloat()
                )

        val top =
            (
                placeholder.y * height
            )
                .coerceIn(
                    0f,
                    height.toFloat()
                )

        when (placeholder.type) {

            PlaceholderType.TEXT ->

                drawWrapped(
                    canvas,
                    text,
                    left,
                    top,
                    placeholder.width * width,
                    paint
                )

            PlaceholderType.BULLETS ->

                drawLines(
                    canvas,
                    text.lines()
                        .filter { it.isNotBlank() }
                        .map { "• $it" },
                    left,
                    top,
                    placeholder.width * width,
                    paint
                )

            PlaceholderType.CHECKLIST ->

                drawLines(
                    canvas,
                    text.lines()
                        .filter { it.isNotBlank() }
                        .map { "☐ $it" },
                    left,
                    top,
                    placeholder.width * width,
                    paint
                )

            PlaceholderType.TABLE ->

                drawTable(
                    canvas,
                    text,
                    left,
                    top,
                    placeholder.width * width,
                    paint
                )
        }
    }

    private fun drawWrapped(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        width: Float,
        paint: Paint
    ) {

        val words =
            text.split(" ")

        val lines =
            mutableListOf<String>()

        var line = ""

        words.forEach { word ->

            val candidate =
                if (line.isEmpty()) {

                    word

                } else {

                    "$line $word"
                }

            if (
                paint.measureText(candidate) > width &&
                line.isNotEmpty()
            ) {

                lines += line
                line = word

            } else {

                line = candidate
            }
        }

        if (line.isNotEmpty()) {
            lines += line
        }

        drawLines(
            canvas,
            lines,
            x,
            y,
            width,
            paint
        )
    }

    private fun drawLines(
        canvas: Canvas,
        lines: List<String>,
        x: Float,
        y: Float,
        width: Float,
        paint: Paint
    ) {

        var currentY = y

        val gap =
            paint.textSize * 1.35f

        lines.forEach { line ->

            canvas.drawText(
                line,
                x,
                currentY,
                paint
            )

            currentY += gap
        }
    }

    private fun drawTable(
        canvas: Canvas,
        raw: String,
        x: Float,
        y: Float,
        width: Float,
        paint: Paint
    ) {

        val rows =
            raw.lines()
                .filter { it.isNotBlank() }
                .map {
                    it.split("|")
                        .map(String::trim)
                }

        if (rows.isEmpty()) return

        val columns =
            max(
                1,
                rows.maxOf { it.size }
            )

        val cellWidth =
            width / columns

        val rowHeight =
            paint.textSize * 1.7f

        val stroke =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {

                color = paint.color
                alpha = paint.alpha
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }

        rows.forEachIndexed { rowIndex, row ->

            for (column in 0 until columns) {

                val left =
                    x + column * cellWidth

                val top =
                    y + rowIndex * rowHeight

                canvas.drawRect(
                    left,
                    top,
                    left + cellWidth,
                    top + rowHeight,
                    stroke
                )

                canvas.drawText(
                    row.getOrNull(column).orEmpty(),
                    left + 8f,
                    top + paint.textSize + 3f,
                    paint
                )
            }
        }
    }
}