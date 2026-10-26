package com.cameramanai.studio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions

private val Ink = Color(0xFF0B0B12)
private val Panel = Color(0xFF171722)
private val Violet = Color(0xFF9B7BFF)
private val Cyan = Color(0xFF62E6E0)
private val Muted = Color(0xFF9B9BAA)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(11, 11, 18)
        window.navigationBarColor = android.graphics.Color.rgb(11, 11, 18)
        setContent { CameramanApp() }
    }
}

private enum class StudioTab(val label: String) {
    Home("Studio"), Plan("Shot list"), Director("Director"), Camera("Camera"), Edit("Edit"), Settings("Settings")
}

@Composable
private fun CameramanApp() {
    var tab by remember { mutableStateOf(StudioTab.Home) }
    var projectTitle by remember { mutableStateOf("My first music video") }
    var style by remember { mutableStateOf("Cinematic") }
    var duration by remember { mutableIntStateOf(30) }
    var shots by remember { mutableStateOf(makeShots(style, duration)) }
    var completed by remember { mutableStateOf(setOf<Int>()) }
    var notice by remember { mutableStateOf("") }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Violet, secondary = Cyan, background = Ink, surface = Panel,
            onPrimary = Ink, onBackground = Color.White, onSurface = Color.White
        )
    ) {
        Scaffold(
            containerColor = Ink,
            bottomBar = {
                NavigationBar(containerColor = Color(0xFF111119), contentColor = Violet) {
                    StudioTab.entries.forEach { item ->
                        NavigationBarItem(
                            selected = tab == item,
                            onClick = { tab = item },
                            icon = {
                                Icon(
                                    when (item) {
                                        StudioTab.Home -> Icons.Default.Dashboard
                                        StudioTab.Plan -> Icons.Default.Movie
                                        StudioTab.Director -> Icons.Default.AutoAwesome
                                        StudioTab.Camera -> Icons.Default.Videocam
                                        StudioTab.Edit -> Icons.Default.Tune
                                        StudioTab.Settings -> Icons.Default.Settings
                                    }, contentDescription = item.label
                                )
                            },
                            label = { Text(item.label, fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Violet, selectedTextColor = Violet,
                                indicatorColor = Violet.copy(alpha = .16f),
                                unselectedIconColor = Muted, unselectedTextColor = Muted
                            )
                        )
                    }
                }
            }
        ) { padding ->
            AnimatedContent(targetState = tab, label = "studio-tabs") { selected ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    when (selected) {
                        StudioTab.Home -> HomeScreen(
                            title = projectTitle,
                            completedCount = completed.size,
                            totalCount = shots.size,
                            onStart = { tab = StudioTab.Plan },
                            onCamera = { tab = StudioTab.Camera },
                            onEdit = { tab = StudioTab.Edit },
                            onDirector = { tab = StudioTab.Director }
                        )
                        StudioTab.Plan -> PlanScreen(
                            title = projectTitle,
                            onTitleChange = { projectTitle = it },
                            style = style,
                            onStyleChange = { style = it; shots = makeShots(it, duration); completed = emptySet() },
                            duration = duration,
                            onDurationChange = { duration = it; shots = makeShots(style, it); completed = emptySet() },
                            shots = shots,
                            completed = completed,
                            onToggleShot = { id -> completed = if (id in completed) completed - id else completed + id },
                            onMoveShot = { from, to ->
                                if (from in shots.indices && to in shots.indices) {
                                    val reordered = shots.toMutableList()
                                    val moved = reordered.removeAt(from)
                                    reordered.add(to, moved)
                                    shots = reordered
                                }
                            },
                            onShoot = { tab = StudioTab.Camera }
                        )
                        StudioTab.Director -> DirectorScreen(
                            style = style,
                            onStyleChange = { style = it; shots = makeShots(it, duration); completed = emptySet() },
                            onOpenCamera = { tab = StudioTab.Camera },
                            onOpenPlan = { tab = StudioTab.Plan }
                        )
                        StudioTab.Camera -> CameraScreen(
                            onBack = { tab = StudioTab.Plan },
                            onSaved = { path -> notice = "Saved video: ${File(path).name}" }
                        )
                        StudioTab.Edit -> EditScreen(
                            title = projectTitle,
                            completedCount = completed.size,
                            totalCount = shots.size,
                            notice = notice,
                            onCamera = { tab = StudioTab.Camera },
                            onPlan = { tab = StudioTab.Plan }
                        )
                        StudioTab.Settings -> SettingsScreen()
                    }
                }
            }
        }
    }
}

@Composable
private fun StudioHeader(eyebrow: String, title: String, subtitle: String? = null) {
    Column(Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
        Text(eyebrow.uppercase(), color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Text(title, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-.6).sp)
        if (subtitle != null) Text(subtitle, color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun HomeScreen(title: String, completedCount: Int, totalCount: Int, onStart: () -> Unit, onCamera: () -> Unit, onEdit: () -> Unit, onDirector: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(listOf(Violet, Color(0xFF5142B7)))), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.MovieCreation, null, tint = Color.White, modifier = Modifier.size(26.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text("CAMERAMAN AI", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, letterSpacing = 1.sp)
                    Text("CREATOR STUDIO · ANDROID", color = Muted, fontSize = 9.sp, letterSpacing = 1.5.sp)
                }
                Icon(Icons.Default.AutoAwesome, null, tint = Cyan)
            }
        }
        item {
            Spacer(Modifier.height(4.dp))
            Text("Your creative co-pilot", color = Violet, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("Imagine it.\nShoot it. Edit it.", color = Color.White, fontSize = 35.sp, lineHeight = 39.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp)
        }
        item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Brush.linearGradient(listOf(Color(0xFF30205A), Color(0xFF181827), Color(0xFF10252B)))).padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = Color.White.copy(alpha = .1f), shape = CircleShape) {
                        Text("  AI VIDEO WORKFLOW  ", Modifier.padding(horizontal = 7.dp, vertical = 6.dp), color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.AutoAwesome, null, tint = Violet)
                }
                Spacer(Modifier.height(16.dp))
                Text("From first idea to final cut.", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Plan each shot, follow clear camera cues, record real footage, and prepare your sequence.", color = Color(0xFFD2D0DF), fontSize = 12.sp, lineHeight = 19.sp)
                Spacer(Modifier.height(18.dp))
                Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Ink)) {
                    Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Create a video project", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf("01  PLAN", "02  SHOOT", "03  EDIT").forEach { Text(it, color = Color(0xFFD1C8FF), fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("SHOTS CAPTURED", "$completedCount / $totalCount", Modifier.weight(1f))
                MetricCard("PROJECT", "In progress", Modifier.weight(1f))
            }
        }
        item { Text("Your workflow", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        item { ToolCard(Icons.Default.Movie, "AI shot planner", "Build a sequence with angles and movements", "PLAN", onStart) }
        item { ToolCard(Icons.Default.Videocam, "Camera director", "Open the live camera and record clips", "SHOOT", onCamera) }
        item { ToolCard(Icons.Default.AutoAwesome, "Live shot director", "Get framing, angle and movement coaching", "DIRECT", onDirector) }
        item { ToolCard(Icons.Default.Tune, "Editing workspace", "Review your project progress", "EDIT", onEdit) }
        item { Text("Current project", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Plan → Record → Review", color = Muted, fontSize = 11.sp)
                }
            }
        }
        item { Text("Suggestions guide your creativity. Check every clip before publishing.", color = Muted, fontSize = 10.sp, lineHeight = 15.sp) }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(Panel).padding(14.dp)) {
        Text(label, color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(7.dp))
        Text(value, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp)
    }
}

@Composable
private fun ToolCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, detail: String, tag: String, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(Violet.copy(alpha = .14f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Violet)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(detail, color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
            }
            Text(tag, color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

private data class Shot(val id: Int, val title: String, val angle: String, val movement: String, val duration: Int, val cue: String)

private fun makeShots(style: String, duration: Int): List<Shot> {
    val base = listOf(
        Shot(1, "Opening hook", "Wide establishing", "Slow push-in", 3, "Start still, then move forward smoothly."),
        Shot(2, "Hero introduction", "Low-angle medium", "Gentle orbit", 4, "Keep the subject centered; move in a small arc."),
        Shot(3, "Detail cutaway", "Tight close-up", "Slow tilt", 3, "Focus on hands, accessories, or a key detail."),
        Shot(4, "Energy build", "Medium tracking", "Side tracking", 5, "Walk parallel to the subject at a steady pace."),
        Shot(5, "Signature moment", "Close-up portrait", "Subtle handheld", 4, "Hold eye contact; capture a confident expression."),
        Shot(6, "Final hero frame", "Wide to medium", "Pull-back reveal", 5, "Finish with a clean pose and hold for two seconds.")
    )
    return base.mapIndexed { i, s ->
        val angle = when (style) {
            "Dance / performance" -> listOf("Full-body wide", "Low-angle medium", "Footwork close-up", "Side tracking", "Face close-up", "Hero wide")[i]
            "Reels / short-form" -> listOf("Hook close-up", "Eye-level medium", "Detail macro", "POV tracking", "Reaction close-up", "Final punch-in")[i]
            "Cinematic story" -> listOf("Establishing wide", "Low-angle portrait", "Insert close-up", "Lateral tracking", "Emotional close-up", "Wide reveal")[i]
            else -> s.angle
        }
        s.copy(angle = angle, duration = maxOf(2, (s.duration * duration / 30.0).toInt()), movement = if (i == 0 && style == "Reels / short-form") "Fast punch-in" else s.movement)
    }
}

@Composable
private fun PlanScreen(
    title: String, onTitleChange: (String) -> Unit, style: String, onStyleChange: (String) -> Unit,
    duration: Int, onDurationChange: (Int) -> Unit, shots: List<Shot>, completed: Set<Int>,
    onToggleShot: (Int) -> Unit, onMoveShot: (Int, Int) -> Unit, onShoot: () -> Unit
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { StudioHeader("Module 07 · Production", "AI shot sequencer", "Arrange your opening, hero shots, detail cutaways and ending into a shootable sequence.") }
        item {
            OutlinedTextField(value = title, onValueChange = onTitleChange, modifier = Modifier.fillMaxWidth(), label = { Text("Project name") }, singleLine = true, shape = RoundedCornerShape(15.dp))
        }
        item {
            Text("Shooting style", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Cinematic", "Dance / performance", "Reels / short-form", "Cinematic story").forEach { item ->
                    FilterChip(selected = style == item, onClick = { onStyleChange(item) }, label = { Text(item, fontSize = 11.sp) })
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Target length", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("$duration seconds", color = Cyan, fontWeight = FontWeight.Bold)
            }
            Slider(value = duration.toFloat(), onValueChange = { onDurationChange(it.toInt().coerceAtLeast(15)) }, valueRange = 15f..90f, steps = 4)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF201A37)), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timeline, null, tint = Cyan)
                        Spacer(Modifier.width(8.dp))
                        Text("MODULE 7 · SHOT SEQUENCE", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.weight(1f))
                        Text("${completed.size}/${shots.size}", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    LinearProgressIndicator(
                        progress = { if (shots.isEmpty()) 0f else completed.size.toFloat() / shots.size },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                        color = Cyan,
                        trackColor = Color.White.copy(alpha = .12f)
                    )
                    Text("Estimated footage: ${shots.sumOf { it.duration }} seconds · drag-free reorder with ↑ / ↓ controls", color = Muted, fontSize = 10.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("OPENING", "BUILD", "HERO", "ENDING").forEach { phase ->
                            Surface(color = Violet.copy(alpha = .14f), shape = RoundedCornerShape(7.dp)) {
                                Text(phase, Modifier.padding(horizontal = 7.dp, vertical = 5.dp), color = Violet, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
        itemsIndexed(shots) { index, shot ->
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(15.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = Violet.copy(alpha = .16f), shape = RoundedCornerShape(10.dp)) {
                            Text("%02d".format(index + 1), Modifier.padding(horizontal = 11.dp, vertical = 8.dp), color = Violet, fontWeight = FontWeight.ExtraBold)
                        }
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text(shot.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("${shot.angle} · ${shot.duration}s", color = Muted, fontSize = 10.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(onClick = { if (index > 0) onMoveShot(index, index - 1) }, enabled = index > 0, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.KeyboardArrowUp, "Move shot earlier", tint = if (index > 0) Cyan else Muted)
                            }
                            IconButton(onClick = { if (index < shots.lastIndex) onMoveShot(index, index + 1) }, enabled = index < shots.lastIndex, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.KeyboardArrowDown, "Move shot later", tint = if (index < shots.lastIndex) Cyan else Muted)
                            }
                        }
                        Checkbox(checked = shot.id in completed, onCheckedChange = { onToggleShot(shot.id) })
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.OpenWith, null, tint = Cyan, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(shot.movement, color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(shot.cue, color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
                }
            }
        }
        item {
            Button(onClick = onShoot, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.Videocam, null); Spacer(Modifier.width(8.dp)); Text("Open camera and shoot")
            }
        }
    }
}

@Composable
private fun DirectorScreen(
    style: String,
    onStyleChange: (String) -> Unit,
    onOpenCamera: () -> Unit,
    onOpenPlan: () -> Unit
) {
    val context = LocalContext.current
    val ttsRef = remember { arrayOfNulls<TextToSpeech>(1) }
    DisposableEffect(Unit) {
        onDispose {
            ttsRef[0]?.stop()
            ttsRef[0]?.shutdown()
            ttsRef[0] = null
        }
    }
    var scene by remember { mutableStateOf("Beautiful scenery") }
    var subject by remember { mutableStateOf("Landscape") }
    var mood by remember { mutableStateOf("Cinematic") }
    var directionIndex by remember { mutableIntStateOf(0) }
    var voiceStatus by remember { mutableStateOf("Choose a scene to get started") }
    val cues = remember(scene, subject, mood) { makeDirectorCues(scene, subject, mood) }
    val cue = cues[directionIndex.coerceIn(0, cues.lastIndex)]

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { StudioHeader("Module 02 · Director", "Your shot coach", "Choose the scene and get practical framing and movement directions before recording.") }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("WHAT ARE YOU FILMING?", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Text("Scene type", color = Color.White, fontWeight = FontWeight.Bold)
                    FlowChips(listOf("Beautiful scenery", "Person / portrait", "Dance / performance", "Food / product", "Travel / walking"), scene) { scene = it; directionIndex = 0 }
                    Text("Main subject", color = Color.White, fontWeight = FontWeight.Bold)
                    FlowChips(listOf("Landscape", "One person", "Group", "Moving subject", "Small detail"), subject) { subject = it; directionIndex = 0 }
                    Text("Mood", color = Color.White, fontWeight = FontWeight.Bold)
                    FlowChips(listOf("Cinematic", "Energetic", "Romantic", "Dramatic"), mood) { mood = it; directionIndex = 0 }
                    Text("Current planner style: $style", color = Muted, fontSize = 10.sp)
                    OutlinedButton(onClick = { onStyleChange(moodToStyle(mood)) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Tune, null); Spacer(Modifier.width(6.dp)); Text("Use mood in shot planner")
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF201A37)), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, null, tint = Cyan)
                        Spacer(Modifier.width(8.dp))
                        Text("DIRECTOR'S NEXT SHOT", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
                        Spacer(Modifier.weight(1f))
                        Text("${directionIndex + 1}/${cues.size}", color = Muted, fontSize = 11.sp)
                    }
                    Text(cue.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    Surface(color = Violet.copy(alpha = .17f), shape = RoundedCornerShape(12.dp)) {
                        Text(cue.angle, Modifier.padding(horizontal = 12.dp, vertical = 9.dp), color = Violet, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Text("CAMERA MOVE", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(cue.movement, color = Cyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(cue.instruction, color = Color(0xFFE0DDEB), fontSize = 13.sp, lineHeight = 20.sp)
                    Text("AVOID: ${cue.avoid}", color = Color(0xFFFFB9C0), fontSize = 11.sp, lineHeight = 16.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            try {
                                ttsRef[0]?.stop()
                                ttsRef[0]?.shutdown()
                                ttsRef[0] = TextToSpeech(context) { result ->
                                    if (result == TextToSpeech.SUCCESS) {
                                        ttsRef[0]?.language = Locale.US
                                        ttsRef[0]?.speak(cue.voiceLine, TextToSpeech.QUEUE_FLUSH, null, "director-cue")
                                        voiceStatus = "Speaking direction"
                                    } else voiceStatus = "Text-to-speech is unavailable"
                                }
                            } catch (_: Exception) { voiceStatus = "Text-to-speech is unavailable" }
                        }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.VolumeUp, null); Spacer(Modifier.width(5.dp)); Text("Speak cue")
                        }
                        Button(onClick = { directionIndex = (directionIndex + 1) % cues.size }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.SkipNext, null); Spacer(Modifier.width(5.dp)); Text("Next shot")
                        }
                    }
                    Text(voiceStatus, color = Muted, fontSize = 10.sp)
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Before you press record", color = Color.White, fontWeight = FontWeight.Bold)
                    listOf("Clean the lens and check the background.", "Tap your subject on the preview to focus.", "Keep the horizon level and move slowly.", "Record 3 seconds before and after the main action.").forEach {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.CheckCircle, null, tint = Cyan, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(it, color = Muted, fontSize = 12.sp, lineHeight = 17.sp)
                        }
                    }
                }
            }
        }
        item {
            Button(onClick = onOpenCamera, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.Videocam, null); Spacer(Modifier.width(8.dp)); Text("Open camera for this shot")
            }
        }
        item {
            OutlinedButton(onClick = onOpenPlan, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.Movie, null); Spacer(Modifier.width(8.dp)); Text("View full shot list")
            }
        }
        item { Text("This first version uses scene-based coaching rules on the phone. Live scene recognition and Gemini-powered custom direction are not connected yet.", color = Muted, fontSize = 10.sp, lineHeight = 16.sp) }
    }
}

private data class DirectorCue(
    val title: String, val angle: String, val movement: String,
    val instruction: String, val avoid: String, val voiceLine: String
)

private fun moodToStyle(mood: String): String = when (mood) {
    "Energetic" -> "Dance / performance"
    "Dramatic" -> "Cinematic story"
    else -> "Cinematic"
}

private fun makeDirectorCues(scene: String, subject: String, mood: String): List<DirectorCue> {
    val landscape = scene == "Beautiful scenery" || subject == "Landscape"
    val portrait = scene == "Person / portrait" || subject == "One person"
    val performance = scene == "Dance / performance" || mood == "Energetic"
    val product = scene == "Food / product" || subject == "Small detail"
    return when {
        landscape -> listOf(
            DirectorCue("Reveal the location", "Wide establishing · eye level", "Slow push forward", "Hold the phone steady at chest height. Keep the horizon straight and leave space around the main view. Walk forward slowly for 4–5 seconds.", "Fast pans and a tilted horizon.", "Start wide. Keep the horizon level. Walk forward slowly."),
            DirectorCue("Show foreground depth", "Wide · foreground framing", "Slide gently sideways", "Place a nearby leaf, rock, railing or branch near one edge. Move sideways slightly to reveal the scenery behind it.", "Blocking the main view with the foreground.", "Use a foreground object. Slide sideways gently."),
            DirectorCue("Capture a detail", "Close-up · texture or nature", "Slow tilt or static hold", "Film moving leaves, water, flowers, clouds or a textured surface. Hold each detail for at least 3 seconds.", "Digital zoom and sudden movement.", "Find a small detail. Hold steady for three seconds."),
            DirectorCue("Finish with scale", "Wide scenic frame", "Slow pull-back", "Include a person or clear object for scale if available. Pull back slowly, then hold the final composition for two seconds.", "Cutting off the skyline or subject.", "Finish wide. Pull back slowly, then hold the frame.")
        )
        portrait -> listOf(
            DirectorCue("Introduce the person", "Medium shot · eye level", "Slow push-in", "Keep the eyes near the upper third. Ask the person to relax their shoulders and look just past the lens unless direct eye contact suits the scene.", "Camera below the chin or busy background.", "Frame from the waist up. Keep the eyes near the upper third."),
            DirectorCue("Get a natural portrait", "Three-quarter angle", "Small side arc", "Move only a small arc around the person. Keep the face in soft light and focus on the nearer eye.", "A large, fast orbit or harsh backlight.", "Move in a small arc. Keep focus on the nearer eye."),
            DirectorCue("Capture expression", "Close-up portrait", "Mostly still", "Ask for one clear emotion, then record a few seconds before and after the expression. Keep the camera still.", "Over-directing facial expressions.", "Hold still. Capture the expression before and after the moment."),
            DirectorCue("Hero ending", "Medium-wide · slight low angle", "Slow pull-back", "Ask the person to hold a confident natural pose. Keep their head and shoulders inside the frame.", "Cutting off the head or moving too quickly.", "Hold a confident pose. Pull back slowly.")
        )
        performance -> listOf(
            DirectorCue("Start with the hook", "Full-body wide", "Small controlled push-in", "Keep the dancer's feet and hands inside the frame. Leave room in the direction they move and start recording before the beat.", "Cropping feet or digital zoom.", "Frame the full body. Leave space for the movement."),
            DirectorCue("Track the movement", "Full-body side angle", "Walk parallel smoothly", "Move parallel to the performer at a safe, steady pace. Keep distance consistent and let the action happen inside the frame.", "Walking backwards without checking your path.", "Track from the side. Keep your pace smooth and safe."),
            DirectorCue("Add a detail cut", "Hands or footwork close-up", "Short static take", "Record one clean close-up of footwork, hands or an accessory, then stop for a cutaway.", "Shaking the phone while trying to zoom.", "Capture one detail close-up. Keep the phone still."),
            DirectorCue("End on the signature move", "Medium-wide hero frame", "Gentle push-in then hold", "Start a little wider, move in gently during the signature moment, and hold the ending pose for two seconds.", "Stopping recording exactly on the action.", "Move in gently, then hold the final pose.")
        )
        product -> listOf(
            DirectorCue("Show the whole subject", "Three-quarter product shot", "Slow push-in", "Use a clean background and soft side light. Keep the entire item visible and tap the subject to focus.", "Clutter, reflections and digital zoom.", "Show the whole subject. Use soft light and tap to focus."),
            DirectorCue("Reveal texture", "Tight close-up", "Slow lateral slide", "Move the phone a short distance sideways to reveal texture. Keep the subject in focus and avoid casting your shadow.", "Getting so close that focus becomes blurry.", "Move sideways slowly to reveal texture."),
            DirectorCue("Show scale", "Medium shot with context", "Gentle tilt", "Include a hand or familiar object for scale when useful. Tilt slowly from the context to the key detail.", "Fast tilts and changing exposure.", "Show the context, then tilt slowly to the detail."),
            DirectorCue("Clean final frame", "Centered hero composition", "Static hold", "Finish with a clean, uncluttered view and keep it still for at least three seconds.", "Moving away before the take finishes.", "Hold a clean final frame for three seconds.")
        )
        else -> listOf(
            DirectorCue("Establish the action", "Wide · eye level", "Slow follow", "Show the location and where the subject is going. Start recording before movement begins.", "Starting too late or cutting off the subject.", "Start wide and show where the subject is going."),
            DirectorCue("Follow the subject", "Medium tracking shot", "Smooth side tracking", "Keep a safe path, move at a steady pace, and keep the subject in the same area of the frame.", "Walking backwards into obstacles.", "Follow smoothly. Keep a safe path."),
            DirectorCue("Capture a reaction", "Close-up", "Static hold", "Stop moving and capture the subject's reaction or a meaningful detail for 3 seconds.", "Unnecessary zoom or camera shake.", "Stop moving and hold the close-up."),
            DirectorCue("End with a reveal", "Wide finishing frame", "Slow pull-back", "Move back slowly if the path is clear, then hold the final frame for two seconds.", "Rushing the final movement.", "Pull back slowly and hold the ending.")
        )
    }
}

@Composable
private fun FlowChips(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { option ->
                    FilterChip(selected = selected == option, onClick = { onSelect(option) }, label = { Text(option, fontSize = 10.sp) }, modifier = Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CameraScreen(onBack: () -> Unit, onSaved: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { ContextCompat.getMainExecutor(context) }
    var hasPermissions by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var hasAudioPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    var recording by remember { mutableStateOf(false) }
    var showGrid by remember { mutableStateOf(true) }
    var status by remember { mutableStateOf("Ready to shoot") }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var activeRecording by remember { mutableStateOf<androidx.camera.video.Recording?>(null) }
    var boundCamera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    var lens by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var liveScene by remember { mutableStateOf("Waiting for camera frames…") }
    var liveObject by remember { mutableStateOf("No subject detected yet") }
    var liveConfidence by remember { mutableStateOf(0f) }
    var boxLeft by remember { mutableFloatStateOf(0.18f) }
    var boxTop by remember { mutableFloatStateOf(0.18f) }
    var boxRight by remember { mutableFloatStateOf(0.82f) }
    var boxBottom by remember { mutableFloatStateOf(0.82f) }
    var hasTrackedBox by remember { mutableStateOf(false) }
    var compositionMode by remember { mutableStateOf("Rule of thirds") }
    var movement by remember { mutableStateOf("Push in") }
    var steadiness by remember { mutableFloatStateOf(0f) }
    var movementStatus by remember { mutableStateOf("Hold phone still to calibrate") }
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    var analyzerEnabled by remember { mutableStateOf(true) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val lastAnalysisAt = remember { longArrayOf(0L) }
    val imageLabeler = remember { ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS) }
    val objectDetector = remember {
        ObjectDetection.getClient(
            ObjectDetectorOptions.Builder()
                .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                .enableClassification()
                .build()
        )
    }
    DisposableEffect(sensorManager) {
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            private var smooth = 0f
            override fun onSensorChanged(event: SensorEvent) {
                val raw = kotlin.math.sqrt(event.values.take(3).sumOf { it.toDouble() * it.toDouble() }).toFloat()
                smooth = if (smooth == 0f) raw else smooth * 0.82f + raw * 0.18f
                steadiness = smooth
                movementStatus = when {
                    smooth < 0.35f -> "STEADY · ready for a smooth take"
                    smooth < 1.2f -> "MOVING · keep the motion gentle"
                    else -> "SHAKY · pause and stabilize"
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (sensor != null) sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sensorManager.unregisterListener(listener) }
    }
    DisposableEffect(Unit) {
        onDispose {
            analysisExecutor.shutdown()
            imageLabeler.close()
            objectDetector.close()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        hasPermissions = grants[Manifest.permission.CAMERA] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        hasAudioPermission = grants[Manifest.permission.RECORD_AUDIO] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        status = if (hasPermissions) "Camera permission granted · starting preview" else "Camera permission denied. Tap Allow camera to try again."
    }

    DisposableEffect(hasPermissions, lens, previewView) {
        if (!hasPermissions || previewView == null) return@DisposableEffect onDispose { }
        val view = previewView ?: return@DisposableEffect onDispose { }
        val providerFuture = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            try {
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also { it.surfaceProvider = view.surfaceProvider }
                val recorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(Quality.HD))
                    .build()
                val capture = VideoCapture.withOutput(recorder)
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(analysisExecutor) { proxy ->
                    val now = System.currentTimeMillis()
                    if (!analyzerEnabled || now - lastAnalysisAt[0] < 650L) {
                        proxy.close()
                    } else {
                        lastAnalysisAt[0] = now
                        val mediaImage = proxy.image
                        if (mediaImage == null) {
                            proxy.close()
                        } else {
                            val input = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
                            imageLabeler.process(input)
                                .addOnSuccessListener { labels ->
                                    val best = labels.maxByOrNull { it.confidence }
                                    if (best != null && best.confidence >= 0.45f) {
                                        val name = best.text
                                        val confidence = (best.confidence * 100).toInt()
                                        executor.execute {
                                            liveScene = "Scene cue: $name ($confidence%)"
                                        }
                                    }
                                    objectDetector.process(input)
                                        .addOnSuccessListener { objects ->
                                            val target = objects.maxByOrNull { obj ->
                                                val area = obj.boundingBox.width().toLong() * obj.boundingBox.height().toLong()
                                                area * (obj.labels.maxOfOrNull { it.confidence }?.toDouble() ?: 0.5)
                                            }
                                            if (target != null) {
                                                val label = target.labels.firstOrNull()?.text ?: "Subject"
                                                val confidence = target.labels.firstOrNull()?.confidence ?: 0f
                                                val bounds = target.boundingBox
                                                val imageWidth = proxy.width.toFloat().coerceAtLeast(1f)
                                                val imageHeight = proxy.height.toFloat().coerceAtLeast(1f)
                                                var left = (bounds.left / imageWidth).coerceIn(0f, 1f)
                                                var top = (bounds.top / imageHeight).coerceIn(0f, 1f)
                                                var right = (bounds.right / imageWidth).coerceIn(0f, 1f)
                                                var bottom = (bounds.bottom / imageHeight).coerceIn(0f, 1f)
                                                when (proxy.imageInfo.rotationDegrees) {
                                                    90 -> { val l = left; val t = top; val r = right; val b = bottom; left = 1f-b; top = l; right = 1f-t; bottom = r }
                                                    180 -> { val l = left; val t = top; val r = right; val b = bottom; left = 1f-r; top = 1f-b; right = 1f-l; bottom = 1f-t }
                                                    270 -> { val l = left; val t = top; val r = right; val b = bottom; left = t; top = 1f-r; right = b; bottom = 1f-l }
                                                }
                                                executor.execute {
                                                    liveObject = "Detected subject: $label"
                                                    liveConfidence = confidence
                                                    boxLeft = left
                                                    boxTop = top
                                                    boxRight = right
                                                    boxBottom = bottom
                                                    hasTrackedBox = true
                                                }
                                            } else {
                                                executor.execute {
                                                    liveObject = "Searching for a clear subject…"
                                                    liveConfidence = 0f
                                                    hasTrackedBox = false
                                                }
                                            }
                                        }
                                        .addOnFailureListener {
                                            executor.execute { liveObject = "Object tracking unavailable for this frame" }
                                        }
                                        .addOnCompleteListener { proxy.close() }
                                }
                                .addOnFailureListener {
                                    executor.execute { liveScene = "Scene analysis unavailable for this frame" }
                                    objectDetector.process(input)
                                        .addOnCompleteListener { proxy.close() }
                                }
                        }
                    }
                }
                provider.unbindAll()
                boundCamera = provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.Builder().requireLensFacing(lens).build(),
                    preview,
                    capture,
                    analysis
                )
                videoCapture = capture
                status = "Live camera ready · HD"
            } catch (e: Exception) {
                videoCapture = null
                boundCamera = null
                status = "Camera unavailable: ${e.localizedMessage ?: "check camera access"}"
            }
        }
        providerFuture.addListener(listener, executor)
        onDispose {
            try {
                if (providerFuture.isDone) providerFuture.get().unbindAll()
            } catch (_: Exception) { }
            videoCapture = null
            boundCamera = null
        }
    }

    Column(Modifier.fillMaxSize().background(Ink).padding(horizontal = 14.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, enabled = !recording) { Icon(Icons.Default.ArrowBack, "Back", tint = Color.White) }
            Column(Modifier.weight(1f)) {
                Text("DIRECTOR CAMERA", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text(status, color = Muted, fontSize = 11.sp, maxLines = 2)
            }
            IconButton(
                enabled = !recording,
                onClick = { lens = if (lens == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK }
            ) { Icon(Icons.Default.Cameraswitch, "Switch front/back camera", tint = Color.White) }
        }

        Box(
            Modifier.weight(1f).fillMaxWidth().padding(vertical = 10.dp)
                .clip(RoundedCornerShape(24.dp)).background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (hasPermissions) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).also {
                            it.scaleType = PreviewView.ScaleType.FILL_CENTER
                            it.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            previewView = it
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                // Module 6: composition overlay. The guide is an approximate viewfinder guide;
                // detector coordinates can differ slightly because PreviewView crops to fill.
                Canvas(Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val stroke = Stroke(width = 1.dp.toPx())
                    when (compositionMode) {
                        "Rule of thirds" -> {
                            drawLine(Color.White.copy(alpha = .65f), androidx.compose.ui.geometry.Offset(w / 3f, 0f), androidx.compose.ui.geometry.Offset(w / 3f, h), stroke.width)
                            drawLine(Color.White.copy(alpha = .65f), androidx.compose.ui.geometry.Offset(2f * w / 3f, 0f), androidx.compose.ui.geometry.Offset(2f * w / 3f, h), stroke.width)
                            drawLine(Color.White.copy(alpha = .65f), androidx.compose.ui.geometry.Offset(0f, h / 3f), androidx.compose.ui.geometry.Offset(w, h / 3f), stroke.width)
                            drawLine(Color.White.copy(alpha = .65f), androidx.compose.ui.geometry.Offset(0f, 2f * h / 3f), androidx.compose.ui.geometry.Offset(w, 2f * h / 3f), stroke.width)
                        }
                        "Center" -> {
                            drawLine(Color.White.copy(alpha = .55f), androidx.compose.ui.geometry.Offset(w / 2f, 0f), androidx.compose.ui.geometry.Offset(w / 2f, h), stroke.width)
                            drawLine(Color.White.copy(alpha = .55f), androidx.compose.ui.geometry.Offset(0f, h / 2f), androidx.compose.ui.geometry.Offset(w, h / 2f), stroke.width)
                        }
                        "Golden-style" -> {
                            val x = w * 0.382f
                            val y = h * 0.382f
                            drawLine(Color.White.copy(alpha = .55f), androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, h), stroke.width)
                            drawLine(Color.White.copy(alpha = .55f), androidx.compose.ui.geometry.Offset(w - x, 0f), androidx.compose.ui.geometry.Offset(w - x, h), stroke.width)
                            drawLine(Color.White.copy(alpha = .55f), androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(w, y), stroke.width)
                            drawLine(Color.White.copy(alpha = .55f), androidx.compose.ui.geometry.Offset(0f, h - y), androidx.compose.ui.geometry.Offset(w, h - y), stroke.width)
                        }
                    }
                }
                if (hasTrackedBox) {
                    Canvas(Modifier.fillMaxSize()) {
                        val left = boxLeft * size.width
                        val top = boxTop * size.height
                        val right = boxRight * size.width
                        val bottom = boxBottom * size.height
                        drawRect(
                            color = Cyan,
                            topLeft = androidx.compose.ui.geometry.Offset(left, top),
                            size = androidx.compose.ui.geometry.Size((right - left).coerceAtLeast(1f), (bottom - top).coerceAtLeast(1f)),
                            style = Stroke(width = 3.dp.toPx())
                        )
                        val c = 18.dp.toPx()
                        val w = 5.dp.toPx()
                        drawLine(Cyan, androidx.compose.ui.geometry.Offset(left, top), androidx.compose.ui.geometry.Offset(left + c, top), w)
                        drawLine(Cyan, androidx.compose.ui.geometry.Offset(left, top), androidx.compose.ui.geometry.Offset(left, top + c), w)
                        drawLine(Cyan, androidx.compose.ui.geometry.Offset(right, top), androidx.compose.ui.geometry.Offset(right - c, top), w)
                        drawLine(Cyan, androidx.compose.ui.geometry.Offset(right, top), androidx.compose.ui.geometry.Offset(right, top + c), w)
                        drawLine(Cyan, androidx.compose.ui.geometry.Offset(left, bottom), androidx.compose.ui.geometry.Offset(left + c, bottom), w)
                        drawLine(Cyan, androidx.compose.ui.geometry.Offset(left, bottom), androidx.compose.ui.geometry.Offset(left, bottom - c), w)
                        drawLine(Cyan, androidx.compose.ui.geometry.Offset(right, bottom), androidx.compose.ui.geometry.Offset(right - c, bottom), w)
                        drawLine(Cyan, androidx.compose.ui.geometry.Offset(right, bottom), androidx.compose.ui.geometry.Offset(right, bottom - c), w)
                    }
                }
                // Tap anywhere in the preview to focus at that point.
                Box(
                    Modifier.fillMaxSize().pointerInput(boundCamera, previewView) {
                        detectTapGestures { point ->
                            val view = previewView
                            val camera = boundCamera
                            if (view != null && camera != null) {
                                try {
                                    val meteringPoint = view.meteringPointFactory.createPoint(point.x, point.y)
                                    val action = FocusMeteringAction.Builder(meteringPoint)
                                        .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
                                        .build()
                                    camera.cameraControl.startFocusAndMetering(action)
                                    status = "Focusing…"
                                } catch (_: Exception) {
                                    status = "Tap to focus is unavailable on this camera"
                                }
                            }
                        }
                    }
                ) {
                    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Surface(color = Color.Black.copy(alpha = .55f), shape = CircleShape) {
                                Text(if (recording) "● REC" else "● LIVE", Modifier.padding(horizontal = 12.dp, vertical = 7.dp), color = if (recording) Color(0xFFFF5364) else Color(0xFFFF6B78), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Surface(color = Color.Black.copy(alpha = .55f), shape = CircleShape) {
                                Text("HD", Modifier.padding(horizontal = 12.dp, vertical = 7.dp), color = Color.White, fontSize = 10.sp)
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(color = Color.Black.copy(alpha = .72f), shape = RoundedCornerShape(12.dp)) {
                                Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                    Text("LIVE SCENE ANALYSIS", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                                    Text(liveScene, color = Color.White, fontSize = 11.sp, maxLines = 2)
                                    Text(liveObject, color = Violet, fontSize = 11.sp, maxLines = 1)
                                    Text(if (hasTrackedBox) "TRACKING BOX ACTIVE · keep subject in frame" else "TRACKING · searching for subject", color = if (hasTrackedBox) Cyan else Muted, fontSize = 9.sp, maxLines = 1)
                                    if (liveConfidence > 0f) {
                                        Text("Subject confidence: ${(liveConfidence * 100).toInt()}%", color = Muted, fontSize = 9.sp)
                                    }
                                }
                            }
                        }
                        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            if (showGrid) {
                                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly) {
                                    repeat(2) { HorizontalGridLine() }
                                }
                                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    repeat(2) { VerticalGridLine() }
                                }
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Add, null, tint = Color.White.copy(alpha = .72f), modifier = Modifier.size(30.dp))
                                Text("Tap to focus · Keep your subject centered", color = Color.White, fontSize = 11.sp)
                            }
                        }
                        Text("Rule of thirds · Hold steady · Move slowly", color = Color.White, fontSize = 10.sp)
                    }
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Icon(Icons.Default.NoPhotography, null, tint = Violet, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Camera permission needed", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Allow camera access to open the live preview. Microphone is optional for recorded audio.", color = Muted, fontSize = 12.sp)
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = {
                        permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
                    }) { Text("Allow camera") }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Panel),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CenterFocusStrong, null, tint = Violet, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("MODULE 6 · COMPOSITION COACH", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Rule of thirds", "Center", "Golden-style").forEach { mode ->
                        FilterChip(selected = compositionMode == mode, onClick = { compositionMode = mode }, label = { Text(mode, fontSize = 9.sp) })
                    }
                }
                val targetX = if (compositionMode == "Center") 0.5f else 0.333f
                val targetY = 0.333f
                val subjectX = (boxLeft + boxRight) / 2f
                val subjectY = (boxTop + boxBottom) / 2f
                val advice = when {
                    !hasTrackedBox -> "Waiting for a subject: use the grid to compose your shot manually."
                    kotlin.math.abs(subjectX - targetX) < 0.12f && kotlin.math.abs(subjectY - targetY) < 0.15f -> "Good placement. Keep the subject steady and protect space around it."
                    subjectX < 0.28f -> "Subject is near the left edge. Leave breathing room before recording."
                    subjectX > 0.72f -> "Subject is near the right edge. Check headroom and movement space."
                    subjectY < 0.22f -> "Subject is close to the top. Check headroom so it is not cramped."
                    subjectY > 0.78f -> "Subject is low in frame. Reframe or choose a deliberate low composition."
                    else -> "Try placing the subject near a grid intersection for a more intentional frame."
                }
                Text(if (hasTrackedBox) "LIVE COMPOSITION · subject detected" else "COMPOSITION GUIDE · manual framing", color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text(advice, color = Color.White, fontSize = 12.sp, lineHeight = 17.sp)
                Text("Tip: for landscapes, keep the horizon on the upper or lower third. For centered symmetry, choose Center.", color = Muted, fontSize = 10.sp, lineHeight = 14.sp)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Panel),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Navigation, null, tint = Cyan, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("MODULE 5 · SMART MOVEMENT GUIDE", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf("Push in", "Pan left", "Pan right", "Tilt up", "Tilt down", "Hold steady").forEach { option ->
                        FilterChip(selected = movement == option, onClick = { movement = option }, label = { Text(option, fontSize = 9.sp) })
                    }
                }
                val arrow = when (movement) {
                    "Push in" -> "↑  Move forward slowly"
                    "Pan left" -> "←  Rotate left smoothly"
                    "Pan right" -> "→  Rotate right smoothly"
                    "Tilt up" -> "↗  Tilt upward gently"
                    "Tilt down" -> "↘  Tilt downward gently"
                    else -> "◎  Keep phone still"
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = Color(0xFF12354A), shape = RoundedCornerShape(12.dp)) {
                        Text(arrow, Modifier.padding(horizontal = 12.dp, vertical = 9.dp), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(movementStatus, color = if (steadiness < 0.35f) Cyan else if (steadiness < 1.2f) Color(0xFFFFD166) else Color(0xFFFF7B86), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("Motion sensor: ${"%.2f".format(Locale.US, steadiness)} · smaller is steadier", color = Muted, fontSize = 9.sp)
                    }
                }
                Text("Move slowly and keep a clear path. Sensor readings indicate shake, not whether the camera followed the selected direction.", color = Muted, fontSize = 9.sp, lineHeight = 13.sp)
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = { showGrid = !showGrid }) { Icon(Icons.Default.Grid3x3, "Toggle grid", tint = if (showGrid) Cyan else Muted) }
                Text(if (showGrid) "Grid on" else "Grid off", color = Muted, fontSize = 10.sp)
            }
            Box(
                Modifier.size(76.dp).clip(CircleShape)
                    .background(if (recording) Color(0xFFFF5364) else Color.White).padding(5.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    enabled = hasPermissions && videoCapture != null,
                    onClick = {
                        val capture = videoCapture ?: return@IconButton
                        if (recording) {
                            status = "Finishing clip…"
                            activeRecording?.stop()
                        } else {
                            try {
                                val folder = File(context.getExternalFilesDir(null), "CameramanAI")
                                if (!folder.exists() && !folder.mkdirs()) {
                                    status = "Could not create clip storage folder"
                                    return@IconButton
                                }
                                val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                                val file = File(folder, "CAM_$stamp.mp4")
                                val options = FileOutputOptions.Builder(file).build()
                                var pending = capture.output.prepareRecording(context, options)
                                if (hasAudioPermission && ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                    pending = pending.withAudioEnabled()
                                }
                                activeRecording = pending.start(executor) { event ->
                                    when (event) {
                                        is VideoRecordEvent.Start -> {
                                            recording = true
                                            status = if (hasAudioPermission) "Recording video + audio…" else "Recording video (no audio)…"
                                        }
                                        is VideoRecordEvent.Finalize -> {
                                            recording = false
                                            activeRecording = null
                                            if (!event.hasError() && file.exists() && file.length() > 0L) {
                                                status = "Clip saved · ${file.name}"
                                                onSaved(file.absolutePath)
                                            } else {
                                                status = "Recording failed (code ${event.error}). Please try again."
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                recording = false
                                activeRecording = null
                                status = "Could not start recording: ${e.localizedMessage ?: "try again"}"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(if (recording) Icons.Default.Stop else Icons.Default.FiberManualRecord, if (recording) "Stop recording" else "Start recording", tint = if (recording) Color.White else Color(0xFFFF5364), modifier = Modifier.size(34.dp))
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    enabled = !recording,
                    onClick = { lens = if (lens == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK }
                ) { Icon(Icons.Default.FlipCameraAndroid, "Flip lens", tint = Muted) }
                Text("Flip", color = Muted, fontSize = 10.sp)
            }
        }
        Text("Clips are saved in app-private storage on this device.", color = Muted, fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 4.dp))
    }
}

@Composable
private fun HorizontalGridLine() {
    Spacer(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = .3f)))
}

@Composable
private fun VerticalGridLine() {
    Spacer(Modifier.fillMaxHeight().width(1.dp).background(Color.White.copy(alpha = .3f)))
}

@Composable
private fun EditScreen(title: String, completedCount: Int, totalCount: Int, notice: String, onCamera: () -> Unit, onPlan: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        item { StudioHeader("Post-production", "Edit workspace", "Organize your shoot and check what is ready for editing.") }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(progress = { if (totalCount == 0) 0f else completedCount.toFloat() / totalCount }, modifier = Modifier.fillMaxWidth(), color = Cyan, trackColor = Color.White.copy(alpha = .08f))
                    Spacer(Modifier.height(8.dp))
                    Text("$completedCount of $totalCount planned shots marked complete", color = Muted, fontSize = 11.sp)
                }
            }
        }
        item { ToolCard(Icons.Default.Videocam, "Record another clip", "Open the CameraX recording screen", "SHOOT", onCamera) }
        item { ToolCard(Icons.Default.Movie, "Review shot sequence", "Check off shots as you capture them", "PLAN", onPlan) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Editing status", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("Native timeline, transitions, color grading and final export are the next build phase. This screen does not pretend those tools are finished.", color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
                    if (notice.isNotBlank()) { Spacer(Modifier.height(10.dp)); Text(notice, color = Cyan, fontSize = 11.sp) }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen() {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { StudioHeader("Preferences", "Settings", "Built for Android with privacy-conscious local recording.") }
        item { ToolCard(Icons.Default.Security, "Local clip storage", "Camera recordings are stored in this app's private folder.", "ON DEVICE", {}) }
        item { ToolCard(Icons.Default.AutoAwesome, "AI director", "AI shot planning and scene analysis require a valid server-side Gemini key.", "GEMINI", {}) }
        item { ToolCard(Icons.Default.PhoneAndroid, "Platform", "Native Kotlin · Jetpack Compose · CameraX · Android SDK 36", "ANDROID", {}) }
        item { Text("Never place a Gemini API key directly inside the Android app. Keep it on the server.", color = Muted, fontSize = 11.sp, lineHeight = 17.sp) }
    }
}
