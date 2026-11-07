package de.qmwerkzeuge

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.cos.COSDictionary
import com.tom_roush.pdfbox.cos.COSName
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.PDResources
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDFont
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDColor
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDDeviceRGB
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAppearanceCharacteristicsDictionary
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDAcroForm
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDTextField

/**
 * Erzeugt eine Vorlage als PDF (A4).
 * filled = true: mit den eingetragenen Werten als fester Text.
 * filled = false: leer, mit ausfüllbaren Eingabefeldern (PDF-Formular).
 */
object PdfExporter {

    fun write(
        context: Context,
        uri: Uri,
        tool: Tool,
        template: Template,
        values: Map<String, String>,
        filled: Boolean,
    ) {
        PDDocument().use { doc ->
            val w = PdfWriter(doc, filled)
            w.render(tool, template, values)
            w.finish()
            val out = context.contentResolver.openOutputStream(uri)
                ?: error("Datei konnte nicht geöffnet werden")
            out.use { doc.save(it) }
        }
    }
}

private class Rgb(val r: Float, val g: Float, val b: Float)

private val INK = Rgb(0.086f, 0.094f, 0.114f)
private val MUTED = Rgb(0.357f, 0.384f, 0.439f)
private val BRAND = Rgb(0.114f, 0.306f, 0.847f)
private val LINE = Rgb(0.835f, 0.855f, 0.882f)
private val HEAD_BG = Rgb(0.910f, 0.933f, 0.988f)
private val FIELD_BG = Rgb(0.965f, 0.973f, 0.988f)

private class PdfWriter(private val doc: PDDocument, private val filled: Boolean) {

    private val reg: PDFont = PDType1Font.HELVETICA
    private val bold: PDFont = PDType1Font.HELVETICA_BOLD

    private val pageW = PDRectangle.A4.width
    private val pageH = PDRectangle.A4.height
    private val margin = 40f
    private val contentW = pageW - 2 * margin
    private val bottomLimit = pageH - margin - 24f

    private var page: PDPage? = null
    private var cs: PDPageContentStream? = null
    private var y = 0f // Abstand von oben
    private var pageNo = 0
    private var fieldNo = 0
    private var footerText = "QM-Werkzeuge"

    private val acro: PDAcroForm? = if (filled) null else createAcroForm()

    private fun createAcroForm(): PDAcroForm {
        val form = PDAcroForm(doc)
        doc.documentCatalog.setAcroForm(form)
        val res = PDResources()
        res.put(COSName.getPDFName("Helv"), reg)
        form.setDefaultResources(res)
        form.setDefaultAppearance("/Helv 0 Tf 0 g")
        form.setNeedAppearances(true)
        return form
    }

    // ---------------- Seiten ----------------

    private fun newPage() {
        cs?.let {
            drawFooter(it)
            it.close()
        }
        val p = PDPage(PDRectangle.A4)
        doc.addPage(p)
        page = p
        cs = PDPageContentStream(doc, p)
        pageNo++
        y = margin
    }

    fun finish() {
        cs?.let {
            drawFooter(it)
            it.close()
        }
        cs = null
    }

    private fun ensure(h: Float) {
        if (cs == null || y + h > bottomLimit) newPage()
    }

    private fun drawFooter(c: PDPageContentStream) {
        val t = clean("$footerText  ·  Seite $pageNo", reg)
        c.beginText()
        c.setFont(reg, 8f)
        c.setNonStrokingColor(MUTED.r, MUTED.g, MUTED.b)
        c.newLineAtOffset(margin, 22f)
        c.showText(t)
        c.endText()
    }

    // ---------------- Grundfunktionen ----------------

    private val okChars = HashMap<Char, Boolean>()

    /** Ersetzt Zeichen, die die Standardschrift nicht kennt. */
    private fun clean(s: String, font: PDFont): String {
        val sb = StringBuilder()
        for (ch in s) {
            when (ch) {
                '\t' -> sb.append("  ")
                '\r' -> {}
                '→' -> sb.append("->")
                '≤' -> sb.append("<=")
                '≥' -> sb.append(">=")
                '−' -> sb.append('-')
                '△' -> sb.append("(schwach)")
                '○' -> sb.append('o')
                '●' -> sb.append("(x)")
                ' ' -> sb.append(' ')
                else -> {
                    val ok = okChars.getOrPut(ch) {
                        try {
                            font.encode(ch.toString())
                            true
                        } catch (e: Exception) {
                            false
                        }
                    }
                    sb.append(if (ok) ch else '?')
                }
            }
        }
        return sb.toString()
    }

    private fun width(s: String, font: PDFont, size: Float): Float =
        font.getStringWidth(s) / 1000f * size

    /** Bricht Text auf eine Breite um (berücksichtigt Zeilenumbrüche). */
    private fun wrap(text: String, font: PDFont, size: Float, maxW: Float): List<String> {
        val out = mutableListOf<String>()
        for (para in clean(text, font).split('\n')) {
            if (para.isBlank()) {
                out += ""
                continue
            }
            var cur = ""
            for (word in para.split(' ')) {
                val cand = if (cur.isEmpty()) word else "$cur $word"
                if (width(cand, font, size) <= maxW) {
                    cur = cand
                } else {
                    if (cur.isNotEmpty()) out += cur
                    // sehr lange Wörter zeichenweise teilen
                    var rest = word
                    while (width(rest, font, size) > maxW && rest.length > 1) {
                        var n = rest.length
                        while (n > 1 && width(rest.substring(0, n), font, size) > maxW) n--
                        out += rest.substring(0, n)
                        rest = rest.substring(n)
                    }
                    cur = rest
                }
            }
            out += cur
        }
        return out
    }

    private fun text(s: String, x: Float, baselineFromTop: Float, font: PDFont, size: Float, color: Rgb) {
        val c = cs ?: return
        c.beginText()
        c.setFont(font, size)
        c.setNonStrokingColor(color.r, color.g, color.b)
        c.newLineAtOffset(x, pageH - baselineFromTop)
        c.showText(clean(s, font))
        c.endText()
    }

    private fun fillRect(x: Float, top: Float, w: Float, h: Float, color: Rgb) {
        val c = cs ?: return
        c.setNonStrokingColor(color.r, color.g, color.b)
        c.addRect(x, pageH - top - h, w, h)
        c.fill()
    }

    private fun strokeRect(x: Float, top: Float, w: Float, h: Float, color: Rgb, lw: Float = 0.6f) {
        val c = cs ?: return
        c.setStrokingColor(color.r, color.g, color.b)
        c.setLineWidth(lw)
        c.addRect(x, pageH - top - h, w, h)
        c.stroke()
    }

    private fun hLine(x: Float, top: Float, w: Float, color: Rgb, lw: Float) {
        val c = cs ?: return
        c.setStrokingColor(color.r, color.g, color.b)
        c.setLineWidth(lw)
        c.moveTo(x, pageH - top)
        c.lineTo(x + w, pageH - top)
        c.stroke()
    }

    /** Ausfüllbares Feld (nur bei leerer Vorlage). */
    private fun field(x: Float, top: Float, w: Float, h: Float, multiline: Boolean, fontSize: Int) {
        val form = acro ?: return
        val p = page ?: return
        fillRect(x, top, w, h, FIELD_BG)
        strokeRect(x, top, w, h, LINE)
        val f = PDTextField(form)
        fieldNo++
        f.setPartialName("feld_$fieldNo")
        f.setDefaultAppearance("/Helv $fontSize Tf 0 g")
        if (multiline) f.setMultiline(true)
        form.getFields().add(f)
        val wd = f.getWidgets()[0]
        wd.setRectangle(PDRectangle(x + 1f, pageH - top - h + 1f, w - 2f, h - 2f))
        wd.setPage(p)
        wd.setPrinted(true)
        val ac = PDAppearanceCharacteristicsDictionary(COSDictionary())
        ac.setBackground(PDColor(floatArrayOf(FIELD_BG.r, FIELD_BG.g, FIELD_BG.b), PDDeviceRGB.INSTANCE))
        wd.setAppearanceCharacteristics(ac)
        p.getAnnotations().add(wd)
    }

    // ---------------- Inhalt ----------------

    fun render(tool: Tool, t: Template, values: Map<String, String>) {
        footerText = "QM-Werkzeuge  ·  ${tool.name}"
        newPage()

        // Titel
        text(tool.name, margin, y + 18f, bold, 18f, INK)
        y += 26f
        val sub = if (filled) "Ausgefüllte Vorlage" else "Vorlage zum Ausfüllen"
        text("$sub  ·  ${tool.kind}", margin, y + 9f, reg, 9f, MUTED)
        y += 16f
        hLine(margin, y, contentW, BRAND, 1.5f)
        y += 14f

        // Kopfzeilen als 2 × 2 Raster
        val colW = (contentW - 12f) / 2f
        for (row in 0 until 2) {
            ensure(42f)
            for (col in 0 until 2) {
                val i = row * 2 + col
                val x = margin + col * (colW + 12f)
                labelAt(headerLabels[i], x, y)
                valueBox(values["h$i"].orEmpty(), x, y + 12f, colW, 20f, multiline = false)
            }
            y += 12f + 20f + 10f
        }
        y += 4f

        t.sections.forEachIndexed { i, sec ->
            when (sec) {
                is LineSec -> {
                    ensure(44f)
                    labelAt(sec.label, margin, y, bold = true)
                    valueBox(values[secKey(i)].orEmpty(), margin, y + 13f, contentW, 20f, multiline = false)
                    y += 13f + 20f + 12f
                }
                is TextSec -> {
                    val v = values[secKey(i)].orEmpty()
                    val minH = sec.lines * 14f + 8f
                    val h = if (filled) {
                        maxOf(minH, wrap(v, reg, 10f, contentW - 12f).size * 13f + 10f)
                    } else {
                        minH
                    }
                    ensure(13f + h + 12f)
                    labelAt(sec.label, margin, y, bold = true)
                    valueBox(v, margin, y + 13f, contentW, h, multiline = true)
                    y += 13f + h + 12f
                }
                is NoteSec -> {
                    val lines = wrap(sec.label, reg, 8.5f, contentW)
                    ensure(lines.size * 11f + 8f)
                    lines.forEach {
                        text(it, margin, y + 9f, reg, 8.5f, MUTED)
                        y += 11f
                    }
                    y += 8f
                }
                is TableSec -> table(i, sec, values)
            }
        }
    }

    private fun labelAt(label: String, x: Float, top: Float, bold: Boolean = false) {
        text(label, x, top + 9f, if (bold) this.bold else reg, if (bold) 10f else 8.5f, if (bold) INK else MUTED)
    }

    /** Wert als Text (ausgefüllt) oder Eingabefeld (leer). */
    private fun valueBox(v: String, x: Float, top: Float, w: Float, h: Float, multiline: Boolean) {
        if (filled) {
            fillRect(x, top, w, h, Rgb(1f, 1f, 1f))
            strokeRect(x, top, w, h, LINE)
            if (multiline) {
                var ty = top + 14f
                for (l in wrap(v, reg, 10f, w - 12f)) {
                    text(l, x + 6f, ty, reg, 10f, INK)
                    ty += 13f
                }
            } else {
                val l = wrap(v.replace('\n', ' '), reg, 10f, w - 12f).firstOrNull().orEmpty()
                text(l, x + 6f, top + h / 2f + 3.5f, reg, 10f, INK)
            }
        } else {
            field(x, top, w, h, multiline, if (multiline) 10 else 0)
        }
    }

    private fun table(i: Int, sec: TableSec, values: Map<String, String>) {
        val total = sec.weights.sum()
        val widths = sec.weights.map { it / total * contentW }
        val headSize = 8f
        val cellSize = 8.5f
        val headLines = sec.cols.mapIndexed { c, name -> wrap(name, bold, headSize, widths[c] - 6f) }
        val headH = headLines.maxOf { it.size } * 10f + 8f

        fun drawHeader() {
            var x = margin
            sec.cols.indices.forEach { c ->
                fillRect(x, y, widths[c], headH, HEAD_BG)
                strokeRect(x, y, widths[c], headH, LINE)
                var ty = y + 11f
                for (l in headLines[c]) {
                    text(l, x + 3f, ty, bold, headSize, INK)
                    ty += 10f
                }
                x += widths[c]
            }
            y += headH
        }

        val rows = if (filled) rowCount(values, i, sec) else sec.rows

        ensure(14f + headH + 20f)
        labelAt(sec.label, margin, y, bold = true)
        y += 15f
        drawHeader()

        for (r in 0 until rows) {
            val cellLines = sec.cols.indices.map { c ->
                val v = if (filled) cellValue(values, i, r, c, sec) else sec.prefill[c]?.getOrNull(r).orEmpty()
                wrap(v, reg, cellSize, widths[c] - 6f)
            }
            val rowH = if (filled) maxOf(18f, cellLines.maxOf { it.size } * 10.5f + 7f) else 20f
            if (y + rowH > bottomLimit) {
                newPage()
                drawHeader()
            }
            var x = margin
            sec.cols.indices.forEach { c ->
                val pre = sec.prefill[c]?.getOrNull(r)
                if (!filled && pre == null) {
                    field(x, y, widths[c], rowH, multiline = false, fontSize = 0)
                } else {
                    strokeRect(x, y, widths[c], rowH, LINE)
                    var ty = y + 12.5f
                    for (l in cellLines[c]) {
                        text(l, x + 3f, ty, reg, cellSize, INK)
                        ty += 10.5f
                    }
                }
                x += widths[c]
            }
            y += rowH
        }
        y += 14f
    }
}
