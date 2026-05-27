package com.example

import android.os.Bundle
import android.os.Vibrator
import android.os.VibrationEffect
import android.os.Build
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.db.HistoryItem
import kotlin.math.*
import java.util.Locale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        setContent {
            MyApplicationTheme {
                CalculatorScreen(vibrator = vibrator)
            }
        }
    }
}

// Complete set of key layouts
sealed class CalcKey {
    data class Digit(val value: String) : CalcKey()
    data class Operator(val value: String) : CalcKey()
    data class Scientific(val value: String, val label: String) : CalcKey()
    object Clear : CalcKey()
    object Backspace : CalcKey()
    object Equal : CalcKey()
    object Negate : CalcKey()
    object Percent : CalcKey()
    data class Parenthesis(val value: String) : CalcKey()
}

// Material 3 compliant high-fidelity palette architectures
data class CalcTheme(
    val id: String,
    val name: String,
    val isDark: Boolean,
    val backgroundColor: Color,
    val darkSurfaceColor: Color,
    val operatorColor: Color,
    val actionColor: Color,
    val digitColor: Color,
    val clearColor: Color,
    val clearTextColor: Color,
    val operatorTextColor: Color,
    val expressionTextColor: Color,
    val previewResultColor: Color,
    val functionalTextColor: Color,
    val labelColor: Color = if (isDark) Color.White else Color(0xFF0F172A)
)

val ThemesList = listOf(
    CalcTheme(
        id = "prism_ice_blue",
        name = "Prism Ice Blue",
        isDark = false,
        backgroundColor = Color(0xFFEDF2F8),
        darkSurfaceColor = Color(0xFFD3E0EA),
        operatorColor = Color(0xFF1E40AF),
        actionColor = Color(0xFFE0F2FE),
        digitColor = Color(0xFFFFFFFF),
        clearColor = Color(0xFFFCA5A5),
        clearTextColor = Color(0xFF991B1B),
        operatorTextColor = Color(0xFFFFFFFF),
        expressionTextColor = Color(0xFF0F172A),
        previewResultColor = Color(0xFF475569),
        functionalTextColor = Color(0xFF1D4ED8)
    ),
    CalcTheme(
        id = "midnight_forest",
        name = "Midnight Forest",
        isDark = true,
        backgroundColor = Color(0xFF0C1315),
        darkSurfaceColor = Color(0xFF142422),
        operatorColor = Color(0xFF0D9488),
        actionColor = Color(0xFF11433E),
        digitColor = Color(0xFF131D1B),
        clearColor = Color(0xFFE11D48),
        clearTextColor = Color(0xFFFFFFFF),
        operatorTextColor = Color(0xFFFFFFFF),
        expressionTextColor = Color(0xFFCCFBF1),
        previewResultColor = Color(0xFF99F6E4),
        functionalTextColor = Color(0xFF2DD4BF)
    ),
    CalcTheme(
        id = "crimson_slate",
        name = "Crimson Slate",
        isDark = true,
        backgroundColor = Color(0xFF0F1117),
        darkSurfaceColor = Color(0xFF1E2430),
        operatorColor = Color(0xFF991B1B),
        actionColor = Color(0xFF551919),
        digitColor = Color(0xFF171F2C),
        clearColor = Color(0xFFE11D48),
        clearTextColor = Color(0xFFFFFFFF),
        operatorTextColor = Color(0xFFFFFFFF),
        expressionTextColor = Color(0xFFFEE2E2),
        previewResultColor = Color(0xFFFDA4AF),
        functionalTextColor = Color(0xFFF43F5E)
    )
)

@Composable
fun CalculatorScreen(viewModel: CalculatorViewModel = viewModel(), vibrator: Vibrator? = null) {
    val expr by viewModel.expression.collectAsState()
    val preview by viewModel.previewResult.collectAsState()
    val error by viewModel.errorMessage.collectAsState()
    val isScientificExpanded by viewModel.isScientificExpanded.collectAsState()
    val angleMode by viewModel.angleMode.collectAsState()
    val baseMode by viewModel.baseMode.collectAsState()
    val variables by viewModel.variables.collectAsState()
    val historyList by viewModel.historyState.collectAsState()
    val isHighPrecision by viewModel.isHighPrecision.collectAsState()

    var showHistory by remember { mutableStateOf(false) }
    var showNavMenu by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf("TRIG") }
    
    // Dynamic Theme state
    var currentTheme by remember { mutableStateOf(ThemesList[0]) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var showGrapherDialog by remember { mutableStateOf(false) }

    // Dialog/Form triggers
    var showSolverDialog by remember { mutableStateOf<String?>(null) } // "QUAD", "SYSTEM"
    var showConversionDialog by remember { mutableStateOf(false) }
    var showVariableStoreDialog by remember { mutableStateOf<String?>(null) } // Variable name selected
    var showWorksheetDialog by remember { mutableStateOf<String?>(null) }

    val haptic = LocalHapticFeedback.current

    // Precision active themes mappings
    val backgroundColor = currentTheme.backgroundColor
    val darkSurfaceColor = currentTheme.darkSurfaceColor
    val operatorColor = currentTheme.operatorColor
    val actionColor = currentTheme.actionColor
    val digitColor = currentTheme.digitColor
    val clearColor = currentTheme.clearColor

    val clearTextColor = currentTheme.clearTextColor
    val operatorTextColor = currentTheme.operatorTextColor
    val expressionTextColor = currentTheme.expressionTextColor
    val previewResultColor = currentTheme.previewResultColor
    val functionalTextColor = currentTheme.functionalTextColor
    val labelColor = currentTheme.labelColor

    // Custom Weighted Vibrotactile Feedback Loop
    val triggerVibe = { keyType: CalcKey ->
        if (vibrator != null) {
            try {
                val duration: Long
                val amplitude: Int
                when (keyType) {
                    is CalcKey.Digit, is CalcKey.Parenthesis -> {
                        duration = 10
                        amplitude = 40 // subtle light digit click
                    }
                    is CalcKey.Operator -> {
                        duration = 20
                        amplitude = 110 // firm operator snap
                    }
                    is CalcKey.Equal -> {
                        duration = 35
                        amplitude = 170 // crisp action success snap
                    }
                    else -> {
                        duration = 22
                        amplitude = 135 // moderate action press
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(duration, amplitude))
                } else {
                    vibrator.vibrate(duration)
                }
            } catch (e: Exception) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        } else {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Mode Banner (DEG status, base status, scientific triggers)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dynamic Category Navigation Menu trigger button
                IconButton(
                    onClick = {
                        triggerVibe(CalcKey.Digit("0"))
                        showNavMenu = true
                    },
                    modifier = Modifier
                        .background(actionColor, RoundedCornerShape(12.dp))
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Dynamic Navigation Menu",
                        tint = functionalTextColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Beautiful, pill-shaped mock Search Bar row
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .background(actionColor, RoundedCornerShape(12.dp))
                        .clickable {
                            triggerVibe(CalcKey.Digit("0"))
                            showSearchDialog = true
                        }
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search functions and worksheets",
                        tint = functionalTextColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Search tools...",
                        color = functionalTextColor.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    // Visual indicator / chip for quick search shortcut
                    Box(
                        modifier = Modifier
                            .background(functionalTextColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "FAST",
                            color = functionalTextColor,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Interactive 2D Plotter trigger
                IconButton(
                    onClick = {
                        triggerVibe(CalcKey.Digit("0"))
                        showGrapherDialog = true
                    },
                    modifier = Modifier
                        .background(actionColor, RoundedCornerShape(12.dp))
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "2D Function Plotter",
                        tint = functionalTextColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Settings button replaces Basic Palette Dialog Button
                IconButton(
                    onClick = {
                        triggerVibe(CalcKey.Digit("0"))
                        showSettingsDialog = true
                    },
                    modifier = Modifier
                        .background(actionColor, RoundedCornerShape(12.dp))
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings and Themes",
                        tint = functionalTextColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // COMPANION HIST Sidebar
                IconButton(
                    onClick = {
                        triggerVibe(CalcKey.Digit("0"))
                        showHistory = true
                    },
                    modifier = Modifier
                        .background(actionColor, RoundedCornerShape(12.dp))
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Companion Console",
                        tint = functionalTextColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Real-time scientific indicator status & Precision Mode Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "VARS: [x=${String.format(Locale.US, "%.1f", variables["x"] ?: 0.0)}, y=${String.format(Locale.US, "%.1f", variables["y"] ?: 0.0)}]",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = previewResultColor.copy(alpha = 0.8f)
                )

                // High Precision decimal engine Toggle Switch
                Button(
                    onClick = {
                        triggerVibe(CalcKey.Digit("0"))
                        viewModel.toggleHighPrecision()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isHighPrecision) operatorColor else actionColor
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(
                        text = if (isHighPrecision) "DEC Mode: High Prec" else "DEC Mode: Float64",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isHighPrecision) operatorTextColor else functionalTextColor
                    )
                }
            }

            // Display expression panel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.0f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (currentTheme.isDark) Color(0xFF131920) else Color.White)
                    .border(1.5.dp, if (currentTheme.isDark) Color(0xFF374151) else Color(0xFFBFDBFE), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.End
                ) {
                    if (error != null) {
                        Text(
                            text = error ?: "Format Error",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444),
                            textAlign = TextAlign.End
                        )
                    } else if (preview.isNotEmpty()) {
                        val previewScrollState = rememberScrollState()
                        LaunchedEffect(preview) {
                            previewScrollState.scrollTo(previewScrollState.maxValue)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(previewScrollState),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = preview,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Light,
                                color = previewResultColor,
                                textAlign = TextAlign.End,
                                maxLines = 1
                            )
                        }
                    } else {
                        Text(
                            text = "",
                            fontSize = 24.sp,
                            color = Color.Transparent
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    val scrollState = rememberScrollState()
                    LaunchedEffect(expr) {
                        scrollState.scrollTo(scrollState.maxValue)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = expr.ifEmpty { "0" },
                            fontSize = if (expr.length > 8) 36.sp else 50.sp,
                            fontWeight = FontWeight.Light,
                            color = expressionTextColor,
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            letterSpacing = (-0.5).sp
                        )
                    }
                }
            }



            // High aesthetic permanent keypad
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(3.5f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val unifiedGrid = listOf(
                    // Scientific Row 1
                    listOf(
                        CalcKey.Scientific("sin(", "sin"),
                        CalcKey.Scientific("cos(", "cos"),
                        CalcKey.Scientific("tan(", "tan"),
                        CalcKey.Scientific("^", "^")
                    ),
                    // Scientific Row 2
                    listOf(
                        CalcKey.Scientific("sqrt(", "√"),
                        CalcKey.Scientific("log(", "log"),
                        CalcKey.Scientific("ln(", "ln"),
                        CalcKey.Scientific("π", "π")
                    ),
                    // Scientific Row 3
                    listOf(
                        CalcKey.Scientific("e", "e"),
                        CalcKey.Parenthesis("("),
                        CalcKey.Parenthesis(")"),
                        CalcKey.Scientific("!", "x!")
                    ),
                    // Standard Row 1
                    listOf(CalcKey.Clear, CalcKey.Backspace, CalcKey.Percent, CalcKey.Operator("÷")),
                    // Standard Row 2
                    listOf(CalcKey.Digit("7"), CalcKey.Digit("8"), CalcKey.Digit("9"), CalcKey.Operator("×")),
                    // Standard Row 3
                    listOf(CalcKey.Digit("4"), CalcKey.Digit("5"), CalcKey.Digit("6"), CalcKey.Operator("-")),
                    // Standard Row 4
                    listOf(CalcKey.Digit("1"), CalcKey.Digit("2"), CalcKey.Digit("3"), CalcKey.Operator("+")),
                    // Standard Row 5
                    listOf(CalcKey.Digit("0"), CalcKey.Digit("."), CalcKey.Negate, CalcKey.Equal)
                )

                unifiedGrid.forEach { rowKeys ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowKeys.forEach { key ->
                            val currentBgColor = when (key) {
                                is CalcKey.Digit -> digitColor
                                is CalcKey.Clear -> clearColor
                                is CalcKey.Operator, is CalcKey.Equal -> operatorColor
                                else -> actionColor
                            }

                            val currentTextColor = when (key) {
                                is CalcKey.Digit -> expressionTextColor
                                is CalcKey.Clear -> clearTextColor
                                is CalcKey.Operator, is CalcKey.Equal -> operatorTextColor
                                else -> functionalTextColor
                            }

                            KeypadButton(
                                key = key,
                                bgColor = currentBgColor,
                                textColor = currentTextColor,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                onClick = {
                                    triggerVibe(key)
                                    when (key) {
                                        is CalcKey.Digit -> viewModel.onDigitClick(key.value)
                                        is CalcKey.Operator -> viewModel.onOperatorClick(key.value)
                                        is CalcKey.Clear -> viewModel.onClearClick()
                                        is CalcKey.Backspace -> viewModel.onBackspaceClick()
                                        is CalcKey.Percent -> viewModel.onDigitClick("%")
                                        is CalcKey.Parenthesis -> viewModel.onDigitClick(key.value)
                                        is CalcKey.Equal -> viewModel.onEqualClick()
                                        is CalcKey.Negate -> viewModel.onNegateClick()
                                        is CalcKey.Scientific -> {
                                            if (key.value == "π" || key.value == "e") {
                                                viewModel.onConstantClick(key.value)
                                            } else if (key.value == "^") {
                                                viewModel.onOperatorClick(key.value)
                                            } else {
                                                viewModel.onFunctionClick(key.value)
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Sliding Left Dynamic Navigation Menu Drawer
        AnimatedVisibility(
            visible = showNavMenu,
            enter = slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
            ) + fadeIn(),
            exit = slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
            ) + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showNavMenu = false }
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .fillMaxHeight()
                        .align(Alignment.CenterStart)
                        .clickable(enabled = false) {},
                    shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = darkSurfaceColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Title header for drawer
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = null,
                                    tint = expressionTextColor,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "IRA05-26 Tool Suite",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = expressionTextColor
                                )
                            }
                            IconButton(onClick = { showNavMenu = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close Menu", tint = expressionTextColor)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = expressionTextColor.copy(alpha = 0.12f), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Scrollable navigation groups
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Section 1: Advanced Full Screen Engineering Worksheets
                            Text(
                                "Advanced Worksheets",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = previewResultColor.copy(alpha = 0.7f),
                                letterSpacing = 1.sp
                            )
                            val wsList = listOf(
                                "calculus" to "Calculus & Limits",
                                "linalg" to "Linear Algebra Studio (3x3)",
                                "complex" to "Complex Numbers & DeMoivre",
                                "regression" to "Linear Regression Studio",
                                "special" to "Special Functions & Stats",
                                "finance" to "Financial TVM Ledger",
                                "cas" to "Symbolic CAS (Factor/Expand)",
                                "fraction" to "Fraction & Decimal converter"
                            )
                            wsList.forEach { (typeKey, label) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(actionColor)
                                        .clickable {
                                            triggerVibe(CalcKey.Digit("0"))
                                            showWorksheetDialog = typeKey
                                            showNavMenu = false
                                        }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.List,
                                        contentDescription = null,
                                        tint = functionalTextColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = label,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = expressionTextColor
                                    )
                                }
                            }

                            // Section 2: Equations Solvers & Conversions
                            Text(
                                "Solvers, Plotters & Conversions",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = previewResultColor.copy(alpha = 0.7f),
                                letterSpacing = 1.sp
                            )
                            val solverList = listOf(
                                "QUAD" to "ax² + bx + c = 0 Quadratic Solver",
                                "SYSTEM" to "2x2 Linear Equations System"
                            )
                            solverList.forEach { (typeKey, label) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(actionColor)
                                        .clickable {
                                            triggerVibe(CalcKey.Digit("0"))
                                            showSolverDialog = typeKey
                                            showNavMenu = false
                                        }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = functionalTextColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = label,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = expressionTextColor
                                    )
                                }
                            }

                            // Converters
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(actionColor)
                                    .clickable {
                                        triggerVibe(CalcKey.Digit("0"))
                                        showConversionDialog = true
                                        showNavMenu = false
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = functionalTextColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Scientific Unit Converter",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = expressionTextColor
                                )
                            }

                            // Plotter
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(actionColor)
                                    .clickable {
                                        triggerVibe(CalcKey.Digit("0"))
                                        showGrapherDialog = true
                                        showNavMenu = false
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = functionalTextColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "2D Canvas Function Grapher",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = expressionTextColor
                                )
                            }

                            // Section 3: High Advanced Math Functions Quick Injectors
                            Text(
                                "Advanced Math Function Injectors",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = previewResultColor.copy(alpha = 0.7f),
                                letterSpacing = 1.sp
                            )
                            
                            // Trig subsection
                            Text("Trigonometry & Hyperbolic", fontSize = 11.sp, color = previewResultColor.copy(alpha = 0.5f))
                            val trigFuncs = listOf(
                                "sin(" to "sin", "cos(" to "cos", "tan(" to "tan",
                                "asin(" to "sin⁻¹", "acos(" to "cos⁻¹", "atan(" to "tan⁻¹",
                                "sinh(" to "sinh", "cosh(" to "cosh", "tanh(" to "tanh"
                            )
                            trigFuncs.chunked(3).forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    rowItems.forEach { item ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(actionColor)
                                                .clickable {
                                                    triggerVibe(CalcKey.Digit("0"))
                                                    viewModel.onFunctionClick(item.first)
                                                    showNavMenu = false
                                                }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(item.second, color = functionalTextColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            // Math operators subsection
                            Text("General Math Operators", fontSize = 11.sp, color = previewResultColor.copy(alpha = 0.5f))
                            val mathFuncs = listOf(
                                "abs(" to "abs", "gcd(" to "gcd", "lcm(" to "lcm",
                                "mod(" to "mod", "round(" to "round", "iPart(" to "iPart",
                                "fPart(" to "fPart", "int(" to "int", "min(" to "min", "max(" to "max",
                                "nCr" to "nCr", "nPr" to "nPr"
                            )
                            mathFuncs.chunked(3).forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    rowItems.forEach { item ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(actionColor)
                                                .clickable {
                                                    triggerVibe(CalcKey.Digit("0"))
                                                    if (item.first == "nCr" || item.first == "nPr") {
                                                        viewModel.onOperatorClick(item.first)
                                                    } else {
                                                        viewModel.onFunctionClick(item.first)
                                                    }
                                                    showNavMenu = false
                                                }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(item.second, color = functionalTextColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            // Scientific constants inject
                            Text("Universal Scientific Constants", fontSize = 11.sp, color = previewResultColor.copy(alpha = 0.5f))
                            val consts = listOf(
                                "299792458" to "c (light)",
                                "9.80665" to "g (gravity)",
                                "6.62607e-34" to "h (Planck)",
                                "6.02214e23" to "NA (Avogadro)",
                                "8.31447" to "R (Gas)",
                                "1.60218e-19" to "e (electro-q)"
                            )
                            consts.chunked(3).forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    rowItems.forEach { item ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(actionColor)
                                                .clickable {
                                                    triggerVibe(CalcKey.Digit("0"))
                                                    viewModel.onConstantClick(item.first)
                                                    showNavMenu = false
                                                }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(item.second, color = functionalTextColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                        }
                                    }
                                }
                            }

                            // Section 4: Memory Storage Manager RCL/STO
                            Text(
                                "Memory & Variables",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = previewResultColor.copy(alpha = 0.7f),
                                letterSpacing = 1.sp
                            )
                            listOf("x", "y", "z", "t", "a", "b", "c", "d").forEach { variable ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Var $variable",
                                        color = expressionTextColor,
                                        fontSize = 13.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    
                                    Button(
                                        onClick = {
                                            triggerVibe(CalcKey.Digit("0"))
                                            viewModel.onDigitClick(variable)
                                            showNavMenu = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = actionColor),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(32.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                    ) {
                                        Text("RCL $variable", color = functionalTextColor, fontSize = 11.sp)
                                    }

                                    Button(
                                        onClick = {
                                            triggerVibe(CalcKey.Digit("0"))
                                            showVariableStoreDialog = variable
                                            showNavMenu = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = actionColor),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(32.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                    ) {
                                        Text("STO $variable", color = functionalTextColor, fontSize = 11.sp)
                                    }
                                }
                            }
                            
                            Button(
                                onClick = {
                                    triggerVibe(CalcKey.Digit("0"))
                                    viewModel.clearVariables()
                                    showNavMenu = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Clear Memory Variables", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Sliding Companion Panel Sidebar (History Logs + Variables tab panel)
        AnimatedVisibility(
            visible = showHistory,
            enter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
            ) + fadeIn(),
            exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
            ) + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showHistory = false }
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .fillMaxHeight()
                        .align(Alignment.CenterEnd)
                        .clickable(enabled = false) {},
                    shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = darkSurfaceColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        var sidebarTab by remember { mutableStateOf("HISTORY") } // "HISTORY" or "VARIABLES"

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Companion Panel",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = expressionTextColor
                            )
                            IconButton(onClick = { showHistory = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close Sidebar", tint = expressionTextColor)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Unified tab selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { sidebarTab = "HISTORY" },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (sidebarTab == "HISTORY") operatorColor else actionColor
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    "Logs",
                                    fontWeight = FontWeight.Bold,
                                    color = if (sidebarTab == "HISTORY") operatorTextColor else functionalTextColor
                                )
                            }

                            Button(
                                onClick = { sidebarTab = "VARIABLES" },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (sidebarTab == "VARIABLES") operatorColor else actionColor
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1.1f)
                            ) {
                                Text(
                                    "Variables",
                                    fontWeight = FontWeight.Bold,
                                    color = if (sidebarTab == "VARIABLES") operatorTextColor else functionalTextColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = expressionTextColor.copy(alpha = 0.12f), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(16.dp))

                        if (sidebarTab == "HISTORY") {
                            // History content
                            if (historyList.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1.0f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            tint = previewResultColor,
                                            modifier = Modifier.size(56.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "No calculations recorded yet",
                                            color = previewResultColor,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(historyList) { item ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    triggerVibe(CalcKey.Digit("0"))
                                                    viewModel.selectHistoryItem(item)
                                                    showHistory = false
                                                },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = digitColor)
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalAlignment = Alignment.End
                                            ) {
                                                Text(
                                                    text = item.expression,
                                                    fontSize = 13.sp,
                                                    color = previewResultColor,
                                                    textAlign = TextAlign.End
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "= ${item.result}",
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = expressionTextColor,
                                                    textAlign = TextAlign.End
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { viewModel.clearHistory() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Clear All Logs", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            // Variables tab manager
                            var newVarName by remember { mutableStateOf("") }
                            var newVarValue by remember { mutableStateOf("") }

                            Column(modifier = Modifier.fillMaxSize()) {
                                Text(
                                    "Add Custom Variable",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = expressionTextColor
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = newVarName,
                                        onValueChange = { newVarName = it },
                                        placeholder = { Text("Name", fontSize = 11.sp) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = expressionTextColor,
                                            unfocusedTextColor = expressionTextColor
                                        )
                                    )
                                    OutlinedTextField(
                                        value = newVarValue,
                                        onValueChange = { newVarValue = it },
                                        placeholder = { Text("Value", fontSize = 11.sp) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1.2f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = expressionTextColor,
                                            unfocusedTextColor = expressionTextColor
                                        )
                                    )
                                    IconButton(
                                        onClick = {
                                            if (newVarName.isNotBlank() && newVarValue.isNotBlank()) {
                                                triggerVibe(CalcKey.Equal)
                                                viewModel.storeVariable(newVarName.trim(), newVarValue.trim())
                                                newVarName = ""
                                                newVarValue = ""
                                            }
                                        },
                                        modifier = Modifier
                                            .background(operatorColor, RoundedCornerShape(12.dp))
                                            .size(40.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Add custom variable", tint = Color.White)
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Active Formulas / Variables",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = expressionTextColor
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val currentVars = variables.toList().sortedBy { it.first }
                                    items(currentVars) { (name, value) ->
                                        val isPredefined = name in listOf("x", "y", "z", "t", "a", "b", "c", "d")
                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = digitColor),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = name,
                                                            fontSize = 15.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = operatorColor
                                                        )
                                                        if (!isPredefined) {
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text(
                                                                "[Custom]",
                                                                fontSize = 9.sp,
                                                                color = functionalTextColor
                                                            )
                                                        }
                                                    }
                                                    Text(
                                                        text = "= $value",
                                                        fontSize = 14.sp,
                                                        color = expressionTextColor
                                                    )
                                                }

                                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    // STO click triggers store
                                                    TextButton(
                                                        onClick = {
                                                            triggerVibe(CalcKey.Digit("0"))
                                                            viewModel.storeVariable(name, viewModel.expression.value)
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 4.dp)
                                                    ) {
                                                        Text("STO", fontSize = 11.sp, color = functionalTextColor)
                                                    }

                                                    // RCL click registers symbol to screen
                                                    TextButton(
                                                        onClick = {
                                                            triggerVibe(CalcKey.Digit("0"))
                                                            viewModel.onDigitClick(name)
                                                            showHistory = false
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 4.dp)
                                                    ) {
                                                        Text("RCL", fontSize = 11.sp, color = operatorColor)
                                                    }

                                                    if (!isPredefined) {
                                                        IconButton(
                                                            onClick = {
                                                                triggerVibe(CalcKey.Backspace)
                                                                viewModel.removeVariable(name)
                                                            },
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Icon(
                                                                Icons.Default.Delete,
                                                                contentDescription = "Delete variable",
                                                                tint = Color(0xFFDC2626),
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { viewModel.clearVariables() },
                                    colors = ButtonDefaults.buttonColors(containerColor = actionColor),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Reset Predefined Vars", color = functionalTextColor, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2D Canvas Function Grapher Plotter Modal
        if (showGrapherDialog) {
            FunctionGrapherDialog(
                angleMode = angleMode,
                onDismiss = { showGrapherDialog = false }
            )
        }

        // System Settings & Themes Configuration Dialog
        if (showSettingsDialog) {
            SettingsDialog(
                currentTheme = currentTheme,
                onSelectTheme = { currentTheme = it },
                angleMode = angleMode,
                onToggleAngleMode = { viewModel.toggleAngleMode() },
                isHighPrecision = isHighPrecision,
                onToggleHighPrecision = { viewModel.toggleHighPrecision() },
                onClearVariables = { viewModel.clearVariables() },
                onClearHistory = { viewModel.clearHistory() },
                onDismiss = { showSettingsDialog = false }
            )
        }

        // Global Search Omnibox Dialog Launcher
        if (showSearchDialog) {
            GlobalSearchDialog(
                onDismiss = { showSearchDialog = false },
                onOpenWorksheet = { wsKey ->
                    showWorksheetDialog = wsKey
                    showSearchDialog = false
                },
                onOpenSolver = { solverKey ->
                    showSolverDialog = solverKey
                    showSearchDialog = false
                },
                onOpenConversion = {
                    showConversionDialog = true
                    showSearchDialog = false
                },
                onOpenGrapher = {
                    showGrapherDialog = true
                    showSearchDialog = false
                },
                onInjectFunction = { func ->
                    viewModel.onFunctionClick(func)
                    showSearchDialog = false
                },
                onInjectConstant = { constant ->
                    viewModel.onConstantClick(constant)
                    showSearchDialog = false
                },
                onInjectOperator = { op ->
                    viewModel.onOperatorClick(op)
                    showSearchDialog = false
                }
            )
        }

        // Solver Input Dialog Sheet
        showSolverDialog?.let { type ->
            SolverDialog(
                type = type,
                onDismiss = { showSolverDialog = null },
                onSolveRealRoot = { rootResult ->
                    viewModel.onConstantClick(rootResult)
                    showSolverDialog = null
                }
            )
        }

        // Variable Store Input Dialog Sheet
        showVariableStoreDialog?.let { variableName ->
            VariableStoreDialog(
                variableName = variableName,
                viewModel = viewModel,
                onDismiss = { showVariableStoreDialog = null }
            )
        }

        // Translation/Conversion Dialog Sheet
        if (showConversionDialog) {
            ConversionDialog(
                onDismiss = { showConversionDialog = false },
                onSelectConversionResult = { convertedVal ->
                    viewModel.onConstantClick(convertedVal)
                    showConversionDialog = false
                }
            )
        }

        showWorksheetDialog?.let { type ->
            WorksheetDialog(
                type = type,
                onDismiss = { showWorksheetDialog = null },
                onInsertResult = { res ->
                    viewModel.onConstantClick(res)
                    showWorksheetDialog = null
                }
            )
        }
    }
}

@Composable
fun RibbonButton(theme: CalcTheme, label: String, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Surface(
        modifier = Modifier
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        color = theme.darkSurfaceColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = theme.operatorColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

// Elegant rounded responsive button design
@Composable
fun KeypadButton(
    key: CalcKey,
    bgColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        val label = when (key) {
            is CalcKey.Digit -> key.value
            is CalcKey.Operator -> key.value
            is CalcKey.Scientific -> key.label
            is CalcKey.Clear -> "AC"
            is CalcKey.Backspace -> "⌫"
            is CalcKey.Percent -> "%"
            is CalcKey.Equal -> "="
            is CalcKey.Negate -> "±"
            is CalcKey.Parenthesis -> key.value
        }

        val designFontSize = when (key) {
            is CalcKey.Scientific -> 14.sp
            is CalcKey.Clear, is CalcKey.Backspace, is CalcKey.Negate -> 18.sp
            else -> 22.sp
        }

        val designFontWeight = when (key) {
            is CalcKey.Operator, is CalcKey.Equal -> FontWeight.Bold
            else -> FontWeight.Medium
        }

        Text(
            text = label,
            fontSize = designFontSize,
            fontWeight = designFontWeight,
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SolverDialog(
    type: String,
    onDismiss: () -> Unit,
    onSolveRealRoot: (String) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val dismissWithKeyboard = {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        onDismiss()
    }

    var a by remember { mutableStateOf("1") }
    var b by remember { mutableStateOf("-5") }
    var c by remember { mutableStateOf("6") }
    
    // Systems variables
    var a1 by remember { mutableStateOf("2") }
    var b1 by remember { mutableStateOf("1") }
    var c1 by remember { mutableStateOf("11") }
    var a2 by remember { mutableStateOf("1") }
    var b2 by remember { mutableStateOf("-3") }
    var c2 by remember { mutableStateOf("-5") }

    var resultText by remember { mutableStateOf("") }
    var primaryRoot by remember { mutableStateOf("") }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color(0xFF0F172A),
        unfocusedTextColor = Color(0xFF0F172A),
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        focusedBorderColor = Color(0xFF1E40AF),
        unfocusedBorderColor = Color(0xFF94A3B8)
    )

    Dialog(
        onDismissRequest = dismissWithKeyboard,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF1F5F9)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp)
            ) {
                // Header (App bar)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = dismissWithKeyboard) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF0F172A)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (type == "QUAD") "Quadratic Equation Solver" else "2x2 Linear System Solver",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Scrollable main content container taking full horizontal width!
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (type == "QUAD") {
                        Text("Solves ax² + bx + c = 0", color = Color(0xFF475569), fontSize = 13.sp)
                        
                        OutlinedTextField(
                            value = a, onValueChange = { a = it }, label = { Text("Coefficient a") },
                            colors = fieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = b, onValueChange = { b = it }, label = { Text("Coefficient b") },
                            colors = fieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = c, onValueChange = { c = it }, label = { Text("Coefficient c") },
                            colors = fieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text("Solves a1·x + b1·y = c1 and a2·x + b2·y = c2", color = Color(0xFF475569), fontSize = 13.sp)
                        
                        Text("Equation 1", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        OutlinedTextField(
                            value = a1, onValueChange = { a1 = it }, label = { Text("a1") },
                            colors = fieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = b1, onValueChange = { b1 = it }, label = { Text("b1") },
                            colors = fieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = c1, onValueChange = { c1 = it }, label = { Text("c1") },
                            colors = fieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Equation 2", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        OutlinedTextField(
                            value = a2, onValueChange = { a2 = it }, label = { Text("a2") },
                            colors = fieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = b2, onValueChange = { b2 = it }, label = { Text("b2") },
                            colors = fieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = c2, onValueChange = { c2 = it }, label = { Text("c2") },
                            colors = fieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (resultText.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White, RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                .padding(16.dp)
                        ) {
                            Column {
                                Text("Result Output", color = Color(0xFF1E40AF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = resultText, color = Color(0xFF1E40AF), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                keyboardController?.hide()
                                focusManager.clearFocus(force = true)
                                try {
                                    if (type == "QUAD") {
                                        val av = a.toDoubleOrNull() ?: 1.0
                                        val bv = b.toDoubleOrNull() ?: 0.0
                                        val cv = c.toDoubleOrNull() ?: 0.0
                                        if (av == 0.0) throw IllegalArgumentException("a cannot be 0")
                                        
                                        val D = bv * bv - 4 * av * cv
                                        if (D >= 0) {
                                            val r1 = (-bv + sqrt(D)) / (2 * av)
                                            val r2 = (-bv - sqrt(D)) / (2 * av)
                                            resultText = "Real roots:\nRoot 1 = $r1\nRoot 2 = $r2"
                                            primaryRoot = r1.toString()
                                        } else {
                                            val real = -bv / (2 * av)
                                            val imag = sqrt(-D) / (2 * av)
                                            resultText = "Complex roots:\nRoot 1 = $real + ${imag}i\nRoot 2 = $real - ${imag}i"
                                            primaryRoot = real.toString()
                                        }
                                    } else {
                                        val a1v = a1.toDoubleOrNull() ?: 0.0
                                        val b1v = b1.toDoubleOrNull() ?: 0.0
                                        val c1v = c1.toDoubleOrNull() ?: 0.0
                                        val a2v = a2.toDoubleOrNull() ?: 0.0
                                        val b2v = b2.toDoubleOrNull() ?: 0.0
                                        val c2v = c2.toDoubleOrNull() ?: 0.0
         
                                        val D = a1v * b2v - b1v * a2v
                                        if (D == 0.0) {
                                            resultText = "No unique solution (Determinant = 0)"
                                        } else {
                                            val x = (c1v * b2v - b1v * c2v) / D
                                            val y = (a1v * c2v - c1v * a2v) / D
                                            resultText = "Solution:\nx = $x\ny = $y"
                                            primaryRoot = x.toString()
                                        }
                                    }
                                } catch (e: Exception) {
                                    resultText = "Calculation Error: ${e.message}"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E40AF)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Solve", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        if (primaryRoot.isNotEmpty()) {
                            Button(
                                onClick = { 
                                    keyboardController?.hide()
                                    focusManager.clearFocus(force = true)
                                    onSolveRealRoot(primaryRoot) 
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Use Root", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = dismissWithKeyboard,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64748B)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Close Solver", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun VariableStoreDialog(
    variableName: String,
    viewModel: CalculatorViewModel,
    onDismiss: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val dismissWithKeyboard = {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        onDismiss()
    }

    var inputVal by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = dismissWithKeyboard,
        title = {
            Text("Store Variable", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Save value to variable '$variableName':", color = Color(0xFF475569))
                OutlinedTextField(
                    value = inputVal,
                    onValueChange = { inputVal = it },
                    placeholder = { Text("Leave empty to use current result", color = Color(0xFF94A3B8)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF0F172A),
                        focusedBorderColor = Color(0xFF1E40AF),
                        unfocusedBorderColor = Color(0xFF94A3B8)
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.storeVariable(variableName, inputVal)
                    dismissWithKeyboard()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E40AF))
            ) {
                Text("Store", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = dismissWithKeyboard) {
                Text("Cancel", color = Color(0xFF1E40AF))
            }
        },
        containerColor = Color.White
    )
}

class UnitMetric(val name: String, val abbr: String, val multiplier: Double)

class ConversionCategory(
    val name: String,
    val units: List<UnitMetric>,
    val customConvert: ((Double, UnitMetric, UnitMetric) -> Double)? = null
) {
    fun convert(value: Double, from: UnitMetric, to: UnitMetric): Double {
        if (customConvert != null) {
            return customConvert.invoke(value, from, to)
        }
        return (value * from.multiplier) / to.multiplier
    }
}

@Composable
fun ConversionDialog(
    onDismiss: () -> Unit,
    onSelectConversionResult: (String) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    val dismissWithKeyboard = {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        onDismiss()
    }

    // Custom non-SI converters
    val tempConvert: (Double, UnitMetric, UnitMetric) -> Double = { value, from, to ->
        val kelvin = when (from.abbr) {
            "°C" -> value + 273.15
            "°F" -> (value - 32.0) * 5.0 / 9.0 + 273.15
            "K"  -> value
            "°R" -> value * 5.0 / 9.0
            "°Ré"-> value * 1.25 + 273.15
            else -> value
        }
        when (to.abbr) {
            "°C" -> kelvin - 273.15
            "°F" -> (kelvin - 273.15) * 1.8 + 32.0
            "K"  -> kelvin
            "°R" -> kelvin * 1.8
            "°Ré"-> (kelvin - 273.15) * 0.8
            else -> kelvin
        }
    }

    val fuelConvert: (Double, UnitMetric, UnitMetric) -> Double = { value, from, to ->
        if (value <= 0.0) 0.0 else {
            val l100k = when (from.abbr) {
                "L/100km" -> value
                "MPG (US)" -> 235.214583 / value
                "MPG (UK)" -> 282.481155 / value
                "km/L" -> 100.0 / value
                else -> value
            }
            when (to.abbr) {
                "L/100km" -> l100k
                "MPG (US)" -> 235.214583 / l100k
                "MPG (UK)" -> 282.481155 / l100k
                "km/L" -> 100.0 / l100k
                else -> l100k
            }
        }
    }

    // Setup group definitions
    val CONVERSION_GROUPS = remember {
        mapOf(
            "1. Common & Daily" to listOf(
                ConversionCategory("Length / Distance", listOf(
                    UnitMetric("Meter", "m", 1.0),
                    UnitMetric("Kilometer", "km", 1000.0),
                    UnitMetric("Decimeter", "dm", 0.1),
                    UnitMetric("Centimeter", "cm", 0.01),
                    UnitMetric("Millimeter", "mm", 0.001),
                    UnitMetric("Micrometer", "µm", 1e-6),
                    UnitMetric("Nanometer", "nm", 1e-9),
                    UnitMetric("Inch", "in", 0.0254),
                    UnitMetric("Foot", "ft", 0.3048),
                    UnitMetric("Yard", "yd", 0.9144),
                    UnitMetric("Mile", "mi", 1609.344),
                    UnitMetric("Nautical Mile", "nmi", 1852.0),
                    UnitMetric("Fathom", "fathom", 1.8288),
                    UnitMetric("Cable", "cable", 185.2),
                    UnitMetric("League", "league", 4828.032),
                    UnitMetric("Mil / Thou", "mil", 0.0000254)
                )),
                ConversionCategory("Mass / Weight", listOf(
                    UnitMetric("Kilogram", "kg", 1.0),
                    UnitMetric("Metric Ton", "t", 1000.0),
                    UnitMetric("Gram", "g", 0.001),
                    UnitMetric("Milligram", "mg", 1e-6),
                    UnitMetric("Microgram", "µg", 1e-9),
                    UnitMetric("Stone", "st", 6.35029318),
                    UnitMetric("Pound", "lb", 0.45359237),
                    UnitMetric("Ounce", "oz", 0.0283495231),
                    UnitMetric("Dram", "dr", 0.0017718452),
                    UnitMetric("Grain", "gr", 0.0000647989),
                    UnitMetric("Short Ton (US)", "ton (US)", 907.18474),
                    UnitMetric("Long Ton (UK)", "ton (UK)", 1016.0469),
                    UnitMetric("Troy Ounce", "t oz", 0.0311034768),
                    UnitMetric("Troy Pound", "t lb", 0.3732417216),
                    UnitMetric("Carat", "ct", 0.0002)
                )),
                ConversionCategory("Volume / Capacity", listOf(
                    UnitMetric("Liter", "L", 1.0),
                    UnitMetric("Cubic Meter", "m³", 1000.0),
                    UnitMetric("Deciliter", "dl", 0.1),
                    UnitMetric("Centiliter", "cl", 0.01),
                    UnitMetric("Milliliter", "mL", 0.001),
                    UnitMetric("Microliter", "µL", 1e-6),
                    UnitMetric("US Gallon", "US gal", 3.78541178),
                    UnitMetric("US Quart", "US qt", 0.94635295),
                    UnitMetric("US Pint", "US pt", 0.47317647),
                    UnitMetric("US Cup", "US cup", 0.24),
                    UnitMetric("US Fluid Ounce", "US fl oz", 0.02957353),
                    UnitMetric("US Tablespoon", "US tbsp", 0.01478676),
                    UnitMetric("US Teaspoon", "US tsp", 0.00492892),
                    UnitMetric("Minim (US)", "minim", 0.00006161),
                    UnitMetric("US Bushel", "US bu", 35.23907),
                    UnitMetric("US Peck", "US pk", 8.809768),
                    UnitMetric("Dry Quart (US)", "dry qt", 1.101221),
                    UnitMetric("Dry Pint (US)", "dry pt", 0.550610),
                    UnitMetric("Imperial Gallon", "Imp gal", 4.54609),
                    UnitMetric("Imperial Pint", "Imp pt", 0.568261),
                    UnitMetric("Imperial Fluid Ounce", "Imp fl oz", 0.028413)
                )),
                ConversionCategory("Area", listOf(
                    UnitMetric("Square Meter", "m²", 1.0),
                    UnitMetric("Square Kilometer", "km²", 1e6),
                    UnitMetric("Hectare", "ha", 10000.0),
                    UnitMetric("Are", "a", 100.0),
                    UnitMetric("Square Centimeter", "cm²", 0.0001),
                    UnitMetric("Square Mile", "mi²", 2589988.11),
                    UnitMetric("Acre", "ac", 4046.85642),
                    UnitMetric("Rood", "rood", 1011.7141),
                    UnitMetric("Square Yard", "yd²", 0.83612736),
                    UnitMetric("Square Foot", "ft²", 0.09290304),
                    UnitMetric("Square Inch", "in²", 0.00064516)
                )),
                ConversionCategory("Temperature", listOf(
                    UnitMetric("Celsius", "°C", 1.0),
                    UnitMetric("Fahrenheit", "°F", 1.0),
                    UnitMetric("Kelvin", "K", 1.0),
                    UnitMetric("Rankine", "°R", 1.0),
                    UnitMetric("Réaumur", "°Ré", 1.0)
                ), tempConvert),
                ConversionCategory("Speed / Velocity", listOf(
                    UnitMetric("Meter per Second", "m/s", 1.0),
                    UnitMetric("Kilometer per Hour", "km/h", 1.0 / 3.6),
                    UnitMetric("Mile per Hour", "mph", 0.44704),
                    UnitMetric("Knot", "kt", 0.51444444),
                    UnitMetric("Mach (Speed of Sound)", "Mach", 340.29)
                )),
                ConversionCategory("Time", listOf(
                    UnitMetric("Second", "s", 1.0),
                    UnitMetric("Millisecond", "ms", 0.001),
                    UnitMetric("Microsecond", "µs", 1e-6),
                    UnitMetric("Nanosecond", "ns", 1e-9),
                    UnitMetric("Picosecond", "ps", 1e-12),
                    UnitMetric("Minute", "min", 60.0),
                    UnitMetric("Hour", "hr", 3600.0),
                    UnitMetric("Day", "d", 86400.0),
                    UnitMetric("Week", "wk", 604800.0),
                    UnitMetric("Month (Avg)", "mo", 2629746.0),
                    UnitMetric("Year (Avg)", "yr", 31557600.0),
                    UnitMetric("Decade", "decade", 315576000.0),
                    UnitMetric("Century", "century", 3155760000.0),
                    UnitMetric("Millennium", "millennium", 31557600000.0)
                ))
            ),
            "2. Science & Lab" to listOf(
                ConversionCategory("Amount of Substance", listOf(
                    UnitMetric("Mole", "mol", 1.0),
                    UnitMetric("Kilomole", "kmol", 1000.0),
                    UnitMetric("Millimole", "mmol", 0.001),
                    UnitMetric("Micromole", "µmol", 1e-6),
                    UnitMetric("Compound Molecule Count", "molecules", 1.0 / 6.02214076e23)
                )),
                ConversionCategory("Density", listOf(
                    UnitMetric("Kilogram/Cubic Meter", "kg/m³", 1.0),
                    UnitMetric("Gram/Cubic Centimeter", "g/cm³", 1000.0),
                    UnitMetric("Gram/Milliliter", "g/mL", 1000.0),
                    UnitMetric("Pound/Cubic Foot", "lb/ft³", 16.018463),
                    UnitMetric("Pound/Gallon", "lb/gal", 119.826427)
                )),
                ConversionCategory("Concentration", listOf(
                    UnitMetric("Grams per Liter", "g/L", 1.0),
                    UnitMetric("Milligrams per Deciliter", "mg/dL", 0.01),
                    UnitMetric("Molarity (M) (NaCl)", "mol/L", 58.44),
                    UnitMetric("Normality (N) (NaCl)", "N", 58.44)
                )),
                ConversionCategory("Concentration Ratios", listOf(
                    UnitMetric("Parts per Million", "ppm", 1.0),
                    UnitMetric("Parts per Billion", "ppb", 0.001),
                    UnitMetric("Parts per Trillion", "ppt", 1e-6)
                )),
                ConversionCategory("Energy / Work", listOf(
                    UnitMetric("Joule", "J", 1.0),
                    UnitMetric("Kilojoule", "kJ", 1000.0),
                    UnitMetric("Megajoule", "MJ", 1e6),
                    UnitMetric("Calorie", "cal", 4.184),
                    UnitMetric("Kilocalorie / Food Cal", "kcal", 4184.0),
                    UnitMetric("Watt-hour", "Wh", 3600.0),
                    UnitMetric("Kilowatt-hour", "kWh", 3.6e6),
                    UnitMetric("British Thermal Unit", "BTU", 1055.05585),
                    UnitMetric("Therm", "therm", 1.05505585e8),
                    UnitMetric("Erg", "erg", 1e-7),
                    UnitMetric("Electronvolt", "eV", 1.60217663e-19),
                    UnitMetric("Foot-pound", "ft·lb", 1.3558179)
                )),
                ConversionCategory("Power", listOf(
                    UnitMetric("Watt", "W", 1.0),
                    UnitMetric("Kilowatt", "kW", 1000.0),
                    UnitMetric("Megawatt", "MW", 1e6),
                    UnitMetric("Gigawatt", "GW", 1e9),
                    UnitMetric("Mechanical Horsepower", "hp", 745.69987),
                    UnitMetric("Metric Horsepower", "ps", 735.49875),
                    UnitMetric("Volt-Ampere", "VA", 1.0),
                    UnitMetric("BTU per Hour", "BTU/h", 0.293071),
                    UnitMetric("Calories per Second", "cal/s", 4.184)
                )),
                ConversionCategory("Radiation Activity", listOf(
                    UnitMetric("Becquerel", "Bq", 1.0),
                    UnitMetric("Curie", "Ci", 3.7e10),
                    UnitMetric("Rutherford", "Rd", 1e6)
                )),
                ConversionCategory("Radiation Absorbed Dose", listOf(
                    UnitMetric("Gray", "Gy", 1.0),
                    UnitMetric("Rad", "rad", 0.01)
                )),
                ConversionCategory("Radiation Equivalent Dose", listOf(
                    UnitMetric("Sievert", "Sv", 1.0),
                    UnitMetric("Rem", "rem", 0.01)
                )),
                ConversionCategory("Radiation Exposure", listOf(
                    UnitMetric("Röntgen", "R", 1.0)
                ))
            ),
            "3. Engineering & Fluids" to listOf(
                ConversionCategory("Pressure", listOf(
                    UnitMetric("Pascal", "Pa", 1.0),
                    UnitMetric("Kilopascal", "kPa", 1000.0),
                    UnitMetric("Megapascal", "MPa", 1e6),
                    UnitMetric("Bar", "bar", 100000.0),
                    UnitMetric("Millibar", "mbar", 100.0),
                    UnitMetric("Technical Atmosphere", "at", 98066.5),
                    UnitMetric("Standard Atmosphere", "atm", 101325.0),
                    UnitMetric("Torr / mmHg", "mmHg", 133.322368),
                    UnitMetric("Inches of Mercury", "inHg", 3386.389),
                    UnitMetric("Pounds per Sq Inch", "psi", 6894.757)
                )),
                ConversionCategory("Force", listOf(
                    UnitMetric("Newton", "N", 1.0),
                    UnitMetric("Kilonewton", "kN", 1000.0),
                    UnitMetric("Dyne", "dyn", 1e-5),
                    UnitMetric("Pound-force", "lbf", 4.4482216),
                    UnitMetric("Kilogram-force", "kgf", 9.80665),
                    UnitMetric("Kip", "kip", 4448.2216)
                )),
                ConversionCategory("Volumetric Flow", listOf(
                    UnitMetric("Liter per Minute", "L/min", 1.0),
                    UnitMetric("Cubic Meter/Second", "m³/s", 60000.0),
                    UnitMetric("Cubic Foot/Minute", "cfm", 28.316846),
                    UnitMetric("Gallon per Minute", "gpm", 3.78541178)
                )),
                ConversionCategory("Mass Flow Rate", listOf(
                    UnitMetric("Kilogram per Second", "kg/s", 1.0),
                    UnitMetric("Gram per Minute", "g/min", 0.001 / 60.0),
                    UnitMetric("Pound per Hour", "lb/hr", 0.453592 / 3600.0)
                )),
                ConversionCategory("Viscosity (Dynamic)", listOf(
                    UnitMetric("Pascal-second", "Pa·s", 1.0),
                    UnitMetric("Poise", "P", 0.1),
                    UnitMetric("Centipoise", "cP", 0.001)
                )),
                ConversionCategory("Viscosity (Kinematic)", listOf(
                    UnitMetric("Sq Meter per Second", "m²/s", 1.0),
                    UnitMetric("Stokes", "St", 0.0001),
                    UnitMetric("Centistokes", "cSt", 1e-6)
                )),
                ConversionCategory("Torque", listOf(
                    UnitMetric("Newton-meter", "N·m", 1.0),
                    UnitMetric("Pound-foot", "lb·ft", 1.355818),
                    UnitMetric("Pound-inch", "lb·in", 0.1129848),
                    UnitMetric("Kilogram-force meter", "kgf·m", 9.80665)
                )),
                ConversionCategory("Angle", listOf(
                    UnitMetric("Radian", "rad", 1.0),
                    UnitMetric("Degree", "°", Math.PI / 180.0),
                    UnitMetric("Gradian", "grad", Math.PI / 200.0),
                    UnitMetric("Arcminute", "'", Math.PI / 10800.0),
                    UnitMetric("Arcsecond", "\"", Math.PI / 648000.0),
                    UnitMetric("Circle Turn / Revolution", "turn", 2.0 * Math.PI)
                ))
            ),
            "4. Electric & Light" to listOf(
                ConversionCategory("Electric Current", listOf(
                    UnitMetric("Ampere", "A", 1.0),
                    UnitMetric("Kiloampere", "kA", 1000.0),
                    UnitMetric("Milliampere", "mA", 0.001),
                    UnitMetric("Microampere", "µA", 1e-6),
                    UnitMetric("Biot", "Bi", 10.0)
                )),
                ConversionCategory("Voltage", listOf(
                    UnitMetric("Volt", "V", 1.0),
                    UnitMetric("Kilovolt", "kV", 1000.0),
                    UnitMetric("Megavolt", "MV", 1e6),
                    UnitMetric("Millivolt", "mV", 0.001),
                    UnitMetric("Microvolt", "µv", 1e-6)
                )),
                ConversionCategory("Electric Resistance", listOf(
                    UnitMetric("Ohm", "Ω", 1.0),
                    UnitMetric("Kilohm", "kΩ", 1000.0),
                    UnitMetric("Megohm", "MΩ", 1e6),
                    UnitMetric("Statohm", "statΩ", 8.9875517e11),
                    UnitMetric("Abohm", "abΩ", 1e-9)
                )),
                ConversionCategory("Capacitance", listOf(
                    UnitMetric("Farad", "F", 1.0),
                    UnitMetric("Millifarad", "mF", 0.001),
                    UnitMetric("Microfarad", "µF", 1e-6),
                    UnitMetric("Nanofarad", "nF", 1e-9),
                    UnitMetric("Picofarad", "pF", 1e-12)
                )),
                ConversionCategory("Inductance", listOf(
                    UnitMetric("Henry", "H", 1.0),
                    UnitMetric("Kilohenry", "kH", 1000.0),
                    UnitMetric("Millihenry", "mH", 0.001),
                    UnitMetric("Microhenry", "µH", 1e-6),
                    UnitMetric("Nanohenry", "nH", 1e-9)
                )),
                ConversionCategory("Magnetic Flux", listOf(
                    UnitMetric("Weber", "Wb", 1.0),
                    UnitMetric("Milliweber", "mWb", 0.001),
                    UnitMetric("Microweber", "µWb", 1e-6),
                    UnitMetric("Maxwell", "Mx", 1e-8)
                )),
                ConversionCategory("Magnetic Field Strength", listOf(
                    UnitMetric("Tesla", "T", 1.0),
                    UnitMetric("Gauss", "G", 1e-4),
                    UnitMetric("Millitesla", "mT", 0.001),
                    UnitMetric("Microtesla", "µT", 1e-6),
                    UnitMetric("Gamma", "γ", 1e-9)
                )),
                ConversionCategory("Illuminance", listOf(
                    UnitMetric("Lux", "lx", 1.0),
                    UnitMetric("Foot-candle", "fc", 10.76391),
                    UnitMetric("Phot", "ph", 10000.0)
                )),
                ConversionCategory("Luminance", listOf(
                    UnitMetric("Nit (cd/m²)", "nit", 1.0),
                    UnitMetric("Lambert", "L", 3183.09886),
                    UnitMetric("Foot-lambert", "fL", 3.426259)
                ))
            ),
            "5. Compute & Lifestyle" to listOf(
                ConversionCategory("Data Storage (Decimal)", listOf(
                    UnitMetric("Bit", "b", 0.125),
                    UnitMetric("Byte", "B", 1.0),
                    UnitMetric("Kilobyte", "KB", 1000.0),
                    UnitMetric("Megabyte", "MB", 1e6),
                    UnitMetric("Gigabyte", "GB", 1e9),
                    UnitMetric("Terabyte", "TB", 1e12),
                    UnitMetric("Petabyte", "PB", 1e15),
                    UnitMetric("Exabyte", "EB", 1e18)
                )),
                ConversionCategory("Data Storage (Binary)", listOf(
                    UnitMetric("Kibibyte", "KiB", 1024.0),
                    UnitMetric("Mebibyte", "MiB", 1048576.0),
                    UnitMetric("Gibibyte", "GiB", 1073741824.0),
                    UnitMetric("Tebibyte", "TiB", 1099511627776.0),
                    UnitMetric("Pebibyte", "PiB", 1125899906842624.0)
                )),
                ConversionCategory("Data Transfer Rate", listOf(
                    UnitMetric("Bits per second", "bps", 1.0),
                    UnitMetric("Kilobits per sec", "kbps", 1000.0),
                    UnitMetric("Megabits per sec", "Mbps", 1e6),
                    UnitMetric("Gigabits per sec", "Gbps", 1e9),
                    UnitMetric("Bytes per second", "B/s", 8.0),
                    UnitMetric("Megabytes per sec", "MB/s", 8e6)
                )),
                ConversionCategory("Fuel Consumption", listOf(
                    UnitMetric("Liters per 100km", "L/100km", 1.0),
                    UnitMetric("Miles per Gallon (US)", "MPG (US)", 1.0),
                    UnitMetric("Miles per Gallon (UK)", "MPG (UK)", 1.0),
                    UnitMetric("Kilometers per Liter", "km/L", 1.0)
                ), fuelConvert),
                ConversionCategory("Typography", listOf(
                    UnitMetric("Point", "pt", 1.0),
                    UnitMetric("Pica", "pc", 12.0),
                    UnitMetric("Pixel", "px", 0.75),
                    UnitMetric("Em", "em", 12.0),
                    UnitMetric("Rem", "rem", 12.0),
                    UnitMetric("Inch", "in", 72.0),
                    UnitMetric("Millimeter", "mm", 2.8346)
                ))
            )
        )
    }

    // Size datasets
    val mensShoeSizes = remember {
        listOf(
            mapOf("US (Men)" to "6.0", "UK" to "5.0", "EU" to "39", "JP (cm)" to "24.0", "Int" to "S"),
            mapOf("US (Men)" to "7.0", "UK" to "6.0", "EU" to "40", "JP (cm)" to "25.0", "Int" to "S"),
            mapOf("US (Men)" to "8.0", "UK" to "7.0", "EU" to "41", "JP (cm)" to "26.0", "Int" to "M"),
            mapOf("US (Men)" to "9.0", "UK" to "8.0", "EU" to "42", "JP (cm)" to "27.0", "Int" to "M"),
            mapOf("US (Men)" to "10.0", "UK" to "9.0", "EU" to "43", "JP (cm)" to "28.0", "Int" to "L"),
            mapOf("US (Men)" to "11.0", "UK" to "10.0", "EU" to "44", "JP (cm)" to "29.0", "Int" to "L"),
            mapOf("US (Men)" to "12.0", "UK" to "11.0", "EU" to "45", "JP (cm)" to "30.0", "Int" to "XL"),
            mapOf("US (Men)" to "13.0", "UK" to "12.0", "EU" to "46", "JP (cm)" to "31.0", "Int" to "XXL")
        )
    }

    val womensShoeSizes = remember {
        listOf(
            mapOf("US (Women)" to "5.0", "UK" to "3.0", "EU" to "35", "JP (cm)" to "21.0", "Int" to "XS"),
            mapOf("US (Women)" to "6.0", "UK" to "4.0", "EU" to "36", "JP (cm)" to "22.0", "Int" to "S"),
            mapOf("US (Women)" to "7.0", "UK" to "5.0", "EU" to "37", "JP (cm)" to "23.0", "Int" to "S"),
            mapOf("US (Women)" to "8.0", "UK" to "6.0", "EU" to "38", "JP (cm)" to "24.0", "Int" to "M"),
            mapOf("US (Women)" to "9.0", "UK" to "7.0", "EU" to "39", "JP (cm)" to "25.0", "Int" to "M"),
            mapOf("US (Women)" to "10.0", "UK" to "8.0", "EU" to "40", "JP (cm)" to "26.0", "Int" to "L")
        )
    }

    val unisexClothingSizes = remember {
        listOf(
            mapOf("Int" to "XS", "US (Chest)" to "34", "UK" to "34", "EU" to "44", "JP" to "S", "Chest (cm)" to "86"),
            mapOf("Int" to "S", "US (Chest)" to "36", "UK" to "36", "EU" to "46", "JP" to "M", "Chest (cm)" to "91"),
            mapOf("Int" to "M", "US (Chest)" to "38", "UK" to "38", "EU" to "48", "JP" to "L", "Chest (cm)" to "96"),
            mapOf("Int" to "L", "US (Chest)" to "40", "UK" to "40", "EU" to "50", "JP" to "LL", "Chest (cm)" to "101"),
            mapOf("Int" to "XL", "US (Chest)" to "42", "UK" to "42", "EU" to "52", "JP" to "3L", "Chest (cm)" to "106"),
            mapOf("Int" to "XXL", "US (Chest)" to "44", "UK" to "44", "EU" to "54", "JP" to "4L", "Chest (cm)" to "111")
        )
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf("1. Common & Daily") }
    var selectedCategory by remember { mutableStateOf(CONVERSION_GROUPS["1. Common & Daily"]!![0]) }
    var fromUnit by remember(selectedCategory) { mutableStateOf(selectedCategory.units[0]) }
    var inputValue by remember { mutableStateOf("1") }

    // Apparel states
    var isApparelMode by remember { mutableStateOf(false) }
    var apparelType by remember { mutableStateOf("Men's Shoes") }
    var apparelSystem by remember(apparelType) {
        mutableStateOf(if (apparelType == "Unisex Clothing") "Int" else if (apparelType == "Women's Shoes") "US (Women)" else "US (Men)")
    }
    val sizeOptions = remember(apparelType, apparelSystem) {
        val list = when (apparelType) {
            "Men's Shoes" -> mensShoeSizes
            "Women's Shoes" -> womensShoeSizes
            else -> unisexClothingSizes
        }
        list.mapNotNull { it[apparelSystem] }.distinct()
    }
    var apparelSize by remember(sizeOptions) { mutableStateOf(sizeOptions.firstOrNull() ?: "") }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color(0xFF0F172A),
        unfocusedTextColor = Color(0xFF0F172A),
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        focusedBorderColor = Color(0xFF1E40AF),
        unfocusedBorderColor = Color(0xFF94A3B8)
    )

    fun formatConvertedValue(v: Double): String {
        if (v.isNaN() || v.isInfinite()) return "N/A"
        val absV = abs(v)
        if (absV == 0.0) return "0"
        return if (absV >= 0.0001 && absV <= 10000000.0) {
            val formatted = String.format(Locale.US, "%.6f", v)
            var res = formatted
            while (res.endsWith("0")) {
                res = res.substring(0, res.length - 1)
            }
            if (res.endsWith(".")) {
                res = res.substring(0, res.length - 1)
            }
            res
        } else {
            String.format(Locale.US, "%.5e", v)
        }
    }

    Dialog(
        onDismissRequest = dismissWithKeyboard,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF8FAFC)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp)
            ) {
                // Header (App bar)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = dismissWithKeyboard) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF0F172A)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "IRA05-26 Metrology Reference",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1E3A8A),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Interactive Realtime Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search units (e.g. meter, pascal, density)...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    colors = fieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                if (searchQuery.isNotEmpty()) {
                    // Search Results List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val uppercaseQuery = searchQuery.uppercase(Locale.US)
                        
                        // Apparel matches
                        if ("SHOE".contains(uppercaseQuery) || "CLOTHING".contains(uppercaseQuery) || "SIZE".contains(uppercaseQuery) || "APPAREL".contains(uppercaseQuery)) {
                            item {
                                Card(
                                    onClick = {
                                        isApparelMode = true
                                        selectedGroup = "5. Compute & Lifestyle"
                                        searchQuery = ""
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Apparel & Shoe Sizes", fontWeight = FontWeight.Bold, color = Color(0xFF1E40AF))
                                        Text("Convert US, UK, Europe, Japan, and International sizes.", fontSize = 12.sp, color = Color(0xFF475569))
                                    }
                                }
                            }
                        }

                        // Grid category matches
                        CONVERSION_GROUPS.forEach { (grpName, categories) ->
                            categories.forEach { cat ->
                                val score = cat.name.uppercase(Locale.US).contains(uppercaseQuery) ||
                                        cat.units.any { it.name.uppercase(Locale.US).contains(uppercaseQuery) || it.abbr.uppercase(Locale.US).contains(uppercaseQuery) }
                                
                                if (score) {
                                    item {
                                        Card(
                                            onClick = {
                                                isApparelMode = false
                                                selectedGroup = grpName
                                                selectedCategory = cat
                                                searchQuery = ""
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = Color.White),
                                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(cat.name, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                                Text("Group: $grpName • Unit count: ${cat.units.size}", fontSize = 11.sp, color = Color(0xFF64748B))
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(cat.units.joinToString { "${it.name} (${it.abbr})" }, fontSize = 11.sp, color = Color(0xFF475569), maxLines = 1)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Modern Category Selectors Group Row
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(vertical = 8.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CONVERSION_GROUPS.keys.forEach { groupKey ->
                            val isSelected = selectedGroup == groupKey && !isApparelMode
                            item {
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        isApparelMode = false
                                        selectedGroup = groupKey
                                        val firstCat = CONVERSION_GROUPS[groupKey]!![0]
                                        selectedCategory = firstCat
                                    },
                                    label = { Text(groupKey, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF1E40AF),
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                        
                        // Apparel option
                        item {
                            FilterChip(
                                selected = isApparelMode,
                                onClick = {
                                    isApparelMode = true
                                    selectedGroup = "5. Compute & Lifestyle"
                                },
                                label = { Text("Apparel & Shoe Sizes", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF0D9488),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    if (!isApparelMode) {
                        // Sub-categories listed as elevated filter chips
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val categories = CONVERSION_GROUPS[selectedGroup] ?: emptyList()
                            items(categories) { cat ->
                                val isSelected = selectedCategory == cat
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedCategory = cat },
                                    label = { Text(cat.name, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF60A5FA),
                                        selectedLabelColor = Color.Black
                                    )
                                )
                            }
                        }
                    }

                    // Main Content Section
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (isApparelMode) {
                            // Apparel Sizer UI
                            Text(
                                text = "Apparel Sizes Conversions",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            
                            // Category Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Men's Shoes", "Women's Shoes", "Unisex Clothing").forEach { t ->
                                    val isSelected = apparelType == t
                                    Button(
                                        onClick = { apparelType = t },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) Color(0xFF0D9488) else Color(0xFFE2E8F0)
                                        ),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                    ) {
                                        Text(t, color = if (isSelected) Color.White else Color(0xFF334155), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Source System Selector
                            val systems = remember(apparelType) {
                                when (apparelType) {
                                    "Men's Shoes" -> listOf("US (Men)", "UK", "EU", "JP (cm)", "Int")
                                    "Women's Shoes" -> listOf("US (Women)", "UK", "EU", "JP (cm)", "Int")
                                    else -> listOf("Int", "US (Chest)", "UK", "EU", "JP", "Chest (cm)")
                                }
                            }

                            var systemMenuExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedCard(
                                    onClick = { systemMenuExpanded = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.outlinedCardColors(containerColor = Color.White)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Sizing System", fontSize = 11.sp, color = Color(0xFF64748B))
                                            Text(apparelSystem, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                        }
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Expand spinner")
                                    }
                                }
                                DropdownMenu(
                                    expanded = systemMenuExpanded,
                                    onDismissRequest = { systemMenuExpanded = false },
                                    modifier = Modifier.fillMaxWidth(0.9f).background(Color.White)
                                ) {
                                    systems.forEach { sys ->
                                        DropdownMenuItem(
                                            text = { Text(sys, color = Color(0xFF0F172A)) },
                                            onClick = {
                                                apparelSystem = sys
                                                systemMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Size Selector
                            var sizeMenuExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedCard(
                                    onClick = { sizeMenuExpanded = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.outlinedCardColors(containerColor = Color.White)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Selected Size rating", fontSize = 11.sp, color = Color(0xFF64748B))
                                            Text(apparelSize, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                        }
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Expand size selection")
                                    }
                                }
                                DropdownMenu(
                                    expanded = sizeMenuExpanded,
                                    onDismissRequest = { sizeMenuExpanded = false },
                                    modifier = Modifier.fillMaxWidth(0.9f).background(Color.White)
                                ) {
                                    sizeOptions.forEach { size ->
                                        DropdownMenuItem(
                                            text = { Text(size, color = Color(0xFF0F172A)) },
                                            onClick = {
                                                apparelSize = size
                                                sizeMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Equivalent Conversions Table:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))

                            // Find and translate row
                            val sizeData = remember(apparelType, apparelSystem, apparelSize) {
                                val list = when (apparelType) {
                                    "Men's Shoes" -> mensShoeSizes
                                    "Women's Shoes" -> womensShoeSizes
                                    else -> unisexClothingSizes
                                }
                                list.find { it[apparelSystem] == apparelSize }
                            }

                            if (sizeData != null) {
                                sizeData.forEach { (scaledSystem, scaledValue) ->
                                    if (scaledSystem != apparelSystem) {
                                        OutlinedCard(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.outlinedCardColors(containerColor = Color.White)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(scaledSystem, fontSize = 11.sp, color = Color(0xFF64748B))
                                                    Text(scaledValue, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0D9488))
                                                }
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    IconButton(onClick = {
                                                        val clip = android.content.ClipData.newPlainText("Copied apparel size", scaledValue)
                                                        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                        cb.setPrimaryClip(clip)
                                                    }) {
                                                        Icon(Icons.Default.Share, contentDescription = "Copy size", tint = Color(0xFF64748B))
                                                    }
                                                    Button(
                                                        onClick = {
                                                            onSelectConversionResult(scaledValue)
                                                            dismissWithKeyboard()
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Text("Insert", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Text("Select valid sizing elements to display calculations.", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            }
                        } else {
                            // Standard Multi-metrology calculator
                            Text(
                                text = "${selectedCategory.name} Live conversions",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )

                            // Numerical input box
                            OutlinedTextField(
                                value = inputValue,
                                onValueChange = { inputValue = it },
                                label = { Text("Input scalar quantity v") },
                                colors = fieldColors,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            // Source Unit Type Selector Dropdown Card
                            var fromMenuExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedCard(
                                    onClick = { fromMenuExpanded = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.outlinedCardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, Color(0xFF94A3B8))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("Source Unit Type (From)", fontSize = 11.sp, color = Color(0xFF64748B))
                                            Text("${fromUnit.name} (${fromUnit.abbr})", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                        }
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Expand spinner",
                                            tint = Color(0xFF475569)
                                        )
                                    }
                                }
                                
                                DropdownMenu(
                                    expanded = fromMenuExpanded,
                                    onDismissRequest = { fromMenuExpanded = false },
                                    modifier = Modifier.fillMaxWidth(0.9f).background(Color.White)
                                ) {
                                    selectedCategory.units.forEach { unit ->
                                        DropdownMenuItem(
                                            text = { Text("${unit.name} (${unit.abbr})", color = Color(0xFF0F172A)) },
                                            onClick = {
                                                fromUnit = unit
                                                fromMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Equivalent Quantities Matrix:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))

                            // Convert to ALL OTHER units instantly
                            val inValParsed = inputValue.toDoubleOrNull() ?: 1.0
                            selectedCategory.units.forEach { targetUnit ->
                                if (targetUnit != fromUnit) {
                                    val convertedVal = try {
                                        selectedCategory.convert(inValParsed, fromUnit, targetUnit)
                                    } catch (e: Exception) {
                                        Double.NaN
                                    }
                                    val strVal = formatConvertedValue(convertedVal)

                                    OutlinedCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.outlinedCardColors(containerColor = Color.White),
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("${targetUnit.name} (${targetUnit.abbr})", fontSize = 11.sp, color = Color(0xFF64748B))
                                                Text(strVal, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E40AF))
                                            }
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                IconButton(onClick = {
                                                    val clip = android.content.ClipData.newPlainText("Metrology value", strVal)
                                                    val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                    cb.setPrimaryClip(clip)
                                                }) {
                                                    Icon(Icons.Default.Share, contentDescription = "Copy value", tint = Color(0xFF64748B))
                                                }
                                                Button(
                                                    onClick = {
                                                        onSelectConversionResult(strVal)
                                                        dismissWithKeyboard()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E40AF)),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text("Insert", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = dismissWithKeyboard,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64748B)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Close Reference Matrix", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

class SearchItem(
    val label: String,
    val category: String,
    val hint: String,
    val onSelect: () -> Unit
)

@Composable
fun SettingsDialog(
    currentTheme: CalcTheme,
    onSelectTheme: (CalcTheme) -> Unit,
    angleMode: String,
    onToggleAngleMode: () -> Unit,
    isHighPrecision: Boolean,
    onToggleHighPrecision: () -> Unit,
    onClearVariables: () -> Unit,
    onClearHistory: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(24.dp))
                .border(2.dp, currentTheme.operatorColor.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
            color = if (currentTheme.isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings icon",
                            tint = currentTheme.functionalTextColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Metrology Preferences",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = currentTheme.labelColor
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Settings",
                            tint = currentTheme.labelColor.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = currentTheme.labelColor.copy(alpha = 0.12f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable content body
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // 1. Theme selection grid section
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "ACTIVE COLOR PALETTE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = currentTheme.functionalTextColor,
                            letterSpacing = 1.sp
                        )
                        
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(ThemesList) { theme ->
                                val isSelected = theme.id == currentTheme.id
                                Card(
                                    modifier = Modifier
                                        .width(160.dp)
                                        .clickable { onSelectTheme(theme) },
                                    shape = RoundedCornerShape(12.dp),
                                    border = if (isSelected) BorderStroke(2.dp, theme.operatorColor) else BorderStroke(1.dp, theme.labelColor.copy(alpha = 0.1f)),
                                    colors = CardDefaults.cardColors(containerColor = theme.backgroundColor)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = theme.name,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = theme.expressionTextColor,
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(modifier = Modifier.size(12.dp).background(theme.operatorColor, CircleShape))
                                            Box(modifier = Modifier.size(12.dp).background(theme.actionColor, CircleShape))
                                            Box(modifier = Modifier.size(12.dp).background(theme.digitColor, CircleShape))
                                            Spacer(modifier = Modifier.weight(1f))
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = theme.functionalTextColor,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Calculator Math Preference switches
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "CALCULATION ENGINE SETTINGS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = currentTheme.functionalTextColor,
                            letterSpacing = 1.sp
                        )

                        // Angle mode setting
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, currentTheme.labelColor.copy(alpha = 0.08f)),
                            colors = CardDefaults.cardColors(containerColor = currentTheme.actionColor.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Trigonometric Angle Unit",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = currentTheme.labelColor
                                    )
                                    Text(
                                        text = "Degrees (DEG) or Radians (RAD) calculations",
                                        fontSize = 11.sp,
                                        color = currentTheme.labelColor.copy(alpha = 0.6f)
                                    )
                                }
                                Button(
                                    onClick = onToggleAngleMode,
                                    colors = ButtonDefaults.buttonColors(containerColor = currentTheme.operatorColor),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(angleMode, color = currentTheme.operatorTextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Decimal precision setting
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, currentTheme.labelColor.copy(alpha = 0.08f)),
                            colors = CardDefaults.cardColors(containerColor = currentTheme.actionColor.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "High Precision Math Engine",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = currentTheme.labelColor
                                    )
                                    Text(
                                        text = "BigDecimal Arbitrary Precision modes for exact results",
                                        fontSize = 11.sp,
                                        color = currentTheme.labelColor.copy(alpha = 0.6f)
                                    )
                                }
                                Button(
                                    onClick = onToggleHighPrecision,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isHighPrecision) currentTheme.operatorColor else currentTheme.actionColor
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    border = if (!isHighPrecision) BorderStroke(1.dp, currentTheme.functionalTextColor.copy(alpha = 0.3f)) else null
                                ) {
                                    Text(
                                        text = if (isHighPrecision) "HIGH PREC" else "FLOAT64",
                                        color = if (isHighPrecision) currentTheme.operatorTextColor else currentTheme.functionalTextColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }

                    // 3. Data & cache cleaners
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "DATA & CACHE MAINTENANCE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = currentTheme.functionalTextColor,
                            letterSpacing = 1.sp
                        )

                        // Clear variables button
                        OutlinedButton(
                            onClick = {
                                onClearVariables()
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.3f))
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reset Predefined variables (x,y,z,t,a,b,c,d)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Clear history button
                        OutlinedButton(
                            onClick = {
                                onClearHistory()
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.3f))
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Flush All Calculation Logs Database", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // 4. Aerospace Metrology specification
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = currentTheme.actionColor.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "SYSTEM VERIFICATION",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = currentTheme.functionalTextColor
                            )
                            Text(
                                "Engine: IRA05-26 Aerospace Spec",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = currentTheme.labelColor
                            )
                            Text(
                                "Standard: ISO 2768 metrology-accurate decimal solvers, fractions, Symbolic CAS, 2D vector coordinates & statistics modules.",
                                fontSize = 11.sp,
                                color = currentTheme.labelColor.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = currentTheme.operatorColor),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Apply Preferences", color = currentTheme.operatorTextColor, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun GlobalSearchDialog(
    onDismiss: () -> Unit,
    onOpenWorksheet: (String) -> Unit,
    onOpenSolver: (String) -> Unit,
    onOpenConversion: () -> Unit,
    onOpenGrapher: () -> Unit,
    onInjectFunction: (String) -> Unit,
    onInjectConstant: (String) -> Unit,
    onInjectOperator: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    
    // Setup searchable array
    val items = remember {
        listOf(
            // Worksheets
            SearchItem("Calculus & Limits Worksheet", "Advanced Worksheet", "Evaluate limits, derivatives, definite integrals") { onOpenWorksheet("calculus") },
            SearchItem("Linear Algebra Studio (3x3)", "Advanced Worksheet", "Solve 3x3 matrices - det, inverse, eigenvalues") { onOpenWorksheet("linalg") },
            SearchItem("Complex Numbers & DeMoivre", "Advanced Worksheet", "Perform complex analysis & polar calculations") { onOpenWorksheet("complex") },
            SearchItem("Linear Regression Studio", "Advanced Worksheet", "Analyze stats and generate linear regression tables") { onOpenWorksheet("regression") },
            SearchItem("Special Functions & Stats", "Advanced Worksheet", "Bessel functions, Gamma, normal CDF, stats analyzer") { onOpenWorksheet("special") },
            SearchItem("Financial TVM Ledger", "Advanced Worksheet", "Financial values: PV, FV, PMT, NPV, Cashflow") { onOpenWorksheet("finance") },
            SearchItem("Symbolic CAS Studio (Factor/Expand)", "Advanced Worksheet", "Compute symbolic factorizations and expansions") { onOpenWorksheet("cas") },
            SearchItem("Fraction & Greater Precision Converter", "Advanced Worksheet", "Convert floats to rational fraction ratios") { onOpenWorksheet("fraction") },
            
            // Solvers
            SearchItem("ax² + bx + c = 0 Quadratic Solver", "Solver", "Find real or complex roots for quadratic formulas") { onOpenSolver("QUAD") },
            SearchItem("2x2 Linear Equations System Solver", "Solver", "Solve system of 2 linear equations variables") { onOpenSolver("SYSTEM") },
            SearchItem("2D Canvas Function Grapher", "Visual Plotter", "Plot multiple equations on an interactive Coordinate Cartesian canvas") { onOpenGrapher() },
            SearchItem("Scientific Unit Converter & Apparel Sizes", "Converter", "Length, weight, volume, temperature, speed, area, data, speed, shoe sizing") { onOpenConversion() },
            
            // Functions
            SearchItem("sin (Sine function)", "Trigonometry", "Insert sine trigonometric function sin()") { onInjectFunction("sin(") },
            SearchItem("cos (Cosine function)", "Trigonometry", "Insert cosine trigonometric function cos()") { onInjectFunction("cos(") },
            SearchItem("tan (Tangent function)", "Trigonometry", "Insert tangent trigonometric function tan()") { onInjectFunction("tan(") },
            SearchItem("asin (Inverse Sine / arcsin)", "Trigonometry", "Insert arcsine trigonometric function asin()") { onInjectFunction("asin(") },
            SearchItem("acos (Inverse Cosine / arccos)", "Trigonometry", "Insert arccosine trigonometric function acos()") { onInjectFunction("acos(") },
            SearchItem("atan (Inverse Tangent / arctan)", "Trigonometry", "Insert arctangent trigonometric function atan()") { onInjectFunction("atan(") },
            SearchItem("sinh (Hyperbolic Sine)", "Hyperbolic", "Insert hyperbolic sine function sinh()") { onInjectFunction("sinh(") },
            SearchItem("cosh (Hyperbolic Cosine)", "Hyperbolic", "Insert hyperbolic cosine function cosh()") { onInjectFunction("cosh(") },
            SearchItem("tanh (Hyperbolic Tangent)", "Hyperbolic", "Insert hyperbolic tangent function tanh()") { onInjectFunction("tanh(") },
            SearchItem("abs (Absolute Value)", "Math Operator", "Get raw positive value of numerical numbers abs()") { onInjectFunction("abs(") },
            SearchItem("gcd (Greatest Common Divisor)", "Math Operator", "Calculate G.C.D. between numbers gcd()") { onInjectFunction("gcd(") },
            SearchItem("lcm (Least Common Multiple)", "Math Operator", "Calculate L.C.M. of numbers lcm()") { onInjectFunction("lcm(") },
            SearchItem("mod (Modulo Remainder)", "Math Operator", "Find positive modulus remainder mod()") { onInjectFunction("mod(") },
            SearchItem("round (Precision Rounding)", "Math Operator", "Round values to specified nearest decimal digits round()") { onInjectFunction("round(") },
            SearchItem("min (Minimum selector)", "Math Operator", "Select smallest value from input list min()") { onInjectFunction("min(") },
            SearchItem("max (Maximum selector)", "Math Operator", "Select greatest value from input list max()") { onInjectFunction("max(") },
            SearchItem("nCr (Combinations selector)", "Probability", "Calculate probability combinations of N items chosen in R size") { onInjectOperator("nCr") },
            SearchItem("nPr (Permutations selector)", "Probability", "Calculate ordered permutation subsets probability") { onInjectOperator("nPr") },
            
            // Constants
            SearchItem("c (Speed of light constant in vacuum)", "Constants", "Insert Speed of light: 299,792,458 m/s") { onInjectConstant("299792458") },
            SearchItem("g (Standard gravities acceleration)", "Constants", "Insert gravity constant: 9.80665 m/s²") { onInjectConstant("9.80665") },
            SearchItem("h (Planck scientific constant)", "Constants", "Insert Planck: 6.62607e-34 j·s") { onInjectConstant("6.626068e-34") },
            SearchItem("NA (Avogadro constant mole count)", "Constants", "Insert Avogadro count: 6.022141e23") { onInjectConstant("6.022141e23") },
            SearchItem("R (Gas constant thermodynamics energy)", "Constants", "Insert gas constant: 8.31447") { onInjectConstant("8.314472") },
            SearchItem("e (Elementary electron charge unit)", "Constants", "Insert electron charge: 1.602176e-19 C") { onInjectConstant("1.602176e-19") },
            SearchItem("π (Pi Ratio)", "Constants", "Insert Pi geometric ratio constant") { onInjectConstant("π") },
            SearchItem("e (Euler constant value)", "Constants", "Insert natural base Euler constant value") { onInjectConstant("e") }
        )
    }

    val filteredItems = remember(query) {
        if (query.isBlank()) {
            items
        } else {
            val q = query.lowercase(Locale.US).trim()
            items.filter {
                it.label.lowercase(Locale.US).contains(q) ||
                it.category.lowercase(Locale.US).contains(q) ||
                it.hint.lowercase(Locale.US).contains(q)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(20.dp)),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header search bar row
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search solvers, formulas, sheets, constants...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon", tint = Color(0xFF1E40AF)) },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear text")
                                }
                            }
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Close search", tint = Color.Gray)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF1E40AF),
                        unfocusedBorderColor = Color(0xFF94A3B8),
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = Color(0xFFF8FAFC)
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // List header identifier
                Text(
                    text = if (query.isEmpty()) "ALL POWERFUL METROLOGY FUNCTIONS & TOOLS" else "SEARCH RESULTS (${filteredItems.size})",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF64748B),
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 8.dp)
                )

                // List of matched items
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (filteredItems.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "No tools matched your search criteria.",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF475569)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Try typing 'sin', 'Planck', 'Calculus', 'Matrix', or 'Solvers'",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    } else {
                        items(filteredItems) { item ->
                            val catColor = when (item.category) {
                                "Advanced Worksheet" -> Color(0xFFECFDF5) to Color(0xFF047857) // emerald tint
                                "Solver" -> Color(0xFFEFF6FF) to Color(0xFF1D4ED8) // blue tint
                                "Visual Plotter" -> Color(0xFFFAF5FF) to Color(0xFF7E22CE) // purple tint
                                "Converter" -> Color(0xFFFFF7ED) to Color(0xFFC2410C) // orange tint
                                "Trigonometry" -> Color(0xFFFFF1F2) to Color(0xFFBE123C) // rose tint
                                "Hyperbolic" -> Color(0xFFFFF1F2) to Color(0xFFBE123C) // rose tint
                                "Constants" -> Color(0xFFF0FDF4) to Color(0xFF15803D) // green tint
                                else -> Color(0xFFF1F5F9) to Color(0xFF475569) // gray tint
                            }
                            
                            Card(
                                onClick = item.onSelect,
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = item.label,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF0F172A)
                                            )
                                            
                                            Box(
                                                modifier = Modifier
                                                    .background(catColor.first, RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = item.category,
                                                    color = catColor.second,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = item.hint,
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B),
                                            lineHeight = 14.sp
                                        )
                                    }
                                    
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Execute action",
                                        tint = Color(0xFFCBD5E1),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FunctionGrapherDialog(
    angleMode: String,
    onDismiss: () -> Unit
) {
    // Formulas setup - up to 3 concurrent functions
    var formula1 by remember { mutableStateOf("sin(x) * x") }
    var formula2 by remember { mutableStateOf("cos(x) * 3") }
    var formula3 by remember { mutableStateOf("abs(x) - 4") }

    var formula1Enabled by remember { mutableStateOf(true) }
    var formula2Enabled by remember { mutableStateOf(false) }
    var formula3Enabled by remember { mutableStateOf(false) }

    // Selected textbox focus track to inject presets
    var activeFormulaSelection by remember { mutableStateOf(1) } // 1, 2, or 3

    // Double precision bounds parameters
    var xMinStr by remember { mutableStateOf("-10") }
    var xMaxStr by remember { mutableStateOf("10") }
    var yMinStr by remember { mutableStateOf("-10") }
    var yMaxStr by remember { mutableStateOf("10") }

    var xMin by remember(xMinStr) { mutableStateOf(xMinStr.toDoubleOrNull() ?: -10.0) }
    var xMax by remember(xMaxStr) { mutableStateOf(xMaxStr.toDoubleOrNull() ?: 10.0) }
    var yMin by remember(yMinStr) { mutableStateOf(yMinStr.toDoubleOrNull() ?: -10.0) }
    var yMax by remember(yMaxStr) { mutableStateOf(yMaxStr.toDoubleOrNull() ?: 10.0) }

    // Multi-curve preset templates
    val presets = listOf("sin(x) * x", "sin(x)", "cos(x)", "abs(x)", "x^2 - 4", "ln(x)", "sqrt(x)", "3 * sin(x)/x")

    // Interactive Tap tracing state
    var tappedXVal by remember { mutableStateOf<Double?>(null) }
    val currentXMin = rememberUpdatedState(xMin)
    val currentXMax = rememberUpdatedState(xMax)

    // Precompute trace lists for high performance canvas rendering
    val graphPoints1 = remember(formula1, angleMode, xMin, xMax) {
        val pointsList = mutableListOf<Pair<Double, Double>>()
        if (xMax > xMin && formula1.isNotBlank()) {
            val steps = 300
            val stepSize = (xMax - xMin) / steps
            for (j in 0..steps) {
                val xVal = xMin + j * stepSize
                val yVal = try {
                    val parser = CalculatorParser(formula1, angleMode, mapOf("x" to xVal))
                    parser.parse().toDouble()
                } catch (e: Throwable) {
                    Double.NaN
                }
                pointsList.add(Pair(xVal, yVal))
            }
        }
        pointsList
    }

    val graphPoints2 = remember(formula2, angleMode, xMin, xMax) {
        val pointsList = mutableListOf<Pair<Double, Double>>()
        if (xMax > xMin && formula2.isNotBlank()) {
            val steps = 300
            val stepSize = (xMax - xMin) / steps
            for (j in 0..steps) {
                val xVal = xMin + j * stepSize
                val yVal = try {
                    val parser = CalculatorParser(formula2, angleMode, mapOf("x" to xVal))
                    parser.parse().toDouble()
                } catch (e: Throwable) {
                    Double.NaN
                }
                pointsList.add(Pair(xVal, yVal))
            }
        }
        pointsList
    }

    val graphPoints3 = remember(formula3, angleMode, xMin, xMax) {
        val pointsList = mutableListOf<Pair<Double, Double>>()
        if (xMax > xMin && formula3.isNotBlank()) {
            val steps = 300
            val stepSize = (xMax - xMin) / steps
            for (j in 0..steps) {
                val xVal = xMin + j * stepSize
                val yVal = try {
                    val parser = CalculatorParser(formula3, angleMode, mapOf("x" to xVal))
                    parser.parse().toDouble()
                } catch (e: Throwable) {
                    Double.NaN
                }
                pointsList.add(Pair(xVal, yVal))
            }
        }
        pointsList
    }

    // Evaluate tapped X coordinates for crosshair readout overlay
    val evaluatedTappedVals = remember(tappedXVal, formula1, formula2, formula3, angleMode) {
        val x = tappedXVal
        if (x != null && x.isFinite()) {
            val y1 = try {
                val parser = CalculatorParser(formula1, angleMode, mapOf("x" to x))
                parser.parse().toDouble()
            } catch (e: Throwable) {
                Double.NaN
            }
            val y2 = try {
                val parser = CalculatorParser(formula2, angleMode, mapOf("x" to x))
                parser.parse().toDouble()
            } catch (e: Throwable) {
                Double.NaN
            }
            val y3 = try {
                val parser = CalculatorParser(formula3, angleMode, mapOf("x" to x))
                parser.parse().toDouble()
            } catch (e: Throwable) {
                Double.NaN
            }
            Triple(y1, y2, y3)
        } else {
            null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF0F172A) // Sleek slate-900 canvas dark theme
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp)
                    .padding(8.dp)
            ) {
                // Workspace Header TopAppBar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Return home",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Plotter",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Angle warning badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF334155))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "TR_TRG: $angleMode",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                    }
                }

                // Split Pane/Layout with Form inputs & Active plots
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Equations Panel Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Active Math Formula Declarations",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF38BDF8)
                            )

                            // Formula 1
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = formula1Enabled,
                                    onCheckedChange = { formula1Enabled = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color(0xFF06B6D4),
                                        checkmarkColor = Color.Black
                                    )
                                )
                                OutlinedTextField(
                                    value = formula1,
                                    onValueChange = { formula1 = it },
                                    label = { Text("y₁ (x)", color = Color(0xFF06B6D4)) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { activeFormulaSelection = 1 },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF06B6D4),
                                        unfocusedBorderColor = Color(0xFF475569)
                                    )
                                )
                            }

                            // Formula 2
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = formula2Enabled,
                                    onCheckedChange = { formula2Enabled = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color(0xFFF59E0B),
                                        checkmarkColor = Color.Black
                                    )
                                )
                                OutlinedTextField(
                                    value = formula2,
                                    onValueChange = { formula2 = it },
                                    label = { Text("y₂ (x)", color = Color(0xFFF59E0B)) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { activeFormulaSelection = 2 },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFFF59E0B),
                                        unfocusedBorderColor = Color(0xFF475569)
                                    )
                                )
                            }

                            // Formula 3
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = formula3Enabled,
                                    onCheckedChange = { formula3Enabled = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color(0xFF10B981),
                                        checkmarkColor = Color.Black
                                    )
                                )
                                OutlinedTextField(
                                    value = formula3,
                                    onValueChange = { formula3 = it },
                                    label = { Text("y₃ (x)", color = Color(0xFF10B981)) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { activeFormulaSelection = 3 },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF10B981),
                                        unfocusedBorderColor = Color(0xFF475569)
                                    )
                                )
                            }

                            // Injected preset templates row
                            Text(
                                text = "Presets Tap Box (inserts in active y${activeFormulaSelection} slot):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(presets) { p ->
                                    Surface(
                                        onClick = {
                                            if (activeFormulaSelection == 1) formula1 = p
                                            else if (activeFormulaSelection == 2) formula2 = p
                                            else formula3 = p
                                        },
                                        color = Color(0xFF334155),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = p,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // MAIN PLOTTING CANVAS CONTAINER WITH COORDS TRACING GESTURES
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .background(Color(0xFF020617), RoundedCornerShape(16.dp))
                            .border(1.5.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        if (size.width > 0) {
                                            val xFraction = (down.position.x / size.width).toDouble()
                                            val clampedFraction = xFraction.coerceIn(0.0, 1.0)
                                            tappedXVal = currentXMin.value + clampedFraction * (currentXMax.value - currentXMin.value)
                                        }
                                        down.consume()

                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val anyDown = event.changes.any { it.pressed }
                                            if (!anyDown) break
                                            val change = event.changes.firstOrNull()
                                            if (change != null) {
                                                if (size.width > 0) {
                                                    val xFraction = (change.position.x / size.width).toDouble()
                                                    val clampedFraction = xFraction.coerceIn(0.0, 1.0)
                                                    tappedXVal = currentXMin.value + clampedFraction * (currentXMax.value - currentXMin.value)
                                                }
                                                change.consume()
                                            }
                                        }
                                    }
                                }
                        ) {
                            val gridColor = Color(0xFF334155)
                            val axisLineColor = Color(0xFF64748B)

                            // Helper scaling maps
                            val scaleX = { x: Double ->
                                val fraction = (x - xMin) / (xMax - xMin)
                                if (fraction.isFinite()) (fraction * size.width).toFloat() else 0f
                            }
                            val scaleY = { y: Double ->
                                val fraction = (y - yMin) / (yMax - yMin)
                                if (fraction.isFinite()) (size.height - fraction * size.height).toFloat() else size.height
                            }

                            // Dynamic Grid Drawing
                            if (yMax > yMin) {
                                val step = max(0.5, (yMax - yMin) / 10.0)
                                var curr = floor(yMin)
                                while (curr <= ceil(yMax)) {
                                    if (curr != 0.0) {
                                        val sy = scaleY(curr)
                                        if (sy.isFinite() && sy in 0f..size.height) {
                                            drawLine(
                                                color = gridColor.copy(alpha = 0.4f),
                                                start = Offset(0f, sy),
                                                end = Offset(size.width, sy),
                                                strokeWidth = 1.dp.toPx()
                                            )
                                        }
                                    }
                                    curr += step
                                }
                            }

                            if (xMax > xMin) {
                                val step = max(0.5, (xMax - xMin) / 10.0)
                                var curr = floor(xMin)
                                while (curr <= ceil(xMax)) {
                                    if (curr != 0.0) {
                                        val sx = scaleX(curr)
                                        if (sx.isFinite() && sx in 0f..size.width) {
                                            drawLine(
                                                color = gridColor.copy(alpha = 0.4f),
                                                start = Offset(sx, 0f),
                                                end = Offset(sx, size.height),
                                                strokeWidth = 1.dp.toPx()
                                            )
                                        }
                                    }
                                    curr += step
                                }
                            }

                            // Horizontal reference line X-Axis
                            val zeroY = scaleY(0.0)
                            if (zeroY in 0f..size.height) {
                                drawLine(
                                    color = axisLineColor,
                                    start = Offset(0f, zeroY),
                                    end = Offset(size.width, zeroY),
                                    strokeWidth = 2.dp.toPx()
                                )
                            }

                            // Vertical reference line Y-Axis
                            val zeroX = scaleX(0.0)
                            if (zeroX in 0f..size.width) {
                                drawLine(
                                    color = axisLineColor,
                                    start = Offset(zeroX, 0f),
                                    end = Offset(zeroX, size.height),
                                    strokeWidth = 2.dp.toPx()
                                )
                            }

                            // DRAW CURVE 1
                            if (formula1Enabled && graphPoints1.isNotEmpty() && xMax > xMin && yMax > yMin) {
                                val curvePath = Path()
                                var isFirstPoint = true
                                for (point in graphPoints1) {
                                    val xVal = point.first
                                    val yVal = point.second
                                    if (xVal.isFinite() && yVal.isFinite()) {
                                        val sx = scaleX(xVal)
                                        val sy = scaleY(yVal)
                                        if (sx.isFinite() && sy.isFinite() && sx in 0f..size.width && sy in 0f..size.height) {
                                            if (isFirstPoint) {
                                                curvePath.moveTo(sx, sy)
                                                isFirstPoint = false
                                            } else {
                                                curvePath.lineTo(sx, sy)
                                            }
                                        } else {
                                            isFirstPoint = true
                                        }
                                    } else {
                                        isFirstPoint = true
                                    }
                                }
                                drawPath(
                                    path = curvePath,
                                    color = Color(0xFF06B6D4), // Cyan neon curve
                                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }

                            // DRAW CURVE 2
                            if (formula2Enabled && graphPoints2.isNotEmpty() && xMax > xMin && yMax > yMin) {
                                val curvePath = Path()
                                var isFirstPoint = true
                                for (point in graphPoints2) {
                                    val xVal = point.first
                                    val yVal = point.second
                                    if (xVal.isFinite() && yVal.isFinite()) {
                                        val sx = scaleX(xVal)
                                        val sy = scaleY(yVal)
                                        if (sx.isFinite() && sy.isFinite() && sx in 0f..size.width && sy in 0f..size.height) {
                                            if (isFirstPoint) {
                                                curvePath.moveTo(sx, sy)
                                                isFirstPoint = false
                                            } else {
                                                curvePath.lineTo(sx, sy)
                                            }
                                        } else {
                                            isFirstPoint = true
                                        }
                                    } else {
                                        isFirstPoint = true
                                    }
                                }
                                drawPath(
                                    path = curvePath,
                                    color = Color(0xFFF59E0B), // Neon Orange curve
                                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }

                            // DRAW CURVE 3
                            if (formula3Enabled && graphPoints3.isNotEmpty() && xMax > xMin && yMax > yMin) {
                                val curvePath = Path()
                                var isFirstPoint = true
                                for (point in graphPoints3) {
                                    val xVal = point.first
                                    val yVal = point.second
                                    if (xVal.isFinite() && yVal.isFinite()) {
                                        val sx = scaleX(xVal)
                                        val sy = scaleY(yVal)
                                        if (sx.isFinite() && sy.isFinite() && sx in 0f..size.width && sy in 0f..size.height) {
                                            if (isFirstPoint) {
                                                curvePath.moveTo(sx, sy)
                                                isFirstPoint = false
                                            } else {
                                                curvePath.lineTo(sx, sy)
                                            }
                                        } else {
                                            isFirstPoint = true
                                        }
                                    } else {
                                        isFirstPoint = true
                                    }
                                }
                                drawPath(
                                    path = curvePath,
                                    color = Color(0xFF10B981), // Neon Emerald curve
                                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }

                            // DRAW TRACING VERTICAL DOTS INTERSECTS
                            val rawTx = tappedXVal
                            if (rawTx != null && rawTx.isFinite() && rawTx >= xMin && rawTx <= xMax) {
                                val sx = scaleX(rawTx)
                                if (sx.isFinite() && sx in 0f..size.width) {
                                    // Plot tracing vertical pointer
                                    drawLine(
                                        color = Color.White.copy(alpha = 0.5f),
                                        start = Offset(sx, 0f),
                                        end = Offset(sx, size.height),
                                        strokeWidth = 1.5.dp.toPx()
                                    )

                                    // Intersection dots
                                    evaluatedTappedVals?.let { (y1, y2, y3) ->
                                        if (formula1Enabled && y1.isFinite() && y1 >= yMin && y1 <= yMax) {
                                            val sy1 = scaleY(y1)
                                            if (sy1.isFinite() && sy1 in 0f..size.height) {
                                                drawCircle(Color.White, radius = 7.dp.toPx(), center = Offset(sx, sy1))
                                                drawCircle(Color(0xFF06B6D4), radius = 4.dp.toPx(), center = Offset(sx, sy1))
                                            }
                                        }
                                        if (formula2Enabled && y2.isFinite() && y2 >= yMin && y2 <= yMax) {
                                            val sy2 = scaleY(y2)
                                            if (sy2.isFinite() && sy2 in 0f..size.height) {
                                                drawCircle(Color.White, radius = 7.dp.toPx(), center = Offset(sx, sy2))
                                                drawCircle(Color(0xFFF59E0B), radius = 4.dp.toPx(), center = Offset(sx, sy2))
                                            }
                                        }
                                        if (formula3Enabled && y3.isFinite() && y3 >= yMin && y3 <= yMax) {
                                            val sy3 = scaleY(y3)
                                            if (sy3.isFinite() && sy3 in 0f..size.height) {
                                                drawCircle(Color.White, radius = 7.dp.toPx(), center = Offset(sx, sy3))
                                                drawCircle(Color(0xFF10B981), radius = 4.dp.toPx(), center = Offset(sx, sy3))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Floating dynamic trace overlay card
                        evaluatedTappedVals?.let { (y1, y2, y3) ->
                            val tx = tappedXVal
                            if (tx != null) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(12.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xE60F172A))
                                        .border(1.dp, Color(0xFF475569), RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text("TRACE COORDS", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Text("x: ${String.format(Locale.US, "%.4f", tx)}", color = Color.White, fontSize = 11.sp)
                                        if (formula1Enabled) {
                                            Text(
                                                text = "y₁: ${if (y1.isFinite()) String.format(Locale.US, "%.4f", y1) else "Undefined"}",
                                                color = Color(0xFF22D3EE),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        if (formula2Enabled) {
                                            Text(
                                                text = "y₂: ${if (y2.isFinite()) String.format(Locale.US, "%.4f", y2) else "Undefined"}",
                                                color = Color(0xFFFBBF24),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        if (formula3Enabled) {
                                            Text(
                                                text = "y₃: ${if (y3.isFinite()) String.format(Locale.US, "%.4f", y3) else "Undefined"}",
                                                color = Color(0xFF34D399),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Bottom right helper tips
                        Text(
                            text = "Tap & drag to slide tracing target",
                            color = Color(0xFF64748B),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(10.dp)
                        )
                    }

                    // INTERACTIVE ZOOM & NAVIGATION PADS TOOLBAR
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E293B))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Interactive Precision Navigation Dashboard",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF94A3B8)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Zoom buttons
                            Button(
                                onClick = {
                                    val currentXSpan = xMax - xMin
                                    val centerX = xMin + currentXSpan / 2.0
                                    val newHalfSpan = (currentXSpan * 0.6) / 2.0
                                    xMinStr = String.format(Locale.US, "%.3f", centerX - newHalfSpan)
                                    xMaxStr = String.format(Locale.US, "%.3f", centerX + newHalfSpan)

                                    val currentYSpan = yMax - yMin
                                    val centerY = yMin + currentYSpan / 2.0
                                    val newYHalfSpan = (currentYSpan * 0.6) / 2.0
                                    yMinStr = String.format(Locale.US, "%.3f", centerY - newYHalfSpan)
                                    yMaxStr = String.format(Locale.US, "%.3f", centerY + newYHalfSpan)
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                            ) {
                                Text("🔍 Zoom In [+]", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val currentXSpan = xMax - xMin
                                    val centerX = xMin + currentXSpan / 2.0
                                    val newHalfSpan = (currentXSpan * 1.5) / 2.0
                                    xMinStr = String.format(Locale.US, "%.3f", centerX - newHalfSpan)
                                    xMaxStr = String.format(Locale.US, "%.3f", centerX + newHalfSpan)

                                    val currentYSpan = yMax - yMin
                                    val centerY = yMin + currentYSpan / 2.0
                                    val newYHalfSpan = (currentYSpan * 1.5) / 2.0
                                    yMinStr = String.format(Locale.US, "%.3f", centerY - newYHalfSpan)
                                    yMaxStr = String.format(Locale.US, "%.3f", centerY + newYHalfSpan)
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                            ) {
                                Text("🔎 Zoom Out [-]", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            // Reset standard viewport [-10, 10]
                            Button(
                                onClick = {
                                    xMinStr = "-10"
                                    xMaxStr = "10"
                                    yMinStr = "-10"
                                    yMaxStr = "10"
                                    tappedXVal = null
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569))
                            ) {
                                Text("↺ Reset View", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        // Shift/Pan buttons triggers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val panButton = @Composable { label: String, onClick: () -> Unit ->
                                Button(
                                    onClick = onClick,
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(vertical = 4.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                                ) {
                                    Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            panButton("◀ Left") {
                                val step = (xMax - xMin) * 0.25
                                xMinStr = String.format(Locale.US, "%.3f", xMin - step)
                                xMaxStr = String.format(Locale.US, "%.3f", xMax - step)
                            }
                            panButton("▶ Right") {
                                val step = (xMax - xMin) * 0.25
                                xMinStr = String.format(Locale.US, "%.3f", xMin + step)
                                xMaxStr = String.format(Locale.US, "%.3f", xMax + step)
                            }
                            panButton("▲ Up") {
                                val step = (yMax - yMin) * 0.25
                                yMinStr = String.format(Locale.US, "%.3f", yMin + step)
                                yMaxStr = String.format(Locale.US, "%.3f", yMax + step)
                            }
                            panButton("▼ Down") {
                                val step = (yMax - yMin) * 0.25
                                yMinStr = String.format(Locale.US, "%.3f", yMin - step)
                                yMaxStr = String.format(Locale.US, "%.3f", yMax - step)
                            }
                        }
                    }

                    // MANUAL INPUTS CARD
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = xMinStr,
                                onValueChange = { xMinStr = it },
                                label = { Text("xMin", fontSize = 11.sp, color = Color(0xFF94A3B8)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = Color(0xFF475569)
                                )
                            )
                            OutlinedTextField(
                                value = xMaxStr,
                                onValueChange = { xMaxStr = it },
                                label = { Text("xMax", fontSize = 11.sp, color = Color(0xFF94A3B8)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = Color(0xFF475569)
                                )
                            )
                            OutlinedTextField(
                                value = yMinStr,
                                onValueChange = { yMinStr = it },
                                label = { Text("yMin", fontSize = 11.sp, color = Color(0xFF94A3B8)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = Color(0xFF475569)
                                )
                            )
                            OutlinedTextField(
                                value = yMaxStr,
                                onValueChange = { yMaxStr = it },
                                label = { Text("yMax", fontSize = 11.sp, color = Color(0xFF94A3B8)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = Color(0xFF475569)
                                )
                            )
                        }
                    }

                    // Bottom exit actions
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2E8F0))
                    ) {
                        Text("Return to Scientific Multi-Mode Keypad", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun WorksheetDialog(
    type: String,
    onDismiss: () -> Unit,
    onInsertResult: (String) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val dismissWithKeyboard = {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        onDismiss()
    }

    // Worksheet States
    var calExpr by remember { mutableStateOf("x^2 - 3*x + 2") }
    var calX by remember { mutableStateOf("2.0") }
    var calA by remember { mutableStateOf("0.0") }
    var calB by remember { mutableStateOf("1.0") }

    // Matrix
    var m00 by remember { mutableStateOf("1") }; var m01 by remember { mutableStateOf("2") }; var m02 by remember { mutableStateOf("3") }
    var m10 by remember { mutableStateOf("0") }; var m11 by remember { mutableStateOf("1") }; var m12 by remember { mutableStateOf("4") }
    var m20 by remember { mutableStateOf("5") }; var m21 by remember { mutableStateOf("6") }; var m22 by remember { mutableStateOf("0") }
    var vec1x by remember { mutableStateOf("1") }; var vec1y by remember { mutableStateOf("2") }; var vec1z by remember { mutableStateOf("3") }
    var vec2x by remember { mutableStateOf("4") }; var vec2y by remember { mutableStateOf("5") }; var vec2z by remember { mutableStateOf("6") }

    // Complex
    var compReal by remember { mutableStateOf("1.0") }
    var compImag by remember { mutableStateOf("1.0") }
    var compR by remember { mutableStateOf("1.4142") }
    var compTheta by remember { mutableStateOf("45.0") }
    var compPower by remember { mutableStateOf("3") }

    // TVM & finance
    var tvmPV by remember { mutableStateOf("1000") }
    var tvmFV by remember { mutableStateOf("") }
    var tvmPMT by remember { mutableStateOf("50") }
    var tvmRate by remember { mutableStateOf("5") }
    var tvmN by remember { mutableStateOf("10") }
    var financeCFs by remember { mutableStateOf("100,200,300,400") }

    // Special & Stats
    var specX by remember { mutableStateOf("5.0") }
    var statXList by remember { mutableStateOf("10, 20, 30, 40, 50") }
    var statYList by remember { mutableStateOf("12, 18, 31, 38, 52") }
    var regXList by remember { mutableStateOf("1, 2, 3, 4, 5") }
    var regYList by remember { mutableStateOf("2.1, 3.9, 6.1, 8.0, 9.9") }

    // CAS
    var casTerm1 by remember { mutableStateOf("x+2") }
    var casTerm2 by remember { mutableStateOf("x-3") }

    // Fraction
    var fracDec by remember { mutableStateOf("2.333333333") }

    var resultText by remember { mutableStateOf("") }
    var insertValue by remember { mutableStateOf("") }

    fun buildMatrix(): Matrix {
        return Matrix(
            arrayOf(
                doubleArrayOf(m00.toDoubleOrNull() ?: 1.0, m01.toDoubleOrNull() ?: 0.0, m02.toDoubleOrNull() ?: 0.0),
                doubleArrayOf(m10.toDoubleOrNull() ?: 0.0, m11.toDoubleOrNull() ?: 1.0, m12.toDoubleOrNull() ?: 0.0),
                doubleArrayOf(m20.toDoubleOrNull() ?: 0.0, m21.toDoubleOrNull() ?: 0.0, m22.toDoubleOrNull() ?: 1.0)
            )
        )
    }

    fun buildVec1(): VectorMath {
        return VectorMath(
            doubleArrayOf(
                vec1x.toDoubleOrNull() ?: 0.0,
                vec1y.toDoubleOrNull() ?: 0.0,
                vec1z.toDoubleOrNull() ?: 0.0
            )
        )
    }

    fun buildVec2(): VectorMath {
        return VectorMath(
            doubleArrayOf(
                vec2x.toDoubleOrNull() ?: 0.0,
                vec2y.toDoubleOrNull() ?: 0.0,
                vec2z.toDoubleOrNull() ?: 0.0
            )
        )
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color(0xFF0F172A),
        unfocusedTextColor = Color(0xFF0F172A),
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        focusedBorderColor = Color(0xFF1E40AF),
        unfocusedBorderColor = Color(0xFF94A3B8)
    )

    Dialog(
        onDismissRequest = dismissWithKeyboard,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF1F5F9)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp)
            ) {
                // High-End Workspace Top Row bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = dismissWithKeyboard) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF0F172A)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = when(type) {
                            "calculus" -> "Calculus & Analysis"
                            "linalg" -> "Linear Algebra Studio"
                            "complex" -> "Complex Numbers & DeMoivre"
                            "regression" -> "Linear Regression Studio"
                            "special" -> "Special Functions & Stats"
                            "finance" -> "Financial TVM Ledger"
                            "cas" -> "Symbolic CAS"
                            "fraction" -> "Fractions & Decimals Converter"
                            else -> "Advanced Worksheet"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (insertValue.isNotEmpty()) {
                        Button(
                            onClick = { onInsertResult(insertValue) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E40AF)),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Insert result", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                // Scrollable main content container taking full horizontal width!
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when(type) {
                    "calculus" -> {
                        Text("Compute analysis operations on your formula:", color = Color(0xFF475569), fontSize = 12.sp)
                        OutlinedTextField(value = calExpr, onValueChange = { calExpr = it }, label = { Text("Formula f(x)") }, colors = fieldColors)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = calX, onValueChange = { calX = it }, label = { Text("x point") }, modifier = Modifier.weight(1f), colors = fieldColors)
                            OutlinedTextField(value = calA, onValueChange = { calA = it }, label = { Text("Bound a") }, modifier = Modifier.weight(1f), colors = fieldColors)
                            OutlinedTextField(value = calB, onValueChange = { calB = it }, label = { Text("Bound b") }, modifier = Modifier.weight(1f), colors = fieldColors)
                        }
                        
                        // Action buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    val x = calX.toDoubleOrNull() ?: 1.0
                                    val d = Calculus.numericalDerivative(calExpr, x)
                                    resultText = "f'($x) = " + String.format(Locale.US, "%.6f", d)
                                    insertValue = d.toString()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF)),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Text("Derivative", color = Color(0xFF1D4ED8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            
                            Button(
                                onClick = {
                                    val a = calA.toDoubleOrNull() ?: 0.0
                                    val b = calB.toDoubleOrNull() ?: 1.0
                                    val d = Calculus.numericalIntegration(calExpr, a, b)
                                    resultText = "∫ f(x) dx = " + String.format(Locale.US, "%.6f", d)
                                    insertValue = d.toString()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF)),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Text("Integrate", color = Color(0xFF1D4ED8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    val x = calX.toDoubleOrNull() ?: 1.0
                                    val left = Calculus.evaluateLimit(calExpr, x, true)
                                    val right = Calculus.evaluateLimit(calExpr, x, false)
                                    resultText = "Limit x->$x\nLeft: $left\nRight: $right"
                                    insertValue = left.toString()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF)),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Text("Limits", color = Color(0xFF1D4ED8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    resultText = "Taylor sin(x):\n" + Calculus.buildTaylorExpansionString("sin", 4)
                                    insertValue = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF)),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Text("Taylor Expansion", color = Color(0xFF1D4ED8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    "linalg" -> {
                        Text("Matrix Input (3x3 default, empty cells use 0):", color = Color(0xFF475569), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                OutlinedTextField(value = m00, onValueChange = { m00 = it }, modifier = Modifier.weight(1f), colors = fieldColors, singleLine = true)
                                OutlinedTextField(value = m01, onValueChange = { m01 = it }, modifier = Modifier.weight(1f), colors = fieldColors, singleLine = true)
                                OutlinedTextField(value = m02, onValueChange = { m02 = it }, modifier = Modifier.weight(1f), colors = fieldColors, singleLine = true)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                OutlinedTextField(value = m10, onValueChange = { m10 = it }, modifier = Modifier.weight(1f), colors = fieldColors, singleLine = true)
                                OutlinedTextField(value = m11, onValueChange = { m11 = it }, modifier = Modifier.weight(1f), colors = fieldColors, singleLine = true)
                                OutlinedTextField(value = m12, onValueChange = { m12 = it }, modifier = Modifier.weight(1f), colors = fieldColors, singleLine = true)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                OutlinedTextField(value = m20, onValueChange = { m20 = it }, modifier = Modifier.weight(1f), colors = fieldColors, singleLine = true)
                                OutlinedTextField(value = m21, onValueChange = { m21 = it }, modifier = Modifier.weight(1f), colors = fieldColors, singleLine = true)
                                OutlinedTextField(value = m22, onValueChange = { m22 = it }, modifier = Modifier.weight(1f), colors = fieldColors, singleLine = true)
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = {
                                    val mat = buildMatrix()
                                    try {
                                        val det = mat.determinant()
                                        resultText = "Determinant = $det"
                                        insertValue = det.toString()
                                    } catch (e: Exception) {
                                        resultText = "Error: " + e.message
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Det", color = Color(0xFF1D4ED8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            
                            Button(
                                onClick = {
                                    val mat = buildMatrix()
                                    try {
                                        val inv = mat.invert()
                                        resultText = "Inverse Matrix:\n$inv"
                                        insertValue = ""
                                    } catch (e: Exception) {
                                        resultText = "Error: " + e.message
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Invert", color = Color(0xFF1D4ED8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val mat = buildMatrix()
                                    try {
                                        val rref = mat.rref()
                                        resultText = "Gauss-Jordan RREF:\n$rref"
                                        insertValue = ""
                                    } catch (e: Exception) {
                                        resultText = "Error: " + e.message
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("RREF", color = Color(0xFF1D4ED8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Vectors Studio (3D):", color = Color(0xFF475569), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedTextField(value = vec1x, onValueChange = { vec1x = it }, label = { Text("V1.x") }, modifier = Modifier.weight(1f), colors = fieldColors, singleLine = true)
                            OutlinedTextField(value = vec1y, onValueChange = { vec1y = it }, label = { Text("V1.y") }, modifier = Modifier.weight(1f), colors = fieldColors, singleLine = true)
                            OutlinedTextField(value = vec1z, onValueChange = { vec1z = it }, label = { Text("V1.z") }, modifier = Modifier.weight(1f), colors = fieldColors, singleLine = true)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedTextField(value = vec2x, onValueChange = { vec2x = it }, label = { Text("V2.x") }, modifier = Modifier.weight(1f), colors = fieldColors, singleLine = true)
                            OutlinedTextField(value = vec2y, onValueChange = { vec2y = it }, label = { Text("V2.y") }, modifier = Modifier.weight(1f), colors = fieldColors, singleLine = true)
                            OutlinedTextField(value = vec2z, onValueChange = { vec2z = it }, label = { Text("V2.z") }, modifier = Modifier.weight(1f), colors = fieldColors, singleLine = true)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = {
                                    val v1 = buildVec1()
                                    val v2 = buildVec2()
                                    val d = v1.dot(v2)
                                    resultText = "Dot Product = $d"
                                    insertValue = d.toString()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Dot", color = Color(0xFF1D4ED8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val v1 = buildVec1()
                                    val v2 = buildVec2()
                                    val cross = v1.cross3D(v2)
                                    resultText = "Cross Product = [${cross.data[0]}, ${cross.data[1]}, ${cross.data[2]}]"
                                    insertValue = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cross", color = Color(0xFF1D4ED8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val v1 = buildVec1()
                                    val v2 = buildVec2()
                                    val a = v1.angleWith(v2)
                                    resultText = "Angle = " + String.format(Locale.US, "%.2f°", a)
                                    insertValue = a.toString()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Angle", color = Color(0xFF1D4ED8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    "complex" -> {
                        Text("Complex Values (Cartesian & Polar):", color = Color(0xFF475569), fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(value = compReal, onValueChange = { compReal = it }, label = { Text("Real (a)") }, modifier = Modifier.weight(1f), colors = fieldColors, singleLine = true)
                            OutlinedTextField(value = compImag, onValueChange = { compImag = it }, label = { Text("Imag (b)") }, modifier = Modifier.weight(1f), colors = fieldColors, singleLine = true)
                        }
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = {
                                    val r = compReal.toDoubleOrNull() ?: 1.0
                                    val i = compImag.toDoubleOrNull() ?: 0.0
                                    val c = Complex(r, i)
                                    resultText = "Polar Form:\n" + c.formatPolar()
                                    insertValue = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("to Polar", color = Color(0xFF1D4ED8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val r = compReal.toDoubleOrNull() ?: 1.0
                                    val i = compImag.toDoubleOrNull() ?: 0.0
                                    val c = Complex(r, i)
                                    val p = compPower.toDoubleOrNull() ?: 2.0
                                    val solved = c.pow(p)
                                    resultText = "($c)^$p = $solved"
                                    insertValue = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("De Moivre Power", color = Color(0xFF1D4ED8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = {
                                    val r = compReal.toDoubleOrNull() ?: 1.0
                                    val i = compImag.toDoubleOrNull() ?: 0.0
                                    val c = Complex(r, i)
                                    val solved = Complex.sinComplex(c)
                                    resultText = "sin($c) = $solved"
                                    insertValue = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Complex sin()", color = Color(0xFF1D4ED8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val r = compReal.toDoubleOrNull() ?: 1.0
                                    val i = compImag.toDoubleOrNull() ?: 0.0
                                    val c = Complex(r, i)
                                    val solved = Complex.logComplex(c)
                                    resultText = "ln($c) = $solved"
                                    insertValue = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Complex ln()", color = Color(0xFF1D4ED8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedTextField(value = compPower, onValueChange = { compPower = it }, label = { Text("Power exponent p") }, colors = fieldColors, singleLine = true)
                    }

                    "regression" -> {
                        Text("Calculate slope, intercept, and correlation coefficient (r) using linear regression:", color = Color(0xFF475569), fontSize = 13.sp)
                        OutlinedTextField(
                            value = regXList,
                            onValueChange = { regXList = it },
                            label = { Text("X Values (separated by commas, spaces, or semicolons)") },
                            colors = fieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = regYList,
                            onValueChange = { regYList = it },
                            label = { Text("Y Values (separated by commas, spaces, or semicolons)") },
                            colors = fieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = {
                                    try {
                                        val xs = regXList.split(Regex("[,\\s;]+")).filter { it.isNotEmpty() }.map { it.trim().toDoubleOrNull() ?: throw IllegalArgumentException("Invalid X value") }
                                        val ys = regYList.split(Regex("[,\\s;]+")).filter { it.isNotEmpty() }.map { it.trim().toDoubleOrNull() ?: throw IllegalArgumentException("Invalid Y value") }
                                        if (xs.size != ys.size) {
                                            resultText = "Count mismatch: X has ${xs.size} values, Y has ${ys.size} values. They must match."
                                            insertValue = ""
                                        } else if (xs.size < 2) {
                                            resultText = "At least 2 points are required for regression."
                                            insertValue = ""
                                        } else {
                                            val stats = Statistics.linearRegression(xs, ys)
                                            resultText = stats
                                            val n = minOf(xs.size, ys.size)
                                            val meanX = xs.sum() / n
                                            val meanY = ys.sum() / n
                                            var num = 0.0
                                            var denX = 0.0
                                            for (i in 0 until n) {
                                                num += (xs[i] - meanX) * (ys[i] - meanY)
                                                denX += (xs[i] - meanX).pow(2)
                                            }
                                            val m = if (denX != 0.0) num / denX else 0.0
                                            insertValue = String.format(Locale.US, "%.5f", m) // slope is a very useful default insert
                                        }
                                    } catch (e: Exception) {
                                        resultText = "Parsing Error: Ensure all entries are valid numbers."
                                        insertValue = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E40AF)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Compute Fit Analysis", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            
                            Button(
                                onClick = {
                                    regXList = "1, 2, 3, 4, 5"
                                    regYList = "2.2, 3.8, 6.1, 7.9, 10.2"
                                    resultText = ""
                                    insertValue = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Load Sample", color = Color(0xFF1D4ED8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    "special" -> {
                        Text("Bessel, Gamma & Statistics Engines:", color = Color(0xFF475569), fontSize = 12.sp)
                        OutlinedTextField(value = specX, onValueChange = { specX = it }, label = { Text("Input Variable x") }, colors = fieldColors, singleLine = true)
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = {
                                    val x = specX.toDoubleOrNull() ?: 1.0
                                    try {
                                        val g = SpecialMath.gamma(x)
                                        resultText = "Γ($x) = " + String.format(Locale.US, "%.5f", g)
                                        insertValue = g.toString()
                                    } catch (e: Exception) {
                                        resultText = "Error in gamma"
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Gamma Γ(x)", color = Color(0xFF1D4ED8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val x = specX.toDoubleOrNull() ?: 1.0
                                    val j = SpecialMath.besselJ0(x)
                                    resultText = "J₀($x) = " + String.format(Locale.US, "%.5f", j)
                                    insertValue = j.toString()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Bessel J₀(x)", color = Color(0xFF1D4ED8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val x = specX.toDoubleOrNull() ?: 1.0
                                    val cdf = Statistics.normalCDF(x)
                                    resultText = "Normal CDF z<$x: " + String.format(Locale.US, "%.5f", cdf)
                                    insertValue = cdf.toString()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Normal CDF", color = Color(0xFF1D4ED8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Statistical Grouped Dataset Analysis:", color = Color(0xFF475569), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        OutlinedTextField(value = statXList, onValueChange = { statXList = it }, label = { Text("Data items (comma separated)") }, colors = fieldColors)
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = {
                                    val items = statXList.split(",").map { it.trim().toDoubleOrNull() ?: 0.0 }
                                    val stats = Statistics.descriptiveStats(items)
                                    resultText = stats.entries.joinToString("\n") { "${it.key}: " + String.format(Locale.US, "%.4f", it.value) }
                                    insertValue = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Descriptive Stats", color = Color(0xFF1D4ED8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    try {
                                        val xs = statXList.split(",").map { it.trim().toDoubleOrNull() ?: 0.0 }
                                        val ys = statYList.split(",").map { it.trim().toDoubleOrNull() ?: 0.0 }
                                        val reg = Statistics.linearRegression(xs, ys)
                                        resultText = "Linear Regression:\n$reg"
                                        insertValue = ""
                                    } catch (e: Exception) {
                                        resultText = "Count mismatch in datasets"
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Linear Fit (X vs Y)", color = Color(0xFF1D4ED8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        OutlinedTextField(value = statYList, onValueChange = { statYList = it }, label = { Text("Data items Y (for fit)") }, colors = fieldColors)
                    }

                    "finance" -> {
                        Text("TVM Input (Leave field blank, click calculate to solve):", color = Color(0xFF475569), fontSize = 12.sp)
                        OutlinedTextField(value = tvmPV, onValueChange = { tvmPV = it }, label = { Text("Present Value (PV)") }, colors = fieldColors, singleLine = true)
                        OutlinedTextField(value = tvmFV, onValueChange = { tvmFV = it }, label = { Text("Future Value (FV)") }, colors = fieldColors, singleLine = true)
                        OutlinedTextField(value = tvmPMT, onValueChange = { tvmPMT = it }, label = { Text("Periodic PMT") }, colors = fieldColors, singleLine = true)
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(value = tvmRate, onValueChange = { tvmRate = it }, label = { Text("Rate I/Y %") }, modifier = Modifier.weight(1f), colors = fieldColors, singleLine = true)
                            OutlinedTextField(value = tvmN, onValueChange = { tvmN = it }, label = { Text("Periods (N)") }, modifier = Modifier.weight(1f), colors = fieldColors, singleLine = true)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = {
                                    val pv = tvmPV.toDoubleOrNull() ?: 0.0
                                    val pmt = tvmPMT.toDoubleOrNull() ?: 0.0
                                    val rate = tvmRate.toDoubleOrNull() ?: 5.0
                                    val n = tvmN.toDoubleOrNull() ?: 10.0
                                    val solved = TVM.solveFV(pv, pmt, rate, n)
                                    tvmFV = String.format(Locale.US, "%.2f", solved)
                                    resultText = "Solved FV = $tvmFV"
                                    insertValue = solved.toString()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Compute FV", color = Color(0xFF1D4ED8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val fv = tvmFV.toDoubleOrNull() ?: 0.0
                                    val pmt = tvmPMT.toDoubleOrNull() ?: 0.0
                                    val rate = tvmRate.toDoubleOrNull() ?: 5.0
                                    val n = tvmN.toDoubleOrNull() ?: 10.0
                                    val solved = TVM.solvePV(fv, pmt, rate, n)
                                    tvmPV = String.format(Locale.US, "%.2f", solved)
                                    resultText = "Solved PV = $tvmPV"
                                    insertValue = solved.toString()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Compute PV", color = Color(0xFF1D4ED8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text("NPV Cashflow Series Ledger:", color = Color(0xFF475569), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        OutlinedTextField(value = financeCFs, onValueChange = { financeCFs = it }, label = { Text("Periodic Flows (e.g. 100, 200, ...)") }, colors = fieldColors)
                        
                        Button(
                            onClick = {
                                val flows = financeCFs.split(",").map { it.trim().toDoubleOrNull() ?: 0.0 }
                                val discountRate = tvmRate.toDoubleOrNull() ?: 5.0
                                val initialInvestment = tvmPV.toDoubleOrNull() ?: 0.0
                                val npvVal = TVM.npv(discountRate, initialInvestment, flows)
                                resultText = "NPV = " + String.format(Locale.US, "%.4f", npvVal)
                                insertValue = npvVal.toString()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Compute Net Present Value (NPV)", color = Color(0xFF1D4ED8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    "cas" -> {
                        Text("Symbolic Expansion Generator:", color = Color(0xFF475569), fontSize = 12.sp)
                        OutlinedTextField(value = casTerm1, onValueChange = { casTerm1 = it }, label = { Text("Factor Term 1") }, colors = fieldColors, singleLine = true)
                        OutlinedTextField(value = casTerm2, onValueChange = { casTerm2 = it }, label = { Text("Factor Term 2") }, colors = fieldColors, singleLine = true)
                        
                        Button(
                            onClick = {
                                val res = CAS.simplifyAlgebraicProduct(casTerm1, casTerm2)
                                resultText = "Expanded CAS result:\n$res"
                                insertValue = res
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Solve Expand (T1 * T2)", color = Color(0xFF1D4ED8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    "fraction" -> {
                        Text("Fraction Precision Engine & Repeating Loops:", color = Color(0xFF475569), fontSize = 12.sp)
                        OutlinedTextField(value = fracDec, onValueChange = { fracDec = it }, label = { Text("Decimal value to convert") }, colors = fieldColors, singleLine = true)
                        
                        Button(
                            onClick = {
                                val d = fracDec.toDoubleOrNull() ?: 0.0
                                try {
                                    val f = Fraction.fromDouble(d)
                                    val mixed = f.toMixedString()
                                    val rep = toRepeatingDecimal(d)
                                    resultText = "Proper/Improper: $f\nMixed Number: $mixed\nRepeating Decimal: $rep"
                                    insertValue = f.toString()
                                } catch (e: Exception) {
                                    resultText = "Conversion Error"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Convert decimal to Fractions & Repeating", color = Color(0xFF1D4ED8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                    if (resultText.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White, RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                .padding(16.dp)
                        ) {
                            Column {
                                Text("Result Output", color = Color(0xFF1E40AF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = resultText, color = Color(0xFF1E40AF), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = dismissWithKeyboard,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64748B)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Close Worksheet", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Helper matrices and vectors generators for Worksheet logic above
private fun WorksheetDialog_buildMatrix_cell(valStr: String): Double {
    return valStr.toDoubleOrNull() ?: 0.0
}

