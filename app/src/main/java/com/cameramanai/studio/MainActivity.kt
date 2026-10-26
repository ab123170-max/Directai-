package com.cameramanai.studio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
    Home("Studio"), Plan("Shot list"), Camera("Camera"), Edit("Edit"), Settings("Settings")
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
                            onEdit = { tab = StudioTab.Edit }
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
                            onShoot = { tab = StudioTab.Camera }
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
private fun HomeScreen(title: String, completedCount: Int, totalCount: Int, onStart: () -> Unit, onCamera: () -> Unit, onEdit: () -> Unit) {
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
    onToggleShot: (Int) -> Unit, onShoot: () -> Unit
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { StudioHeader("Pre-production", "Shot planner", "A practical shot list for your next music video or Reel.") }
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Your shot sequence", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("${completed.size}/${shots.size} done", color = Cyan, fontSize = 11.sp)
            }
        }
        items(shots) { shot ->
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(15.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = Violet.copy(alpha = .16f), shape = RoundedCornerShape(10.dp)) {
                            Text("%02d".format(shot.id), Modifier.padding(horizontal = 11.dp, vertical = 8.dp), color = Violet, fontWeight = FontWeight.ExtraBold)
                        }
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text(shot.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("${shot.angle} · ${shot.duration}s", color = Muted, fontSize = 10.sp)
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
private fun CameraScreen(onBack: () -> Unit, onSaved: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { ContextCompat.getMainExecutor(context) }
    var hasPermissions by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var recording by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Ready to shoot") }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var activeRecording by remember { mutableStateOf<androidx.camera.video.Recording?>(null) }
    var lens by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        hasPermissions = grants[Manifest.permission.CAMERA] == true
        if (!hasPermissions) status = "Camera permission is required to record."
    }

    DisposableEffect(hasPermissions, lens, previewView) {
        if (!hasPermissions || previewView == null) return@DisposableEffect onDispose { }
        val providerFuture = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            try {
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView!!.surfaceProvider }
                val recorder = Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HD)).build()
                val capture = VideoCapture.withOutput(recorder)
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, CameraSelector.Builder().requireLensFacing(lens).build(), preview, capture)
                videoCapture = capture
                status = "Live camera ready · HD"
            } catch (e: Exception) {
                status = "Camera unavailable: ${e.localizedMessage ?: "check camera access"}"
            }
        }
        providerFuture.addListener(listener, executor)
        onDispose { try { providerFuture.get().unbindAll() } catch (_: Exception) {} }
    }

    Column(Modifier.fillMaxSize().background(Ink).padding(horizontal = 14.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = Color.White) }
            Column(Modifier.weight(1f)) {
                Text("DIRECTOR CAMERA", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text(status, color = Muted, fontSize = 11.sp)
            }
            IconButton(onClick = { if (!recording) lens = if (lens == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK }) {
                Icon(Icons.Default.Cameraswitch, "Switch camera", tint = Color.White)
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth().padding(vertical = 10.dp).clip(RoundedCornerShape(24.dp)).background(Color.Black), contentAlignment = Alignment.Center) {
            if (hasPermissions) {
                AndroidView(
                    factory = { ctx -> PreviewView(ctx).also { it.scaleType = PreviewView.ScaleType.FILL_CENTER; previewView = it } },
                    modifier = Modifier.fillMaxSize()
                )
                Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Surface(color = Color.Black.copy(alpha = .55f), shape = CircleShape) { Text("● LIVE", Modifier.padding(horizontal = 12.dp, vertical = 7.dp), color = Color(0xFFFF6B78), fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                        Surface(color = Color.Black.copy(alpha = .55f), shape = CircleShape) { Text("HD · 30 FPS*", Modifier.padding(horizontal = 12.dp, vertical = 7.dp), color = Color.White, fontSize = 10.sp) }
                    }
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Add, null, tint = Color.White.copy(alpha = .6f), modifier = Modifier.size(30.dp))
                            Text("Keep the subject in the center", color = Color.White, fontSize = 11.sp)
                        }
                    }
                    Text("Rule of thirds · Hold steady · Move slowly", color = Color.White, fontSize = 10.sp)
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Icon(Icons.Default.NoPhotography, null, tint = Violet, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Camera permission needed", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Allow camera and microphone access to record clips.", color = Muted, fontSize = 12.sp)
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = { permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)) }) { Text("Allow camera") }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Grid3x3, null, tint = Muted)
                Text("Grid", color = Muted, fontSize = 10.sp)
            }
            Box(Modifier.size(76.dp).clip(CircleShape).background(if (recording) Color(0xFFFF5364) else Color.White).padding(5.dp), contentAlignment = Alignment.Center) {
                IconButton(
                    enabled = hasPermissions && videoCapture != null,
                    onClick = {
                        val capture = videoCapture ?: return@IconButton
                        if (recording) {
                            activeRecording?.stop()
                            activeRecording = null
                            recording = false
                            status = "Finishing clip…"
                        } else {
                            val folder = File(context.getExternalFilesDir(null), "CameramanAI")
                            folder.mkdirs()
                            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                            val file = File(folder, "CAM_$stamp.mp4")
                            val options = FileOutputOptions.Builder(file).build()
                            var pending = capture.output.prepareRecording(context, options)
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) pending = pending.withAudioEnabled()
                            activeRecording = pending.start(executor) { event ->
                                when (event) {
                                    is VideoRecordEvent.Start -> { recording = true; status = "Recording…"; }
                                    is VideoRecordEvent.Finalize -> {
                                        recording = false
                                        activeRecording = null
                                        if (!event.hasError()) {
                                            status = "Clip saved · ${file.name}"
                                            onSaved(file.absolutePath)
                                        } else status = "Recording failed: ${event.error}"
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(if (recording) Icons.Default.Stop else Icons.Default.FiberManualRecord, if (recording) "Stop recording" else "Start recording", tint = if (recording) Color.White else Color(0xFFFF5364), modifier = Modifier.size(34.dp))
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.FlipCameraAndroid, null, tint = Muted)
                Text("Flip", color = Muted, fontSize = 10.sp)
            }
        }
        Text("Recorded clips are saved inside app storage on this device.", color = Muted, fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 4.dp))
    }
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
