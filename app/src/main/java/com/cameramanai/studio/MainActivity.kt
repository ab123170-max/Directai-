package com.cameramanai.studio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.os.SystemClock
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.speech.tts.TextToSpeech
import android.widget.VideoView
import android.widget.MediaController
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

private fun interpretSceneLabels(labels: List<String>): Pair<String, String> {
    val words = labels.joinToString(" ").lowercase(Locale.US)
    return when {
        listOf("sunset", "sunrise", "sky", "cloud", "dusk", "dawn").any { it in words } ->
            "SKY / GOLDEN-HOUR SCENE" to "Expose for the bright sky, keep the horizon level, and try a wide establishing shot or a silhouette."
        listOf("beach", "ocean", "sea", "waterfall", "lake", "river", "water").any { it in words } ->
            "WATER / COASTAL SCENE" to "Keep the horizon straight, protect bright reflections, and capture a wide shot plus a close detail of moving water."
        listOf("mountain", "forest", "tree", "plant", "flower", "grass", "landscape", "nature").any { it in words } ->
            "NATURE / LANDSCAPE" to "Start with a wide establishing frame, include foreground depth, then capture a slow pan only if your footing is safe."
        listOf("person", "people", "face", "human", "portrait", "clothing").any { it in words } ->
            "PEOPLE / PORTRAIT" to "Keep the face well lit, leave space in the direction of the gaze, and capture a medium shot followed by a close-up."
        listOf("food", "dish", "meal", "fruit", "vegetable", "cuisine").any { it in words } ->
            "FOOD / PRODUCT DETAIL" to "Use soft side light, simplify the background, and capture one overhead frame plus one close texture detail."
        listOf("building", "architecture", "house", "tower", "bridge", "city", "skyscraper").any { it in words } ->
            "ARCHITECTURE / CITY" to "Keep vertical lines straight, avoid cutting off key edges, and capture a balanced wide frame before detail shots."
        listOf("car", "vehicle", "bicycle", "motorcycle", "train", "airplane").any { it in words } ->
            "VEHICLE / MOTION" to "Show the direction of travel with extra space ahead, keep a safe distance, and try a steady wide shot before close details."
        listOf("dog", "cat", "bird", "animal", "horse", "pet").any { it in words } ->
            "ANIMAL / PET" to "Move slowly, focus on the eyes when possible, and leave room for the animal to move naturally."
        listOf("room", "furniture", "sofa", "table", "indoor", "kitchen").any { it in words } ->
            "INDOOR / INTERIOR" to "Use available window light, avoid a cluttered background, and show the room with a stable wide shot first."
        labels.isEmpty() -> "SCENE NOT CLEAR YET" to "Pause briefly, point at the main subject, and wait for the visual labels to update."
        else -> "GENERAL SCENE" to "Begin with a steady wide shot, keep the main subject clearly framed, and record a short detail shot for variety."
    }
}

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
    var recordedClips by remember { mutableStateOf(listOf<RecordedClip>()) }
    var projectBpm by remember { mutableIntStateOf(0) }

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
                            onOpenPlan = { tab = StudioTab.Plan },
                            onBpmDetected = { projectBpm = it }
                        )
                        StudioTab.Camera -> CameraScreen(
                            activeShot = shots.firstOrNull { it.id !in completed },
                            onShotCaptured = { id -> completed = completed + id },
                            onBack = { tab = StudioTab.Plan },
                            onSaved = { path, shotId, shotTitle ->
                                recordedClips = listOf(RecordedClip(path, shotId, shotTitle)) + recordedClips
                                notice = "Saved video: ${File(path).name}"
                            }
                        )
                        StudioTab.Edit -> EditScreen(
                            title = projectTitle,
                            completedCount = completed.size,
                            totalCount = shots.size,
                            notice = notice,
                            clips = recordedClips,
                            projectBpm = projectBpm,
                            onReorderClips = { from, to ->
                                val mutable = recordedClips.toMutableList()
                                if (from in mutable.indices && to in mutable.indices) {
                                    val clip = mutable.removeAt(from)
                                    mutable.add(to, clip)
                                    recordedClips = mutable
                                }
                            },
                            onRetake = { clip ->
                                clip.shotId?.let { id -> completed = completed - id }
                                tab = StudioTab.Camera
                            },
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

private data class RecordedClip(val path: String, val shotId: Int?, val shotTitle: String)

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
    onOpenPlan: () -> Unit,
    onBpmDetected: (Int) -> Unit
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
    var selectedMusicUri by remember { mutableStateOf<Uri?>(null) }
    var selectedMusicName by remember { mutableStateOf("No song selected") }
    var musicDurationMs by remember { mutableLongStateOf(0L) }
    var tapTimes by remember { mutableStateOf(listOf<Long>()) }
    var detectedBpm by remember { mutableIntStateOf(0) }
    var viralStyle by remember { mutableStateOf("Cinematic travel") }
    var beatStatus by remember { mutableStateOf("Select a song, then tap along with its beat 8 times.") }
    val musicPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            selectedMusicUri = uri
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, uri)
                selectedMusicName = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    ?: uri.lastPathSegment?.substringAfterLast('/')
                    ?: "Selected audio"
                musicDurationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                retriever.release()
                beatStatus = if (musicDurationMs > 0) "Song loaded · duration ${formatDuration(musicDurationMs)} · tap along with the beat" else "Song selected · tap along with the beat"
            } catch (_: Exception) {
                selectedMusicName = uri.lastPathSegment ?: "Selected audio"
                musicDurationMs = 0L
                beatStatus = "Song selected, but its metadata could not be read"
            }
            tapTimes = emptyList()
            detectedBpm = 0
            onBpmDetected(0)
        }
    }
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
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF142A31)), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MusicNote, null, tint = Cyan, modifier = Modifier.size(21.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("MODULE 12 · MUSIC & BEAT ANALYZER", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Text("Sync your shots to the song", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(selectedMusicName, color = Violet, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                    if (musicDurationMs > 0L) Text("Track length: ${formatDuration(musicDurationMs)}", color = Muted, fontSize = 10.sp)
                    Button(onClick = { musicPicker.launch(arrayOf("audio/*")) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.LibraryMusic, null); Spacer(Modifier.width(7.dp)); Text("Choose song from phone")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(if (detectedBpm > 0) "$detectedBpm BPM" else "Tap tempo", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                            Text(if (detectedBpm > 0) "Approx. ${60000 / detectedBpm} ms per beat" else "${tapTimes.size}/8 beat taps", color = Muted, fontSize = 10.sp)
                        }
                        Button(
                            onClick = {
                                val now = SystemClock.elapsedRealtime()
                                val recent = (tapTimes + now).takeLast(8)
                                tapTimes = recent
                                if (recent.size >= 4) {
                                    val intervals = recent.zipWithNext { a, b -> b - a }.filter { it in 250L..2000L }
                                    if (intervals.size >= 3) {
                                        val sorted = intervals.sorted()
                                        val median = sorted[sorted.size / 2]
                                        detectedBpm = (60000.0 / median).toInt().coerceIn(30, 240)
                                        onBpmDetected(detectedBpm)
                                        beatStatus = "Estimated from your taps · tap Reset if you lose the beat"
                                    } else {
                                        detectedBpm = 0
                                        beatStatus = "Tap steadily with each beat; ignored unusually short or long gaps"
                                    }
                                } else {
                                    beatStatus = "Keep tapping once per beat (${recent.size}/8)"
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Violet, contentColor = Ink)
                        ) {
                            Icon(Icons.Default.TouchApp, null); Spacer(Modifier.width(5.dp)); Text("Tap the beat")
                        }
                    }
                    if (detectedBpm > 0) {
                        val beatMs = 60000L / detectedBpm
                        val barMs = beatMs * 4L
                        Surface(color = Color.Black.copy(alpha = .22f), shape = RoundedCornerShape(12.dp)) {
                            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text("SHOT TIMING SUGGESTIONS", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text("Quick cuts: ${formatDuration(beatMs)} per beat", color = Color.White, fontSize = 11.sp)
                                Text("Smooth cinematic cuts: ${formatDuration(barMs)} every 4 beats", color = Color.White, fontSize = 11.sp)
                                Text("Suggested edit rhythm: ${if (mood == "Energetic") "1 beat per cut" else "2–4 beats per cut"} for $mood mood", color = Muted, fontSize = 10.sp)
                            }
                        }
                    }
                    Text(beatStatus, color = Muted, fontSize = 10.sp, lineHeight = 15.sp)
                    OutlinedButton(onClick = { tapTimes = emptyList(); detectedBpm = 0; onBpmDetected(0); beatStatus = "Tap along with the song 8 times to estimate tempo" }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(6.dp)); Text("Reset tempo")
                    }
                    Text("This version reads basic audio metadata and estimates BPM from manual beat taps. It does not yet automatically detect beats from the song or add the track to the video timeline.", color = Muted, fontSize = 10.sp, lineHeight = 15.sp)
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
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF2B1B32)), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("MODULE 14 · VIRAL STYLE ANALYZER", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Choose a short-form pattern", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    FlowChips(listOf("Cinematic travel", "Dance hook", "Story reveal", "Product showcase", "Portrait montage", "Fast-cut vlog"), viralStyle) { viralStyle = it }
                    val advice = when (viralStyle) {
                        "Dance hook" -> "Lead with the strongest move, establish the full body, add a detail cut, then return to the signature move."
                        "Story reveal" -> "Start with a question, add context shots, reveal the subject, and finish on a reaction."
                        "Product showcase" -> "Show the whole item, then texture, use/context, and a clean final hero frame."
                        "Portrait montage" -> "Mix a medium portrait, close-up expression, walking cutaway, and final held portrait."
                        "Fast-cut vlog" -> "Use a clear hook, three action/detail cutaways, and a concise ending."
                        else -> "Reveal the location wide, add foreground depth and detail, then finish with a strong wide frame."
                    }
                    Text(advice, color = Color.White, fontSize = 12.sp, lineHeight = 18.sp)
                    Text("Pattern guide only; no live trend scraping or guarantee of virality.", color = Muted, fontSize = 10.sp)
                }
            }
        }
        item { Text("Scene direction and style advice use on-device rules. This is not a generative AI analysis of current social trends.", color = Muted, fontSize = 10.sp, lineHeight = 16.sp) }
    }
}

private fun shotsForBeat(bpm: Int): List<String> {
    val beat = 60.0 / bpm.coerceIn(30, 240)
    val bar = beat * 4.0
    return listOf(
        "Opening hook — ${"%.2f".format(Locale.US, beat)}s per beat",
        "Detail cutaway — ${"%.2f".format(Locale.US, bar)}s per 4 beats",
        "Hero moment — hold ${"%.2f".format(Locale.US, bar * 2)}s for 8 beats"
    )
}

private fun formatDuration(milliseconds: Long): String {
    val totalSeconds = (milliseconds / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%d:%02d".format(Locale.US, minutes, seconds)
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
private fun CameraScreen(activeShot: Shot?, onShotCaptured: (Int) -> Unit, onBack: () -> Unit, onSaved: (String, Int?, String) -> Unit) {
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
    var liveSceneType by remember { mutableStateOf("Analyzing scene…") }
    var liveSceneAdvice by remember { mutableStateOf("Hold the phone steady while the scene is analyzed.") }
    var liveSceneLabels by remember { mutableStateOf("No labels yet") }
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
    var checkedFocus by remember { mutableStateOf(false) }
    var checkedExposure by remember { mutableStateOf(false) }
    var checkedBackground by remember { mutableStateOf(false) }
    var checkedStability by remember { mutableStateOf(false) }
    var selectedPose by remember { mutableStateOf("Confident hero") }
    var selectedExpression by remember { mutableStateOf("Natural confidence") }
    val preflightReady = checkedFocus && checkedExposure && checkedBackground && checkedStability
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
                                    val rankedLabels = labels.sortedByDescending { it.confidence }.take(5)
                                    val best = rankedLabels.firstOrNull()
                                    val labelNames = rankedLabels.map { it.text }
                                    val interpretation = interpretSceneLabels(labelNames)
                                    executor.execute {
                                        liveSceneType = interpretation.first
                                        liveSceneAdvice = interpretation.second
                                        liveSceneLabels = if (labelNames.isEmpty()) "No confident visual labels" else labelNames.joinToString(" · ")
                                        liveScene = if (best != null && best.confidence >= 0.35f) {
                                            "Visual cues: ${(best.confidence * 100).toInt()}% strongest label"
                                        } else {
                                            "Scene labels are uncertain · keep framing simple"
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
                                    Text(liveSceneType, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    Text(liveScene, color = Muted, fontSize = 9.sp, maxLines = 2)
                                    Text("DIRECTOR TIP · $liveSceneAdvice", color = Cyan, fontSize = 10.sp, maxLines = 3, lineHeight = 14.sp)
                                    Text("Cues: $liveSceneLabels", color = Muted, fontSize = 9.sp, maxLines = 2)
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
            colors = CardDefaults.cardColors(containerColor = Color(0xFF202034)),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Checklist, null, tint = Cyan, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("MODULE 8 · SMART CAPTURE CHECKLIST", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                }
                Text(activeShot?.let { "NEXT SHOT: ${it.title} · ${it.angle}" } ?: "All planned shots are marked complete", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(activeShot?.let { "Move: ${it.movement} · ${it.duration}s target" } ?: "Return to the shot planner to start another sequence.", color = Muted, fontSize = 10.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = checkedFocus, onCheckedChange = { checkedFocus = it })
                    Text("Focus", color = Color.White, fontSize = 11.sp)
                    Spacer(Modifier.width(6.dp))
                    Checkbox(checked = checkedExposure, onCheckedChange = { checkedExposure = it })
                    Text("Light", color = Color.White, fontSize = 11.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = checkedBackground, onCheckedChange = { checkedBackground = it })
                    Text("Background", color = Color.White, fontSize = 11.sp)
                    Spacer(Modifier.width(6.dp))
                    Checkbox(checked = checkedStability, onCheckedChange = { checkedStability = it })
                    Text("Stable", color = Color.White, fontSize = 11.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = if (preflightReady) Color(0xFF123C35) else Color(0xFF3A2D24), shape = RoundedCornerShape(9.dp)) {
                        Text(if (preflightReady) "READY TO RECORD" else "CHECK ${listOf(checkedFocus, checkedExposure, checkedBackground, checkedStability).count { it }}/4 BEFORE RECORDING", Modifier.padding(horizontal = 10.dp, vertical = 7.dp), color = if (preflightReady) Cyan else Color(0xFFFFD166), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("Saved clip marks this shot done", color = Muted, fontSize = 9.sp)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF211B35)),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessibilityNew, null, tint = Violet, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("MODULE 10 · POSE & EXPRESSION COACH", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                }
                Text("Choose the feeling before you record", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("POSE", color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                FlowChips(
                    options = listOf("Confident hero", "Relaxed natural", "Walking candid", "Power stance", "Look-back portrait", "Seated cinematic"),
                    selected = selectedPose,
                    onSelect = { selectedPose = it }
                )
                Text("EXPRESSION", color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                FlowChips(
                    options = listOf("Natural confidence", "Warm smile", "Serious intensity", "Dreamy / thoughtful", "Playful energy", "Calm neutral"),
                    selected = selectedExpression,
                    onSelect = { selectedExpression = it }
                )
                val poseCue = when (selectedPose) {
                    "Relaxed natural" -> "Relax your shoulders, shift weight onto one leg, keep hands loose, and breathe normally."
                    "Walking candid" -> "Walk slowly past the camera, look slightly beyond the lens, and repeat the walk for a clean take."
                    "Power stance" -> "Plant feet comfortably, lengthen your posture, keep shoulders relaxed, and angle your body slightly."
                    "Look-back portrait" -> "Turn your body away first, then look back toward the lens; keep the neck relaxed."
                    "Seated cinematic" -> "Sit tall near the edge of the seat, angle knees slightly sideways, and keep hands intentional."
                    else -> "Stand at a slight angle to the camera, lengthen your posture, relax your shoulders, and keep your chin natural."
                }
                val expressionCue = when (selectedExpression) {
                    "Warm smile" -> "Think of someone you genuinely like; let the smile reach your eyes instead of forcing it."
                    "Serious intensity" -> "Relax your jaw, focus just beside the lens, and hold a steady gaze without frowning hard."
                    "Dreamy / thoughtful" -> "Look toward a distant point, soften your eyes, and pause as if remembering something."
                    "Playful energy" -> "Use a small natural reaction, shift your gaze once, and keep the movement light."
                    "Calm neutral" -> "Unclench your jaw, breathe out slowly, and keep your face relaxed."
                    else -> "Keep your eyes engaged, breathe naturally, and avoid holding tension in your forehead or jaw."
                }
                Surface(color = Color.Black.copy(alpha = .22f), shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("YOUR POSE CUE", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(poseCue, color = Color.White, fontSize = 11.sp, lineHeight = 16.sp)
                        Text("YOUR EXPRESSION CUE", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(expressionCue, color = Color.White, fontSize = 11.sp, lineHeight = 16.sp)
                    }
                }
                Text("Tip: record a 3–5 second test, review it, then retake if the pose feels stiff. This module gives guided prompts; it does not yet automatically score body pose or facial expression.", color = Muted, fontSize = 10.sp, lineHeight = 14.sp)
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
                                                activeShot?.let { onShotCaptured(it.id) }
                                                checkedFocus = false
                                                checkedExposure = false
                                                checkedBackground = false
                                                checkedStability = false
                                                onSaved(file.absolutePath, activeShot?.id, activeShot?.title ?: "Unassigned clip")
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
private fun EditScreen(
    title: String,
    completedCount: Int,
    totalCount: Int,
    notice: String,
    clips: List<RecordedClip>,
    projectBpm: Int,
    onReorderClips: (Int, Int) -> Unit,
    onRetake: (RecordedClip) -> Unit,
    onCamera: () -> Unit,
    onPlan: () -> Unit
) {
    var transitionStyle by remember { mutableStateOf("Crossfade") }
    var selectedQualityChecks by remember { mutableStateOf(setOf<String>()) }
    var trimStartSeconds by remember { mutableStateOf("0") }
    var trimEndSeconds by remember { mutableStateOf("10") }
    var splitAtSeconds by remember { mutableStateOf("5") }
    var editNotice by remember { mutableStateOf("These edit controls prepare edit points; final video rendering is not connected yet.") }
    var gradingPreset by remember { mutableStateOf("Cinematic teal") }
    var filterPreset by remember { mutableStateOf("Clean") }
    var audioLevel by remember { mutableFloatStateOf(1f) }
    var audioFade by remember { mutableStateOf("Fade in + out") }
    var subtitleText by remember { mutableStateOf("") }
    var autoEditStyle by remember { mutableStateOf("Cinematic story") }
    var exportResolution by remember { mutableStateOf("1080p") }
    var exportFps by remember { mutableStateOf("30 fps") }
    var projectNote by remember { mutableStateOf("My first music video") }
    var directorPreference by remember { mutableStateOf("Smooth cinematic movement") }
    var performanceMode by remember { mutableStateOf("Balanced") }
    var releaseChecks by remember { mutableStateOf(setOf<String>()) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        item { StudioHeader("Module 09 · Review", "Clip review & retake", "Watch the footage you recorded, check each take, and return to the same planned shot when it needs another take.") }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(progress = { if (totalCount == 0) 0f else completedCount.toFloat() / totalCount }, modifier = Modifier.fillMaxWidth(), color = Cyan, trackColor = Color.White.copy(alpha = .08f))
                    Spacer(Modifier.height(8.dp))
                    Text("$completedCount of $totalCount planned shots marked complete · ${clips.size} saved clip(s)", color = Muted, fontSize = 11.sp)
                }
            }
        }
        item { ToolCard(Icons.Default.Videocam, "Record another clip", "Open the CameraX recording screen", "SHOOT", onCamera) }
        item { ToolCard(Icons.Default.Movie, "Review shot sequence", "Check the planned shot list", "PLAN", onPlan) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF142A31)), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("MODULE 13 · BEAT-SYNCED SHOT PLANNER", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                    Text(if (projectBpm > 0) "Tempo: $projectBpm BPM" else "No BPM detected yet", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    val beatMs = if (projectBpm > 0) 60000L / projectBpm else 0L
                    Text(if (projectBpm > 0) "Quick cut: ${beatMs}ms; cinematic cut: ${beatMs * 4}ms (4 beats)." else "Set tempo in Director → Music & Beat Analyzer to get cut timing suggestions.", color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
                    if (projectBpm > 0) shotsForBeat(projectBpm).forEachIndexed { i, line -> Text("${i + 1}. $line", color = Color.White, fontSize = 11.sp) }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF2B1B32)), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("MODULE 16 · SHOT QUALITY CHECKER", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Check the take before editing", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    listOf("Focus looks sharp", "Exposure looks balanced", "Subject is framed well", "Camera movement is smooth", "Audio is clear").forEach { check ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = check in selectedQualityChecks, onCheckedChange = { yes -> selectedQualityChecks = if (yes) selectedQualityChecks + check else selectedQualityChecks - check })
                            Text(check, color = Color.White, fontSize = 12.sp)
                        }
                    }
                    Text("${selectedQualityChecks.size}/5 checks passed · Manual review only; automatic quality scoring is not implemented.", color = Muted, fontSize = 10.sp)
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("MODULE 18 · TIMELINE EDITOR", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Use Up/Down on each clip below to arrange the sequence.", color = Muted, fontSize = 11.sp)
                    Text("MODULE 19 · TRIM & SPLIT", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = trimStartSeconds, onValueChange = { trimStartSeconds = it.filter(Char::isDigit).take(5) }, modifier = Modifier.weight(1f), label = { Text("Start (s)") }, singleLine = true)
                        OutlinedTextField(value = trimEndSeconds, onValueChange = { trimEndSeconds = it.filter(Char::isDigit).take(5) }, modifier = Modifier.weight(1f), label = { Text("End (s)") }, singleLine = true)
                    }
                    OutlinedTextField(value = splitAtSeconds, onValueChange = { splitAtSeconds = it.filter(Char::isDigit).take(5) }, modifier = Modifier.fillMaxWidth(), label = { Text("Split marker (s)") }, singleLine = true)
                    Button(onClick = {
                        val start = trimStartSeconds.toLongOrNull()
                        val end = trimEndSeconds.toLongOrNull()
                        val split = splitAtSeconds.toLongOrNull()
                        editNotice = when {
                            start == null || end == null || split == null -> "Enter valid whole-number seconds."
                            start >= end -> "End must be greater than start."
                            split <= start || split >= end -> "Split marker must be between start and end."
                            else -> "Edit plan: keep $start–$end seconds; split at $split seconds. Export rendering is required to alter the file."
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Validate edit points") }
                    Text(editNotice, color = Muted, fontSize = 10.sp, lineHeight = 15.sp)
                    Text("MODULE 20 · TRANSITIONS ENGINE", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                    FlowChips(listOf("Cut", "Crossfade", "Fade to black", "Dissolve", "Whip-pan"), transitionStyle) {
                        transitionStyle = it
                        editNotice = "Transition selected: $it. This is a setting preview; rendered transitions are not wired yet."
                    }
                    Text("Selected transition: $transitionStyle", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF202A43)), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("MODULE 21 · COLOR GRADING", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Choose a color mood", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    FlowChips(listOf("Cinematic teal", "Warm sunset", "Natural", "Moody", "Bright social"), gradingPreset) { gradingPreset = it; editNotice = "Color mood selected: $it. Pixel-level grading is not rendered yet." }
                    Text("Preview setting: $gradingPreset", color = Muted, fontSize = 11.sp)
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("MODULE 22 · AUDIO EDITING", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Clip audio level: ${(audioLevel * 100).toInt()}%", color = Color.White, fontWeight = FontWeight.Bold)
                    Slider(value = audioLevel, onValueChange = { audioLevel = it }, valueRange = 0f..1f)
                    FlowChips(listOf("No fade", "Fade in", "Fade out", "Fade in + out"), audioFade) { audioFade = it }
                    Text("Audio settings are saved in this screen state only; they do not change the recorded file yet.", color = Muted, fontSize = 10.sp)
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF142A31)), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("MODULE 23 · AI AUTO-EDIT PLAN", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                    FlowChips(listOf("Cinematic story", "Fast social reel", "Travel montage", "Product promo"), autoEditStyle) { autoEditStyle = it }
                    Button(onClick = {
                        editNotice = when {
                            clips.isEmpty() -> "Record at least one clip before creating an edit plan."
                            autoEditStyle == "Fast social reel" -> "Auto-edit plan: open with the strongest hook, use short detail cuts, keep the subject visible, and finish on a clear ending. No rendered montage was created."
                            autoEditStyle == "Travel montage" -> "Auto-edit plan: wide location reveal → movement shot → environmental detail → hero view → held ending. No rendered montage was created."
                            autoEditStyle == "Product promo" -> "Auto-edit plan: product hero → texture close-up → use/context → benefit detail → clean final frame. No rendered montage was created."
                            else -> "Auto-edit plan: establish the scene → show action → add detail → build to the hero moment → hold the ending. No rendered montage was created."
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Create edit plan") }
                    Text("Rule-based edit suggestions; automatic clip analysis, trimming and merging are not connected.", color = Muted, fontSize = 10.sp)
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("MODULE 24 · SUBTITLES & CAPTIONS", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                    OutlinedTextField(value = subtitleText, onValueChange = { subtitleText = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Caption text") }, placeholder = { Text("Type your caption here") })
                    Text("${subtitleText.length} characters · Text entry only; speech-to-text and burned-in captions are not implemented.", color = Muted, fontSize = 10.sp)
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF2B1B32)), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("MODULE 25 · EFFECTS & FILTERS", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                    FlowChips(listOf("Clean", "Film grain", "Soft glow", "Vintage", "High contrast", "Monochrome"), filterPreset) { filterPreset = it; editNotice = "Effect selected: $it. Visual filter rendering is not connected yet." }
                    Text("Selected effect: $filterPreset", color = Cyan, fontSize = 11.sp)
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF123C35)), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("MODULE 26 · VIDEO EXPORT", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                    FlowChips(listOf("720p", "1080p", "4K"), exportResolution) { exportResolution = it }
                    FlowChips(listOf("24 fps", "30 fps", "60 fps"), exportFps) { exportFps = it }
                    Button(onClick = {
                        editNotice = if (clips.isEmpty()) "Export unavailable: record clips first." else "Export preset selected: $exportResolution · $exportFps. MP4 rendering/merging is not connected yet, so no export file was created."
                    }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.FileDownload, null); Spacer(Modifier.width(6.dp)); Text("Prepare export settings") }
                    Text("This button prepares settings only; it does not create a new MP4.", color = Muted, fontSize = 10.sp)
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("MODULE 27 · PROJECT & DRAFT", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                    OutlinedTextField(value = projectNote, onValueChange = { projectNote = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Project name") }, singleLine = true)
                    Text("Draft label: $projectNote", color = Muted, fontSize = 11.sp)
                    Text("Currently held in app memory; persistent save/restore is not implemented.", color = Muted, fontSize = 10.sp)
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF202A43)), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("MODULE 28 · DIRECTOR PERSONALIZATION", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                    FlowChips(listOf("Smooth cinematic movement", "Fast energetic cuts", "Natural documentary", "Portrait-focused"), directorPreference) { directorPreference = it }
                    Text("Preferred direction: $directorPreference", color = Color.White, fontSize = 11.sp)
                    Text("Preference is a local UI selection; adaptive learning is not implemented.", color = Muted, fontSize = 10.sp)
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("MODULE 29 · DEVICE PERFORMANCE", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                    FlowChips(listOf("Battery saver", "Balanced", "High quality"), performanceMode) { performanceMode = it }
                    Text(when (performanceMode) {
                        "Battery saver" -> "Recommended: reduce background work and prefer short preview sessions. Actual encoder/analysis throttling is not yet tied to this setting."
                        "High quality" -> "Recommended: use a well-lit scene and a device that supports the selected capture quality. Export quality controls are not wired to rendering."
                        else -> "Recommended: keep analysis throttled and close unused apps. The setting is a preference only at this stage."
                    }, color = Muted, fontSize = 10.sp, lineHeight = 15.sp)
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF2B1B32)), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("MODULE 30 · RELEASE READINESS", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                    listOf("Camera permission and recording tested", "Front/back camera and orientation checked", "Playback and retake checked", "Trim/transition/export rendering tested", "Tested on Android 16 device").forEach { check ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = check in releaseChecks, onCheckedChange = { yes -> releaseChecks = if (yes) releaseChecks + check else releaseChecks - check })
                            Text(check, color = Color.White, fontSize = 11.sp)
                        }
                    }
                    Text("${releaseChecks.size}/5 release checks marked. Checkboxes are manual tracking, not automated test results.", color = Muted, fontSize = 10.sp)
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PlayCircle, null, tint = Cyan, modifier = Modifier.size(21.dp))
                Spacer(Modifier.width(8.dp))
                Text("Recorded clips", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
        if (clips.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.VideoLibrary, null, tint = Violet, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("No clips to review yet", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Record a shot first. Your saved clips will appear here for playback and retakes.", color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
                    }
                }
            }
        } else {
            items(clips, key = { it.path }) { clip ->
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Text(clip.shotTitle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(File(clip.path).name, color = Muted, fontSize = 10.sp)
                        val clipFile = File(clip.path)
                        Text(if (clipFile.exists()) "File size: ${"%.1f".format(Locale.US, clipFile.length() / (1024f * 1024f))} MB" else "File is no longer available", color = Cyan, fontSize = 10.sp)
                        if (clipFile.exists()) {
                            AndroidView(
                                modifier = Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(14.dp)),
                                factory = { context ->
                                    VideoView(context).apply {
                                        setVideoPath(clip.path)
                                        setMediaController(MediaController(context).also { it.setAnchorView(this) })
                                    }
                                },
                                update = { view ->
                                    if (view.tag != clip.path) {
                                        view.tag = clip.path
                                        view.setVideoPath(clip.path)
                                    }
                                }
                            )
                        }
                        val clipIndex = clips.indexOf(clip)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(onClick = { onReorderClips(clipIndex, clipIndex - 1) }, enabled = clipIndex > 0, modifier = Modifier.weight(1f)) { Icon(Icons.Default.KeyboardArrowUp, null); Text("Up") }
                            OutlinedButton(onClick = { onReorderClips(clipIndex, clipIndex + 1) }, enabled = clipIndex < clips.lastIndex, modifier = Modifier.weight(1f)) { Icon(Icons.Default.KeyboardArrowDown, null); Text("Down") }
                        }
                        Button(onClick = { onRetake(clip) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Violet, contentColor = Ink)) {
                            Icon(Icons.Default.Replay, null); Spacer(Modifier.width(6.dp)); Text("Retake this shot")
                        }
                        Text("Retake keeps the old file. It marks this planned shot incomplete so the camera can guide you through it again.", color = Muted, fontSize = 10.sp, lineHeight = 14.sp)
                    }
                }
            }
        }
        if (notice.isNotBlank()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF123C35)), shape = RoundedCornerShape(14.dp)) {
                    Text(notice, color = Cyan, fontSize = 11.sp, modifier = Modifier.padding(12.dp))
                }
            }
        }
        item {
            Text("Review tools help you inspect footage and repeat a shot. Automatic blur, focus, exposure, and audio-quality scoring are not included in this module yet.", color = Muted, fontSize = 10.sp, lineHeight = 15.sp)
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
