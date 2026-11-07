package de.qmwerkzeuge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private val Brand = Color(0xFF1D4ED8)
private val BrandSoft = Color(0xFFE8EEFC)
private val BrandLine = Color(0xFFC7D2F5)
private val Bg = Color(0xFFF4F5F7)
private val Ink = Color(0xFF16181D)
private val Muted = Color(0xFF5B6270)
private val Line = Color(0xFFE3E6EB)
private val LineSoft = Color(0xFFEEF0F3)
private val ChipBg = Color(0xFFF1F3F6)
private val ChipText = Color(0xFF374151)
private val PanelBg = Color(0xFFF7F9FC)
private val PanelLine = Color(0xFFE3E8F2)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        PDFBoxResourceLoader.init(applicationContext)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Brand,
                    onPrimary = Color.White,
                    background = Bg,
                    surface = Color.White,
                    onSurface = Ink,
                ),
            ) {
                QmApp()
            }
        }
    }
}

@Composable
fun QmApp() {
    var query by rememberSaveable { mutableStateOf("") }
    val openCats = remember { mutableStateMapOf<String, Boolean>() }
    val openItems = remember { mutableStateMapOf<String, Boolean>() }
    var example by remember { mutableStateOf<Tool?>(null) }
    var formTool by remember { mutableStateOf<Tool?>(null) }

    formTool?.let { tool ->
        TemplateScreen(tool = tool, onBack = { formTool = null })
        return
    }

    val q = query.trim().lowercase()
    val filtered = categories.mapNotNull { cat ->
        val tools = if (q.isEmpty()) {
            cat.tools
        } else {
            cat.tools.filter { it.name.lowercase().contains(q) || it.desc.lowercase().contains(q) }
        }
        if (tools.isEmpty()) null else cat to tools
    }
    val allOpen = categories.all { openCats[it.id] == true }
    val total = categories.sumOf { it.tools.size }

    Scaffold(
        containerColor = Bg,
        topBar = {
            Header(
                total = total,
                query = query,
                onQueryChange = { query = it },
                allOpen = allOpen,
                onToggleAll = {
                    if (allOpen) {
                        openCats.clear()
                        openItems.clear()
                    } else {
                        categories.forEach { openCats[it.id] = true }
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 16.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (filtered.isEmpty()) {
                item {
                    Text(
                        text = "Kein Werkzeug gefunden.",
                        color = Muted,
                        fontSize = 15.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                    )
                }
            }
            items(filtered, key = { it.first.id }) { (cat, tools) ->
                val open = q.isNotEmpty() || openCats[cat.id] == true
                CategoryCard(
                    cat = cat,
                    tools = tools,
                    open = open,
                    onToggle = {
                        openCats[cat.id] = !open
                        if (q.isNotEmpty()) query = ""
                    },
                    openItems = openItems,
                    onExample = { example = it },
                    onTemplate = { formTool = it },
                )
            }
        }
    }

    example?.let { tool ->
        ExampleDialog(tool = tool, onClose = { example = null })
    }
}

@Composable
private fun Header(
    total: Int,
    query: String,
    onQueryChange: (String) -> Unit,
    allOpen: Boolean,
    onToggleAll: () -> Unit,
) {
    Surface(color = Color.White) {
        Column {
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("QM-Werkzeuge", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Ink)
                        Text(
                            "$total Werkzeuge in ${categories.size} Kategorien",
                            fontSize = 14.sp,
                            color = Muted,
                        )
                    }
                    OutlinedButton(
                        onClick = onToggleAll,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFD5DAE1)),
                    ) {
                        Text(if (allOpen) "Alle zu" else "Alle auf", color = Brand, fontWeight = FontWeight.SemiBold)
                    }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    placeholder = { Text("Werkzeug suchen, z. B. FMEA") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Muted) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Suche leeren", tint = Muted)
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Bg,
                        focusedContainerColor = Bg,
                        unfocusedBorderColor = Line,
                        focusedBorderColor = Brand,
                    ),
                )
            }
            HorizontalDivider(color = Line)
        }
    }
}

@Composable
private fun CategoryCard(
    cat: Category,
    tools: List<Tool>,
    open: Boolean,
    onToggle: () -> Unit,
    openItems: SnapshotStateMap<String, Boolean>,
    onExample: (Tool) -> Unit,
    onTemplate: (Tool) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Line),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp)
                    .clickable(onClick = onToggle)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(BrandSoft, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(cat.code, color = Brand, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(cat.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Ink, lineHeight = 21.sp)
                    Spacer(Modifier.height(3.dp))
                    Text("${cat.sub} · ${cat.tools.size} Werkzeuge", fontSize = 13.sp, color = Muted, lineHeight = 18.sp)
                }
                Icon(
                    imageVector = if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (open) "Zuklappen" else "Aufklappen",
                    tint = if (open) Brand else Muted,
                )
            }
            AnimatedVisibility(visible = open) {
                Column {
                    HorizontalDivider(color = LineSoft)
                    tools.forEach { tool ->
                        val key = "${cat.id}-${tool.name}"
                        ToolRow(
                            tool = tool,
                            open = openItems[key] == true,
                            onToggle = { openItems[key] = openItems[key] != true },
                            onExample = { onExample(tool) },
                            onTemplate = { onTemplate(tool) },
                        )
                        HorizontalDivider(color = LineSoft)
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolRow(
    tool: Tool,
    open: Boolean,
    onToggle: () -> Unit,
    onExample: () -> Unit,
    onTemplate: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clickable(onClick = onToggle)
                .padding(start = 20.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(tool.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ink, lineHeight = 20.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = tool.kind,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ChipText,
                    modifier = Modifier
                        .background(ChipBg, RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                )
            }
            Icon(
                imageVector = if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (open) "Zuklappen" else "Aufklappen",
                tint = if (open) Brand else Muted,
            )
        }
        AnimatedVisibility(visible = open) {
            Column(
                modifier = Modifier
                    .padding(start = 20.dp, end = 16.dp, bottom = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(PanelBg)
                    .border(1.dp, PanelLine, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(tool.desc, fontSize = 15.sp, lineHeight = 23.sp, color = Ink)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "WANN EINSETZEN?",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Muted,
                        letterSpacing = 0.5.sp,
                    )
                    Text(tool.whenToUse, fontSize = 15.sp, lineHeight = 23.sp, color = Ink)
                }
                Button(
                    onClick = onExample,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BrandLine),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandSoft, contentColor = Brand),
                ) {
                    Text("Beispiel ansehen", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onTemplate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Vorlage ausfüllen", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ExampleDialog(tool: Tool, onClose: () -> Unit) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "BEISPIEL",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Muted,
                            letterSpacing = 0.5.sp,
                        )
                        Text(tool.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink, lineHeight = 25.sp)
                    }
                    IconButton(
                        onClick = onClose,
                        colors = IconButtonDefaults.iconButtonColors(containerColor = ChipBg),
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Beispiel schließen", tint = Ink)
                    }
                }
                ZoomableImage(tool)
                Text(
                    "Mit zwei Fingern zoomen, doppelt tippen zum Zurücksetzen. " +
                        "Die Zahlen im Beispiel sind erfunden und dienen nur zur Veranschaulichung.",
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = Muted,
                )
                Button(
                    onClick = onClose,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Schließen", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ZoomableImage(tool: Tool) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, Line, RoundedCornerShape(14.dp))
            .background(Color.White),
    ) {
        Image(
            painter = painterResource(tool.image),
            contentDescription = "Beispielbild: ${tool.name}",
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = {
                        scale = 1f
                        offset = Offset.Zero
                    })
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 4f)
                        offset = if (scale == 1f) Offset.Zero else offset + pan
                    }
                }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
        )
    }
}
