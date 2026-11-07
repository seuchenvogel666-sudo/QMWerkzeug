package de.qmwerkzeuge

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val TBrand = Color(0xFF1D4ED8)
private val TBg = Color(0xFFF4F5F7)
private val TInk = Color(0xFF16181D)
private val TMuted = Color(0xFF5B6270)
private val TLine = Color(0xFFE3E6EB)

/** Entwürfe werden pro Werkzeug auf dem Gerät gespeichert. */
object Drafts {
    private fun prefs(ctx: Context) = ctx.getSharedPreferences("entwuerfe", Context.MODE_PRIVATE)

    fun load(ctx: Context, id: String): Map<String, String> {
        val raw = prefs(ctx).getString(id, null) ?: return emptyMap()
        return try {
            val o = JSONObject(raw)
            o.keys().asSequence().associateWith { o.optString(it) }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun save(ctx: Context, id: String, values: Map<String, String>) {
        prefs(ctx).edit().putString(id, JSONObject(values).toString()).apply()
    }

    fun clear(ctx: Context, id: String) {
        prefs(ctx).edit().remove(id).apply()
    }
}

private fun today(): String = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))

private fun fileName(tool: Tool, filled: Boolean): String {
    val base = tool.name
        .replace(Regex("[^A-Za-z0-9ÄÖÜäöüß]+"), "_")
        .trim('_')
    val date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    return if (filled) "${base}_$date.pdf" else "${base}_Vorlage_leer.pdf"
}

@Composable
fun TemplateScreen(tool: Tool, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val template = remember(tool.id) { templateFor(tool.id) }
    val values = remember(tool.id) {
        mutableStateMapOf<String, String>().apply {
            putAll(Drafts.load(ctx, tool.id))
            if (this["h3"].isNullOrEmpty()) this["h3"] = today()
        }
    }
    var pendingFilled by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun update(key: String, v: String) {
        values[key] = v
        Drafts.save(ctx, tool.id, values)
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri: Uri? ->
        if (uri != null) {
            val filled = pendingFilled
            val snapshot = HashMap(values)
            busy = true
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching { PdfExporter.write(ctx, uri, tool, template, snapshot, filled) }
                }
                busy = false
                val msg = if (result.isSuccess) {
                    if (filled) "PDF gespeichert" else "Leere Vorlage gespeichert"
                } else {
                    "Fehler beim Speichern: ${result.exceptionOrNull()?.message}"
                }
                Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    fun saveFilled() {
        computeValues(tool.id, template, values)
        Drafts.save(ctx, tool.id, values)
        pendingFilled = true
        launcher.launch(fileName(tool, true))
    }

    fun saveEmpty() {
        pendingFilled = false
        launcher.launch(fileName(tool, false))
    }

    BackHandler(onBack = onBack)

    Scaffold(
        containerColor = TBg,
        topBar = {
            Surface(color = Color.White) {
                Column {
                    Row(
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück", tint = TInk)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("VORLAGE", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TMuted, letterSpacing = 0.5.sp)
                            Text(tool.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TInk, lineHeight = 22.sp)
                        }
                        TextButton(onClick = { saveFilled() }, enabled = !busy) {
                            Text("PDF", fontWeight = FontWeight.SemiBold, color = TBrand)
                        }
                    }
                    HorizontalDivider(color = TLine)
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Felder ausfüllen und als PDF speichern. Eingaben bleiben auf dem Gerät gespeichert, bis du sie leerst.",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = TMuted,
            )

            FormCard {
                headerLabels.forEachIndexed { i, label ->
                    Field(label, values["h$i"].orEmpty(), lines = 1) { update("h$i", it) }
                }
            }

            template.sections.forEachIndexed { i, sec ->
                when (sec) {
                    is LineSec -> FormCard {
                        Field(sec.label, values[secKey(i)].orEmpty(), lines = 1) { update(secKey(i), it) }
                    }
                    is TextSec -> FormCard {
                        Field(sec.label, values[secKey(i)].orEmpty(), lines = sec.lines) { update(secKey(i), it) }
                    }
                    is NoteSec -> Text(sec.label, fontSize = 13.sp, lineHeight = 19.sp, color = TMuted)
                    is TableSec -> {
                        val rows = rowCount(values, i, sec)
                        Text(sec.label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TInk)
                        for (r in 0 until rows) {
                            FormCard {
                                Text("Zeile ${r + 1}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TMuted)
                                sec.cols.forEachIndexed { c, col ->
                                    val k = cellKey(i, r, c)
                                    Field(col, cellValue(values, i, r, c, sec), lines = 1) { update(k, it) }
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (rows < 60) {
                                OutlinedButton(
                                    onClick = { update(rowsKey(i), (rows + 1).toString()) },
                                    shape = RoundedCornerShape(12.dp),
                                ) { Text("+ Zeile") }
                            }
                            if (rows > 1) {
                                TextButton(onClick = {
                                    val last = rows - 1
                                    sec.cols.indices.forEach { c -> values.remove(cellKey(i, last, c)) }
                                    update(rowsKey(i), last.toString())
                                }) { Text("Letzte Zeile entfernen", color = TMuted) }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            HorizontalDivider(color = TLine)

            Button(
                onClick = { saveFilled() },
                enabled = !busy,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                if (busy) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Ausgefüllt als PDF speichern", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            OutlinedButton(
                onClick = { saveEmpty() },
                enabled = !busy,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFC7D2F5)),
            ) {
                Text(
                    "Leere Vorlage mit Eingabefeldern (PDF)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TBrand,
                )
            }
            Text(
                "Die leere Vorlage ist ein PDF-Formular: Die Felder lassen sich in jedem PDF-Programm " +
                    "(z. B. Adobe Acrobat Reader, auch auf dem Handy) ausfüllen oder ausgedruckt von Hand.",
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = TMuted,
            )
            TextButton(onClick = { confirmClear = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Alle Eingaben leeren", color = Color(0xFFB91C1C))
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Eingaben leeren?") },
            text = { Text("Alle Eingaben in dieser Vorlage werden gelöscht.") },
            confirmButton = {
                TextButton(onClick = {
                    values.clear()
                    values["h3"] = today()
                    Drafts.clear(ctx, tool.id)
                    confirmClear = false
                }) { Text("Leeren", color = Color(0xFFB91C1C)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Abbrechen") }
            },
        )
    }
}

@Composable
private fun FormCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, TLine, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        content()
    }
}

@Composable
private fun Field(label: String, value: String, lines: Int, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        minLines = lines,
        singleLine = false,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = TLine,
            focusedBorderColor = TBrand,
        ),
    )
}
