package com.walltext.app.wallpaper

import android.graphics.*
import android.graphics.drawable.ColorDrawable
import com.walltext.app.model.*
import kotlin.math.max

object WallpaperRenderer {
    fun render(base: Bitmap, config: WallpaperConfig, dynamicIndex: Int = 0): Bitmap {
        val out = base.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(out)
        config.placeholders.take(5).forEach { p -> drawPlaceholder(canvas, p, dynamicIndex, out.width, out.height) }
        return out
    }

    private fun drawPlaceholder(c: Canvas, p: TextPlaceholder, index: Int, w: Int, h: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; alpha = (255 * p.opacity).toInt(); textSize = p.fontSizeSp * (w / 1080f); typeface = Typeface.create("sans", Typeface.NORMAL) }
        val text = p.normalizedEntries().let { if (it.isEmpty()) "" else it[index % it.size] }
        val left = (p.x * w - p.width * w / 2f).coerceIn(0f, w.toFloat())
        val top = (p.y * h).coerceIn(0f, h.toFloat())
        when (p.type) {
            PlaceholderType.TEXT -> drawWrapped(c, text, left, top, p.width * w, paint)
            PlaceholderType.BULLETS -> drawLines(c, text.lines().filter { it.isNotBlank() }.map { "• $it" }, left, top, p.width*w, paint)
            PlaceholderType.CHECKLIST -> drawLines(c, text.lines().filter { it.isNotBlank() }.map { "☐ $it" }, left, top, p.width*w, paint)
            PlaceholderType.TABLE -> drawTable(c, text, left, top, p.width*w, paint)
        }
    }

    private fun drawWrapped(c: Canvas, text: String, x: Float, y: Float, width: Float, p: Paint) {
        val words = text.split(" "); val lines = mutableListOf<String>(); var line = ""
        words.forEach { word -> val candidate = if (line.isEmpty()) word else "$line $word"; if (p.measureText(candidate) > width && line.isNotEmpty()) { lines += line; line = word } else line = candidate }; if (line.isNotEmpty()) lines += line
        drawLines(c, lines, x, y, width, p)
    }
    private fun drawLines(c: Canvas, lines: List<String>, x: Float, y: Float, width: Float, p: Paint) { var yy = y; val gap = p.textSize * 1.35f; lines.forEach { c.drawText(it, x, yy, p); yy += gap } }
    private fun drawTable(c: Canvas, raw: String, x: Float, y: Float, width: Float, p: Paint) {
        val rows = raw.lines().filter { it.isNotBlank() }.map { it.split("|").map(String::trim) }; if (rows.isEmpty()) return
        val cols = max(1, rows.maxOf { it.size }); val cellW = width / cols; val rowH = p.textSize * 1.7f
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = p.color; alpha=p.alpha; style=Paint.Style.STROKE; strokeWidth=1f }
        rows.forEachIndexed { r, row -> for (col in 0 until cols) { val l=x+col*cellW; val t=y+r*rowH; c.drawRect(l,t,l+cellW,t+rowH,stroke); c.drawText(row.getOrNull(col).orEmpty(),l+8,t+p.textSize+3,p) } }
    }
}
