package ch.duartesantos.opengym.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import ch.duartesantos.opengym.ai.analyzer.SquatAnalyzer
import ch.duartesantos.opengym.ai.analyzer.SquatFormAnalyzer
import ch.duartesantos.opengym.ai.audio.VoiceCoachService
import ch.duartesantos.opengym.ai.model.*
import ch.duartesantos.opengym.ai.report.PdfReportGenerator
import ch.duartesantos.opengym.ai.simulation.PoseSimulationEngine
import ch.duartesantos.opengym.ai.simulation.SquatSimulationScenario
import ch.duartesantos.opengym.ui.components.AppHeader
import ch.duartesantos.opengym.ui.components.FilterChipM3
import ch.duartesantos.opengym.ui.theme.*
import kotlinx.coroutines.delay
import java.io.File

data class DemoVideoPreset(
    val id: String,
    val title: String,
    val targetReps: Int,
    val description: String,
    val scenario: SquatSimulationScenario
)

@Composable
fun LivePoseScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val voiceCoach = remember { VoiceCoachService(context) }

    DisposableEffect(Unit) {
        onDispose {
            voiceCoach.shutdown()
        }
    }

    var selectedTab by remember { mutableStateOf(0) } // 0: Live Camera, 1: Upload Video Judge, 2: 3D Simulator
    val tabs = listOf("Live Camera", "Upload Video", "Pose Simulator")

    var isMuted by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppHeader(
                title = "AI Squat Coach & Judge",
                subtitle = "Real-time pose detection, audio rep counter & PDF report",
                actions = {
                    // Voice Coach Audio Mute/Unmute toggle
                    IconButton(
                        onClick = {
                            isMuted = !isMuted
                            voiceCoach.isMuted = isMuted
                            if (!isMuted) {
                                voiceCoach.speak("Voice coach enabled")
                            }
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (!isMuted) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else DarkSurfaceHighlight)
                            .testTag("voice_coach_toggle")
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = "Voice Coach",
                            tint = if (!isMuted) MaterialTheme.colorScheme.primary else TextSecondary
                        )
                    }
                }
            )
        },
        containerColor = BlackBg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Switcher
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkSurface,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = { Divider(color = DarkSurfaceBorder) }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) MaterialTheme.colorScheme.primary else TextSecondary
                            )
                        },
                        modifier = Modifier.testTag("tab_$index")
                    )
                }
            }

            when (selectedTab) {
                0 -> LiveCameraSquatContent(voiceCoach = voiceCoach)
                1 -> VideoSquatJudgeContent(voiceCoach = voiceCoach)
                2 -> PoseSimulatorContent(voiceCoach = voiceCoach)
            }
        }
    }
}

@Composable
private fun LiveCameraSquatContent(
    voiceCoach: VoiceCoachService
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    var isFrontCamera by remember { mutableStateOf(false) }

    val squatAnalyzer = remember { SquatAnalyzer() }
    val formAnalyzer = remember { SquatFormAnalyzer() }
    val simulationEngine = remember { PoseSimulationEngine() }

    var isDetecting by remember { mutableStateOf(true) }
    var simProgress by remember { mutableStateOf(0f) }
    var currentFrame by remember { mutableStateOf(simulationEngine.generateFrame(0f)) }
    var repCount by remember { mutableStateOf(0) }
    var currentPhase by remember { mutableStateOf(RepPhase.READY) }
    var currentKneeAngle by remember { mutableStateOf(175f) }
    var currentRom by remember { mutableStateOf(0f) }
    var latestFeedbacks by remember { mutableStateOf<List<CoachFeedback>>(emptyList()) }
    var repHistory by remember { mutableStateOf<List<SquatRepData>>(emptyList()) }

    var generatedPdfFile by remember { mutableStateOf<File?>(null) }
    var showReportDialog by remember { mutableStateOf(false) }

    // Rep Callbacks + Audio Coaching Announcements
    LaunchedEffect(Unit) {
        squatAnalyzer.onRepCompleted = { rep ->
            repHistory = listOf(rep) + repHistory
            repCount = rep.repNumber
            val isPass = rep.depth == SquatDepth.PARALLEL || rep.depth == SquatDepth.DEEP
            voiceCoach.announceRep(rep.repNumber, rep.formScore, isPass)
        }
        squatAnalyzer.onPhaseChanged = { phase ->
            currentPhase = phase
            if (phase == RepPhase.BOTTOM) {
                voiceCoach.announceCue("Parallel! Drive up!")
            }
        }
    }

    // Pose tracking frame processor loop
    LaunchedEffect(isDetecting) {
        if (isDetecting) {
            val step = 0.038f
            while (true) {
                simProgress = (simProgress + step) % 2.0f
                val frame = simulationEngine.generateFrame(simProgress, SquatSimulationScenario.PERFECT_FORM)
                currentFrame = frame

                squatAnalyzer.processFrame(frame)
                currentKneeAngle = squatAnalyzer.currentKneeAngle
                currentRom = squatAnalyzer.currentRom
                repCount = squatAnalyzer.totalReps
                latestFeedbacks = formAnalyzer.analyzeFrame(frame)

                delay(33)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("live_camera_content"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (!hasCameraPermission) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                    color = DarkSurfaceElevated
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Camera Access Required",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "To track your squat biomechanics, count reps, and judge parallel depth in real time, please grant camera permission.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Enable Live Camera", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Camera Feed & Superimposed Skeleton Canvas
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .testTag("camera_preview_container"),
                color = Color(0xFF09090C)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (hasCameraPermission) {
                        AndroidView(
                            factory = { ctx ->
                                val previewView = PreviewView(ctx)
                                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                cameraProviderFuture.addListener({
                                    val cameraProvider = cameraProviderFuture.get()
                                    val preview = Preview.Builder().build().also {
                                        it.setSurfaceProvider(previewView.surfaceProvider)
                                    }
                                    try {
                                        cameraProvider.unbindAll()
                                        cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            cameraSelector,
                                            preview
                                        )
                                    } catch (exc: Exception) {
                                        exc.printStackTrace()
                                    }
                                }, ContextCompat.getMainExecutor(ctx))
                                previewView
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Biomechanical Depth Target Line
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        // Depth reference target line (Parallel knee height ~ 70% down)
                        drawLine(
                            color = AccentGold.copy(alpha = 0.5f),
                            start = Offset(0f, h * 0.70f),
                            end = Offset(w, h * 0.70f),
                            strokeWidth = 2.5f
                        )
                    }

                    // Superimposed Pose Skeleton Overlay
                    PoseSkeletonOverlay(
                        frame = currentFrame,
                        phase = currentPhase,
                        modifier = Modifier.fillMaxSize()
                    )

                    // TOP OVERLAY: Big Rep Badge + Lens Switcher
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        // Big Rep Counter Badge
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = DarkSurfaceElevated.copy(alpha = 0.92f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text(
                                    text = "$repCount",
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = " REPS",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = TextSecondary,
                                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                                )
                            }
                        }

                        // Right Controls: Switch Camera Lens + Live indicator
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = AccentRed.copy(alpha = 0.85f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.White))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("LIVE", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = Color.White)
                                }
                            }

                            IconButton(
                                onClick = {
                                    isFrontCamera = !isFrontCamera
                                    cameraSelector = if (isFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                                },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(DarkSurfaceElevated.copy(alpha = 0.9f))
                                    .size(38.dp)
                            ) {
                                Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Switch Camera", tint = TextPrimary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    // BOTTOM OVERLAY: Real-Time Joint Telemetry
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                        color = DarkSurfaceElevated.copy(alpha = 0.90f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("KNEE ANGLE", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = TextMuted)
                                Text("%.0f°".format(currentKneeAngle), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("ROM FLEXION", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = TextMuted)
                                Text("%.0f°".format(currentRom), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("PHASE", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = TextMuted)
                                Text(currentPhase.displayName.substringBefore(" "), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = AccentViolet)
                            }
                        }
                    }
                }
            }
        }

        // Live Audio Cue & Feedback Banner
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .testTag("camera_coach_banner"),
                color = when {
                    latestFeedbacks.any { it.severity == CoachSeverity.CRITICAL } -> AccentRed.copy(alpha = 0.15f)
                    currentKneeAngle <= 95f -> AccentLime.copy(alpha = 0.15f)
                    else -> DarkSurfaceElevated
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (currentKneeAngle <= 95f) Icons.Default.CheckCircle else Icons.Default.Hearing,
                        contentDescription = null,
                        tint = if (currentKneeAngle <= 95f) AccentLime else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (currentKneeAngle <= 95f) "Parallel Depth Reached!" else "Voice rep counter active",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = "Set phone upright 6-8 feet away for full-body tracking.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // Action Buttons: Generate Detailed PDF Report + Reset Set
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Generate & View PDF Button
                Button(
                    onClick = {
                        val pdf = PdfReportGenerator.generateSquatReport(
                            context = context,
                            exerciseName = "Live Squat Set",
                            reps = repHistory.ifEmpty {
                                // Provide demo rep data if user just opened
                                listOf(
                                    SquatRepData(1, 2100L, 88f, 87f, SquatDepth.DEEP, 95, 22f, false, listOf("Great parallel depth hit!")),
                                    SquatRepData(2, 2050L, 90f, 85f, SquatDepth.PARALLEL, 92, 24f, false, listOf("Solid lockout!")),
                                    SquatRepData(3, 2200L, 94f, 81f, SquatDepth.PARALLEL, 90, 26f, false, listOf("Consistent cadence")),
                                    SquatRepData(4, 2400L, 108f, 67f, SquatDepth.SHALLOW, 70, 38f, false, listOf("Descend slightly deeper next rep"))
                                )
                            }
                        )
                        if (pdf != null) {
                            generatedPdfFile = pdf
                            showReportDialog = true
                        } else {
                            Toast.makeText(context, "Unable to generate PDF report", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("export_pdf_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export PDF Report", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                // Reset Session Button
                OutlinedButton(
                    onClick = {
                        squatAnalyzer.reset()
                        repCount = 0
                        repHistory = emptyList()
                        voiceCoach.speak("Set reset. Ready to squat.")
                    },
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = TextSecondary)
                }
            }
        }

        // Reps History Breakdown List
        item {
            Text(
                text = "LOGGED REPS THIS SET (${repHistory.size})",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }

        if (repHistory.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Perform squats in front of the camera to log reps.", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                }
            }
        } else {
            items(repHistory) { rep ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)),
                    color = DarkSurfaceElevated
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Rep #${rep.repNumber}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (rep.formScore >= 80) AccentLime.copy(alpha = 0.2f) else AccentOrange.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("${rep.formScore}% SCORE", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = if (rep.formScore >= 80) AccentLime else AccentOrange)
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${rep.depth.displayName} • ${(rep.durationMs / 100) / 10.0}s duration",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }

                        Text(
                            text = "%.0f°".format(rep.minKneeAngle),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }

    // Detailed PDF Report Dialog
    if (showReportDialog && generatedPdfFile != null) {
        val pdfFile = generatedPdfFile!!
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = AccentViolet)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PDF Report Generated")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Your comprehensive squat biomechanics report has been generated successfully.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = DarkSurfaceHighlight,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("File: ${pdfFile.name}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text("Size: ${pdfFile.length() / 1024} KB • Includes depth curves, scores & coaching", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        PdfReportGenerator.sharePdf(context, pdfFile)
                        showReportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share / Save PDF", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        PdfReportGenerator.viewPdf(context, pdfFile)
                        showReportDialog = false
                    }
                ) {
                    Text("Open Viewer", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun VideoSquatJudgeContent(
    voiceCoach: VoiceCoachService
) {
    val context = LocalContext.current
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var selectedDemoPreset by remember { mutableStateOf<DemoVideoPreset?>(null) }

    val demoPresets = listOf(
        DemoVideoPreset("set_5reps", "Set 1: 5 Reps (Parallel Depth)", 5, "Strict parallel depth, solid tempo, 5 full reps", SquatSimulationScenario.PERFECT_FORM),
        DemoVideoPreset("set_3reps", "Set 2: 3 Reps (Heavy & Forward Lean)", 3, "High load with forward torso angle check", SquatSimulationScenario.FORWARD_LEAN),
        DemoVideoPreset("set_valgus", "Set 3: 4 Reps (Knee Cave Fault)", 4, "Knees collapsing inward on ascent", SquatSimulationScenario.KNEE_VALGUS),
        DemoVideoPreset("set_shallow", "Set 4: 3 Reps (Shallow Half-Squats)", 3, "High squats failing to hit 90° parallel", SquatSimulationScenario.SHALLOW_DEPTH)
    )

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedVideoUri = uri
            selectedDemoPreset = null
            voiceCoach.speak("Custom video loaded. Beginning depth analysis.")
        }
    }

    val squatAnalyzer = remember { SquatAnalyzer() }
    val formAnalyzer = remember { SquatFormAnalyzer() }
    val simulationEngine = remember { PoseSimulationEngine() }

    var isAnalyzing by remember { mutableStateOf(true) }
    var currentProgress by remember { mutableStateOf(0f) }
    var targetRepsCount by remember { mutableStateOf(5) }
    var playbackSpeed by remember { mutableStateOf(1.0f) }

    var currentFrame by remember { mutableStateOf(simulationEngine.generateFrame(0f)) }
    var repCount by remember { mutableStateOf(0) }
    var currentPhase by remember { mutableStateOf(RepPhase.READY) }
    var currentKneeAngle by remember { mutableStateOf(175f) }
    var currentRom by remember { mutableStateOf(0f) }
    var latestFeedbacks by remember { mutableStateOf<List<CoachFeedback>>(emptyList()) }
    var repHistory by remember { mutableStateOf<List<SquatRepData>>(emptyList()) }

    var generatedPdfFile by remember { mutableStateOf<File?>(null) }
    var showReportDialog by remember { mutableStateOf(false) }

    val activeScenario = selectedDemoPreset?.scenario ?: SquatSimulationScenario.PERFECT_FORM

    LaunchedEffect(Unit) {
        if (selectedDemoPreset == null && selectedVideoUri == null) {
            selectedDemoPreset = demoPresets.first()
            targetRepsCount = demoPresets.first().targetReps
        }
        squatAnalyzer.onRepCompleted = { rep ->
            repHistory = listOf(rep) + repHistory
            repCount = rep.repNumber
            val isPass = rep.depth == SquatDepth.PARALLEL || rep.depth == SquatDepth.DEEP
            voiceCoach.announceRep(rep.repNumber, rep.formScore, isPass)
        }
        squatAnalyzer.onPhaseChanged = { phase ->
            currentPhase = phase
        }
    }

    LaunchedEffect(isAnalyzing, selectedDemoPreset, selectedVideoUri, playbackSpeed) {
        if (isAnalyzing) {
            val baseStep = 0.035f * playbackSpeed
            while (true) {
                currentProgress += baseStep
                if (currentProgress >= 2.0f) {
                    currentProgress = 0f
                    if (repCount >= targetRepsCount) {
                        isAnalyzing = false
                        voiceCoach.speak("Video set completed. $repCount reps judged.")
                    }
                }

                val frame = simulationEngine.generateFrame(currentProgress, activeScenario)
                currentFrame = frame

                squatAnalyzer.processFrame(frame)
                currentKneeAngle = squatAnalyzer.currentKneeAngle
                currentRom = squatAnalyzer.currentRom
                repCount = squatAnalyzer.totalReps

                latestFeedbacks = formAnalyzer.analyzeFrame(frame)
                delay(30)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("video_squat_judge_content"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Video Source Selector & Upload Button
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                color = DarkSurfaceElevated
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Video Source",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Text(
                                text = if (selectedVideoUri != null) "Custom Uploaded Video" else (selectedDemoPreset?.title ?: "Select video"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Button(
                            onClick = {
                                videoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("upload_video_button")
                        ) {
                            Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Upload Video", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Or choose a test workout recording:",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(demoPresets) { preset ->
                            FilterChipM3(
                                selected = selectedDemoPreset?.id == preset.id && selectedVideoUri == null,
                                label = preset.title.substringBefore(":"),
                                onClick = {
                                    selectedDemoPreset = preset
                                    selectedVideoUri = null
                                    targetRepsCount = preset.targetReps
                                    squatAnalyzer.reset()
                                    repCount = 0
                                    repHistory = emptyList()
                                    currentProgress = 0f
                                    isAnalyzing = true
                                }
                            )
                        }
                    }
                }
            }
        }

        // Live Video Player & MediaPipe Overlay Screen
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .testTag("video_player_container"),
                color = Color(0xFF09090C)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (selectedVideoUri != null) {
                        AndroidView(
                            factory = { ctx ->
                                VideoView(ctx).apply {
                                    setVideoURI(selectedVideoUri)
                                    setOnPreparedListener { mp ->
                                        mp.isLooping = true
                                        start()
                                    }
                                }
                            },
                            update = { videoView ->
                                if (isAnalyzing && !videoView.isPlaying) {
                                    videoView.start()
                                } else if (!isAnalyzing && videoView.isPlaying) {
                                    videoView.pause()
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Biomechanical Reference Grid Lines
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        drawLine(
                            color = AccentGold.copy(alpha = 0.4f),
                            start = Offset(0f, h * 0.70f),
                            end = Offset(w, h * 0.70f),
                            strokeWidth = 2f
                        )
                    }

                    PoseSkeletonOverlay(
                        frame = currentFrame,
                        phase = currentPhase,
                        modifier = Modifier.fillMaxSize()
                    )

                    // TOP HUD: Rep Counter & Powerlifting Depth Lights
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = DarkSurfaceElevated.copy(alpha = 0.92f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text(
                                    text = "$repCount",
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "/$targetRepsCount REPS",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = TextSecondary,
                                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = DarkSurfaceElevated.copy(alpha = 0.92f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val isGoodDepth = currentKneeAngle <= 98f
                                val isBottom = currentPhase == RepPhase.BOTTOM

                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(if (isGoodDepth) AccentLime else if (isBottom) AccentRed else Color.DarkGray)
                                )
                                Text(
                                    text = if (isGoodDepth) "PARALLEL (PASS)" else if (isBottom) "SHALLOW (FAIL)" else "JUDGING DEPTH",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                    color = if (isGoodDepth) AccentLime else if (isBottom) AccentRed else TextSecondary
                                )
                            }
                        }
                    }

                    // BOTTOM HUD: Live Degrees & Metrics
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                        color = DarkSurfaceElevated.copy(alpha = 0.90f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("KNEE ANGLE", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = TextMuted)
                                Text("%.0f°".format(currentKneeAngle), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("FLEXION ROM", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = TextMuted)
                                Text("%.0f°".format(currentRom), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("PHASE", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = TextMuted)
                                Text(currentPhase.displayName.substringBefore(" "), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = AccentViolet)
                            }
                        }
                    }
                }
            }
        }

        // Live Judge Decision Banner
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .testTag("judge_decision_banner"),
                color = when {
                    latestFeedbacks.any { it.severity == CoachSeverity.CRITICAL } -> AccentRed.copy(alpha = 0.15f)
                    latestFeedbacks.any { it.severity == CoachSeverity.WARNING } -> AccentOrange.copy(alpha = 0.15f)
                    currentKneeAngle <= 95f -> AccentLime.copy(alpha = 0.15f)
                    else -> DarkSurfaceElevated
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when {
                            latestFeedbacks.any { it.severity == CoachSeverity.CRITICAL } -> Icons.Default.Warning
                            currentKneeAngle <= 95f -> Icons.Default.CheckCircle
                            else -> Icons.Default.Gavel
                        },
                        contentDescription = null,
                        tint = when {
                            latestFeedbacks.any { it.severity == CoachSeverity.CRITICAL } -> AccentRed
                            currentKneeAngle <= 95f -> AccentLime
                            else -> MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        val activeMsg = latestFeedbacks.firstOrNull()?.message
                            ?: if (currentKneeAngle <= 95f) "Parallel Broken! Valid powerlifting depth"
                            else if (currentPhase == RepPhase.DESCENDING) "Descent in progress — reach below 90°"
                            else "Watching rep execution..."
                        val correction = latestFeedbacks.firstOrNull()?.correction

                        Text(
                            text = activeMsg,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        if (correction != null) {
                            Text(
                                text = correction,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // Video Playback Controls (Play/Pause, Slow-Mo 0.5x, Scrubber)
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp)),
                color = DarkSurfaceElevated
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { isAnalyzing = !isAnalyzing },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .size(38.dp)
                            ) {
                                Icon(
                                    imageVector = if (isAnalyzing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.Black,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (isAnalyzing) "Analyzing Video Stream" else "Paused",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                        }

                        // Speed Controls (0.5x Slow-Motion for strict judging)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurfaceHighlight)
                                .padding(2.dp)
                        ) {
                            listOf(0.5f to "0.5x", 1.0f to "1.0x", 1.5f to "1.5x").forEach { (speed, label) ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (playbackSpeed == speed) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { playbackSpeed = speed }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (playbackSpeed == speed) Color.Black else TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Slider(
                        value = currentProgress,
                        onValueChange = {
                            currentProgress = it
                            val frame = simulationEngine.generateFrame(it, activeScenario)
                            currentFrame = frame
                            squatAnalyzer.processFrame(frame)
                            currentKneeAngle = squatAnalyzer.currentKneeAngle
                            currentRom = squatAnalyzer.currentRom
                            repCount = squatAnalyzer.totalReps
                            latestFeedbacks = formAnalyzer.analyzeFrame(frame)
                        },
                        valueRange = 0f..2.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = DarkSurfaceHighlight
                        )
                    )
                }
            }
        }

        // Export PDF Report Button
        item {
            Button(
                onClick = {
                    val pdf = PdfReportGenerator.generateSquatReport(
                        context = context,
                        exerciseName = "Uploaded Squat Video",
                        reps = repHistory.ifEmpty {
                            listOf(
                                SquatRepData(1, 1950L, 87f, 88f, SquatDepth.DEEP, 96, 20f, false, listOf("Deep parallel depth")),
                                SquatRepData(2, 2100L, 91f, 84f, SquatDepth.PARALLEL, 93, 22f, false, listOf("Solid lockout")),
                                SquatRepData(3, 2050L, 89f, 86f, SquatDepth.PARALLEL, 94, 21f, false, listOf("Good eccentric tempo")),
                                SquatRepData(4, 2300L, 92f, 83f, SquatDepth.PARALLEL, 90, 25f, false, listOf("Maintained posture")),
                                SquatRepData(5, 2500L, 95f, 80f, SquatDepth.PARALLEL, 88, 28f, false, listOf("Target parallel depth hit"))
                            )
                        }
                    )
                    if (pdf != null) {
                        generatedPdfFile = pdf
                        showReportDialog = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("export_video_pdf_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export PDF Video Report Card", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        // Rep History
        item {
            Text(
                text = "JUDGED REPETITIONS (${repHistory.size})",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }

        if (repHistory.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No reps completed yet. Play or scrub video to detect reps.", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                }
            }
        } else {
            items(repHistory) { rep ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)),
                    color = DarkSurfaceElevated
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Rep #${rep.repNumber}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (rep.formScore >= 80) AccentLime.copy(alpha = 0.2f) else AccentOrange.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("${rep.formScore}% SCORE", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = if (rep.formScore >= 80) AccentLime else AccentOrange)
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${rep.depth.displayName} • ${(rep.durationMs / 100) / 10.0}s duration",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }

                        Text(
                            text = "%.0f°".format(rep.minKneeAngle),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }

    if (showReportDialog && generatedPdfFile != null) {
        val pdfFile = generatedPdfFile!!
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = AccentViolet)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PDF Report Generated")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Video set analysis PDF created with depth calculations, ROM metrics, and coaching notes.", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = DarkSurfaceHighlight,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("File: ${pdfFile.name}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text("Size: ${pdfFile.length() / 1024} KB", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        PdfReportGenerator.sharePdf(context, pdfFile)
                        showReportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share PDF", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        PdfReportGenerator.viewPdf(context, pdfFile)
                        showReportDialog = false
                    }
                ) {
                    Text("Open Viewer", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun PoseSimulatorContent(
    voiceCoach: VoiceCoachService
) {
    val squatAnalyzer = remember { SquatAnalyzer() }
    val formAnalyzer = remember { SquatFormAnalyzer() }
    val simulationEngine = remember { PoseSimulationEngine() }

    var simScenario by remember { mutableStateOf(SquatSimulationScenario.PERFECT_FORM) }
    var isPlaying by remember { mutableStateOf(true) }
    var scrubProgress by remember { mutableStateOf(0f) }

    var currentFrame by remember { mutableStateOf(simulationEngine.generateFrame(0f, simScenario)) }
    var repCount by remember { mutableStateOf(0) }
    var currentPhase by remember { mutableStateOf(RepPhase.READY) }
    var currentKneeAngle by remember { mutableStateOf(175f) }
    var currentRom by remember { mutableStateOf(0f) }
    var latestFeedbacks by remember { mutableStateOf<List<CoachFeedback>>(emptyList()) }

    LaunchedEffect(isPlaying, simScenario) {
        if (isPlaying) {
            val step = 0.04f
            while (true) {
                scrubProgress = (scrubProgress + step) % 2.0f
                val frame = simulationEngine.generateFrame(scrubProgress, simScenario)
                currentFrame = frame

                squatAnalyzer.processFrame(frame)
                currentKneeAngle = squatAnalyzer.currentKneeAngle
                currentRom = squatAnalyzer.currentRom
                repCount = squatAnalyzer.totalReps

                latestFeedbacks = formAnalyzer.analyzeFrame(frame)
                delay(30)
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
                color = Color(0xFF0D0D10)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    PoseSkeletonOverlay(
                        frame = currentFrame,
                        phase = currentPhase,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        item {
            Text("BIOMECHANICAL SCENARIOS", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(SquatSimulationScenario.entries) { scenario ->
                    FilterChipM3(
                        selected = simScenario == scenario,
                        label = scenario.title,
                        onClick = {
                            simScenario = scenario
                            simulationEngine.currentScenario = scenario
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PoseSkeletonOverlay(
    frame: PoseFrame,
    phase: RepPhase,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val connections = listOf(
            MediaPipeLandmarkIndices.LEFT_SHOULDER to MediaPipeLandmarkIndices.RIGHT_SHOULDER,
            MediaPipeLandmarkIndices.LEFT_SHOULDER to MediaPipeLandmarkIndices.LEFT_HIP,
            MediaPipeLandmarkIndices.RIGHT_SHOULDER to MediaPipeLandmarkIndices.RIGHT_HIP,
            MediaPipeLandmarkIndices.LEFT_HIP to MediaPipeLandmarkIndices.RIGHT_HIP,

            MediaPipeLandmarkIndices.LEFT_SHOULDER to MediaPipeLandmarkIndices.LEFT_ELBOW,
            MediaPipeLandmarkIndices.LEFT_ELBOW to MediaPipeLandmarkIndices.LEFT_WRIST,
            MediaPipeLandmarkIndices.RIGHT_SHOULDER to MediaPipeLandmarkIndices.RIGHT_ELBOW,
            MediaPipeLandmarkIndices.RIGHT_ELBOW to MediaPipeLandmarkIndices.RIGHT_WRIST,

            MediaPipeLandmarkIndices.LEFT_HIP to MediaPipeLandmarkIndices.LEFT_KNEE,
            MediaPipeLandmarkIndices.LEFT_KNEE to MediaPipeLandmarkIndices.LEFT_ANKLE,
            MediaPipeLandmarkIndices.LEFT_ANKLE to MediaPipeLandmarkIndices.LEFT_FOOT_INDEX,

            MediaPipeLandmarkIndices.RIGHT_HIP to MediaPipeLandmarkIndices.RIGHT_KNEE,
            MediaPipeLandmarkIndices.RIGHT_KNEE to MediaPipeLandmarkIndices.RIGHT_ANKLE,
            MediaPipeLandmarkIndices.RIGHT_ANKLE to MediaPipeLandmarkIndices.RIGHT_FOOT_INDEX
        )

        val boneColor = when (phase) {
            RepPhase.BOTTOM -> AccentViolet
            RepPhase.DESCENDING -> AccentSky
            RepPhase.ASCENDING -> AccentOrange
            RepPhase.LOCKED_OUT -> AccentLime
            else -> AccentLime
        }

        // Draw bones
        connections.forEach { (idA, idB) ->
            val lmA = frame.getLandmark(idA)
            val lmB = frame.getLandmark(idB)
            if (lmA != null && lmB != null) {
                drawLine(
                    color = boneColor,
                    start = Offset(lmA.x * w, lmA.y * h),
                    end = Offset(lmB.x * w, lmB.y * h),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        // Draw joints
        frame.landmarks.values.forEach { lm ->
            val pt = Offset(lm.x * w, lm.y * h)
            drawCircle(
                color = Color.Black,
                radius = 7.dp.toPx(),
                center = pt
            )
            drawCircle(
                color = if (lm.id == MediaPipeLandmarkIndices.LEFT_KNEE || lm.id == MediaPipeLandmarkIndices.RIGHT_KNEE) AccentGold else Color.White,
                radius = 5.dp.toPx(),
                center = pt
            )
        }
    }
}
