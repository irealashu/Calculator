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
    var activeTab by remember { mutableStateOf("TRIG") }
    
    // Dynamic Theme state
    var currentTheme by remember { mutableStateOf(ThemesList[0]) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showGrapherDialog by remember { mutableStateOf(false) }

    // Dialog/Form triggers
    var showSolverDialog by remember { mutableStateOf<String?>(null) } // "QUAD", "SYSTEM"
    var showConversionDialog by remember { mutableStateOf(false) }
    var showVariableStoreDialog by remember { mutableStateOf<String?>(null) } // Variable name selected

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
                .padding(16.dp)
        ) {
            // Mode Banner (DEG status, base status, scientific triggers)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Clicking Angle Mode toggles DEG/RAD
                Button(
                    onClick = {
                        triggerVibe(CalcKey.Digit("0"))
                        viewModel.toggleAngleMode()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = actionColor),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = angleMode,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = functionalTextColor
                    )
                }

                Spacer(modifier = Modifier.weight(1.0f))

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

                // Dynamic palette trigger
                IconButton(
                    onClick = {
                        triggerVibe(CalcKey.Digit("0"))
                        showThemeDialog = true
                    },
                    modifier = Modifier
                        .background(actionColor, RoundedCornerShape(12.dp))
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = "Theme Palette Dialog",
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

            // Scrollable ribbon menu
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val tabs = listOf("TRIG", "MATH", "PRB", "MEMORY", "SOLVERS", "CONV", "CONST")
                items(tabs) { tab ->
                    val isActive = tab == activeTab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isActive) operatorColor else actionColor)
                            .clickable {
                                triggerVibe(CalcKey.Digit("0"))
                                activeTab = tab
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = tab,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) operatorTextColor else expressionTextColor
                        )
                    }
                }
            }

            // Active Tab Ribbon Ribbon Helpers
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                when (activeTab) {
                    "TRIG" -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val list = listOf(
                                "sin(" to "sin", "cos(" to "cos", "tan(" to "tan",
                                "asin(" to "sin⁻¹", "acos(" to "cos⁻¹", "atan(" to "tan⁻¹",
                                "sinh(" to "sinh", "cosh(" to "cosh", "tanh(" to "tanh"
                            )
                            list.forEach { item ->
                                RibbonButton(theme = currentTheme, label = item.second, onClick = { viewModel.onFunctionClick(item.first) })
                            }
                        }
                    }
                    "MATH" -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val list = listOf(
                                "abs(" to "abs", "gcd(" to "gcd", "lcm(" to "lcm",
                                "mod(" to "mod", "round(" to "round", "iPart(" to "iPart",
                                "fPart(" to "fPart", "int(" to "int", "min(" to "min", "max(" to "max"
                            )
                            list.forEach { item ->
                                RibbonButton(theme = currentTheme, label = item.second, onClick = { viewModel.onFunctionClick(item.first) })
                            }
                        }
                    }
                    "PRB" -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RibbonButton(theme = currentTheme, label = "nCr", onClick = { viewModel.onOperatorClick("nCr") })
                            RibbonButton(theme = currentTheme, label = "nPr", onClick = { viewModel.onOperatorClick("nPr") })
                            RibbonButton(theme = currentTheme, label = "Factorial (x!)", onClick = { viewModel.onDigitClick("!") })
                        }
                    }
                    "MEMORY" -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("x", "y", "z", "t", "a", "b", "c", "d").forEach { variable ->
                                RibbonButton(theme = currentTheme, label = "RCL $variable", onClick = { viewModel.onDigitClick(variable) })
                                RibbonButton(theme = currentTheme, label = "STO $variable", onClick = { showVariableStoreDialog = variable })
                            }
                            RibbonButton(theme = currentTheme, label = "Clear Mem", onClick = { viewModel.clearVariables() })
                        }
                    }
                    "SOLVERS" -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RibbonButton(theme = currentTheme, label = "Quadratic Solver (ax²+bx+c=0)", onClick = { showSolverDialog = "QUAD" })
                            RibbonButton(theme = currentTheme, label = "2x2 Linear System", onClick = { showSolverDialog = "SYSTEM" })
                        }
                    }
                    "CONV" -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RibbonButton(theme = currentTheme, label = "Conversion Menu", onClick = { showConversionDialog = true })
                        }
                    }
                    "CONST" -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val list = listOf(
                                "299792458" to "c (speed of light)",
                                "9.80665" to "g (gravity)",
                                "6.62607e-34" to "h (Planck)",
                                "6.02214e23" to "NA (Avogadro)",
                                "8.31447" to "R (Universal Gas)",
                                "1.60218e-19" to "e (electron charge)"
                            )
                            list.forEach { item ->
                                RibbonButton(theme = currentTheme, label = item.second, onClick = { viewModel.onConstantClick(item.first) })
                            }
                        }
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

        // Dynamic Palette Selection Modal
        if (showThemeDialog) {
            ThemePickerDialog(
                currentTheme = currentTheme,
                onSelectTheme = { currentTheme = it },
                onDismiss = { showThemeDialog = false }
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

    AlertDialog(
        onDismissRequest = dismissWithKeyboard,
        title = {
            Text(
                text = if (type == "QUAD") "Quadratic Equation Solver" else "2x2 Linear System Solver",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (type == "QUAD") {
                    Text("Solves ax² + bx + c = 0", color = Color(0xFF475569), fontSize = 13.sp)
                    
                    OutlinedTextField(
                        value = a, onValueChange = { a = it }, label = { Text("Coefficient a") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedBorderColor = Color(0xFF1E40AF),
                            unfocusedBorderColor = Color(0xFF94A3B8)
                        )
                    )
                    OutlinedTextField(
                        value = b, onValueChange = { b = it }, label = { Text("Coefficient b") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedBorderColor = Color(0xFF1E40AF),
                            unfocusedBorderColor = Color(0xFF94A3B8)
                        )
                    )
                    OutlinedTextField(
                        value = c, onValueChange = { c = it }, label = { Text("Coefficient c") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedBorderColor = Color(0xFF1E40AF),
                            unfocusedBorderColor = Color(0xFF94A3B8)
                        )
                    )
                } else {
                    Text("Solves a1·x + b1·y = c1 and a2·x + b2·y = c2", color = Color(0xFF475569), fontSize = 13.sp)
                    
                    Text("Equation 1", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    OutlinedTextField(
                        value = a1, onValueChange = { a1 = it }, label = { Text("a1") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color(0xFF0F172A), unfocusedTextColor = Color(0xFF0F172A))
                    )
                    OutlinedTextField(
                        value = b1, onValueChange = { b1 = it }, label = { Text("b1") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color(0xFF0F172A), unfocusedTextColor = Color(0xFF0F172A))
                    )
                    OutlinedTextField(
                        value = c1, onValueChange = { c1 = it }, label = { Text("c1") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color(0xFF0F172A), unfocusedTextColor = Color(0xFF0F172A))
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Equation 2", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    OutlinedTextField(
                        value = a2, onValueChange = { a2 = it }, label = { Text("a2") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color(0xFF0F172A), unfocusedTextColor = Color(0xFF0F172A))
                    )
                    OutlinedTextField(
                        value = b2, onValueChange = { b2 = it }, label = { Text("b2") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color(0xFF0F172A), unfocusedTextColor = Color(0xFF0F172A))
                    )
                    OutlinedTextField(
                        value = c2, onValueChange = { c2 = it }, label = { Text("c2") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color(0xFF0F172A), unfocusedTextColor = Color(0xFF0F172A))
                    )
                }

                if (resultText.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFEFF6FF), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(text = resultText, color = Color(0xFF1E40AF), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E40AF))
                ) {
                    Text("Solve", color = Color.White)
                }

                if (primaryRoot.isNotEmpty()) {
                    Button(
                        onClick = { 
                            keyboardController?.hide()
                            focusManager.clearFocus(force = true)
                            onSolveRealRoot(primaryRoot) 
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Text("Use Root", color = Color.White)
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = dismissWithKeyboard) {
                Text("Dismiss", color = Color(0xFF1E40AF))
            }
        },
        containerColor = Color.White
    )
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

@Composable
fun ConversionDialog(
    onDismiss: () -> Unit,
    onSelectConversionResult: (String) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val dismissWithKeyboard = {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        onDismiss()
    }

    var inputValue by remember { mutableStateOf("100") }
    var resultConverted by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = dismissWithKeyboard,
        title = {
            Text("Scientific Unit Converter", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputValue,
                    onValueChange = { inputValue = it },
                    label = { Text("Input Value") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF0F172A),
                        focusedBorderColor = Color(0xFF1E40AF),
                        unfocusedBorderColor = Color(0xFF94A3B8)
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text("Select Conversion:", color = Color(0xFF475569), fontSize = 13.sp, fontWeight = FontWeight.Bold)

                val conversions = listOf(
                    "°F to °C" to { v: Double -> (v - 32) * 5 / 9 },
                    "°C to °F" to { v: Double -> v * 9 / 5 + 32 },
                    "Inches to Centimeters" to { v: Double -> v * 2.54 },
                    "Centimeters to Inches" to { v: Double -> v / 2.54 },
                    "Feet to Meters" to { v: Double -> v * 0.3048 },
                    "Meters to Feet" to { v: Double -> v / 0.3048 },
                    "Pounds to Kilograms" to { v: Double -> v * 0.453592 },
                    "Kilograms to Pounds" to { v: Double -> v / 0.453592 }
                )

                conversions.forEach { (label, conversionFn) ->
                    Button(
                        onClick = {
                            keyboardController?.hide()
                            focusManager.clearFocus(force = true)
                            val inVal = inputValue.toDoubleOrNull() ?: 1.0
                            val res = conversionFn(inVal)
                            resultConverted = String.format(Locale.US, "%.5f", res)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(label, color = Color(0xFF1D4ED8), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (resultConverted.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFDBEAFE), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(text = "Result: $resultConverted", color = Color(0xFF1E40AF), fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            if (resultConverted.isNotEmpty()) {
                Button(
                    onClick = { 
                        onSelectConversionResult(resultConverted) 
                        dismissWithKeyboard()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E40AF))
                ) {
                    Text("Insert Result", color = Color.White)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = dismissWithKeyboard) {
                Text("Close", color = Color(0xFF1E40AF))
            }
        },
        containerColor = Color.White
    )
}

@Composable
fun ThemePickerDialog(
    currentTheme: CalcTheme,
    onSelectTheme: (CalcTheme) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Select Theme Palette", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ThemesList.forEach { theme ->
                    val isSelected = theme.id == currentTheme.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectTheme(theme) },
                        shape = RoundedCornerShape(12.dp),
                        border = if (isSelected) BorderStroke(2.dp, theme.operatorColor) else null,
                        colors = CardDefaults.cardColors(containerColor = theme.backgroundColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = theme.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.expressionTextColor
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(modifier = Modifier.size(16.dp).background(theme.operatorColor, CircleShape))
                                Box(modifier = Modifier.size(16.dp).background(theme.actionColor, CircleShape))
                                Box(modifier = Modifier.size(16.dp).background(theme.digitColor, CircleShape))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E40AF))) {
                Text("Done", color = Color.White)
            }
        },
        containerColor = Color.White
    )
}

@Composable
fun FunctionGrapherDialog(
    angleMode: String,
    onDismiss: () -> Unit
) {
    var formula by remember { mutableStateOf("sin(x) * x") }
    var xMinStr by remember { mutableStateOf("-10") }
    var xMaxStr by remember { mutableStateOf("10") }
    var yMinStr by remember { mutableStateOf("-10") }
    var yMaxStr by remember { mutableStateOf("10") }

    val xMin = xMinStr.toDoubleOrNull() ?: -10.0
    val xMax = xMaxStr.toDoubleOrNull() ?: 10.0
    val yMin = yMinStr.toDoubleOrNull() ?: -10.0
    val yMax = yMaxStr.toDoubleOrNull() ?: 10.0

    val presets = listOf("sin(x) * x", "sin(x)", "cos(x)", "abs(x)", "x^2 - 4", "ln(x)", "sqrt(x)")

    // Precompute graph points to avoid heavy parsing of 300 formula expressions inside the Canvas draw pass
    val graphPoints = remember(formula, angleMode, xMin, xMax) {
        val pointsList = mutableListOf<Pair<Double, Double>>()
        if (xMax > xMin && formula.isNotBlank()) {
            val steps = 300
            val stepSize = (xMax - xMin) / steps
            for (j in 0..steps) {
                val xVal = xMin + j * stepSize
                val yVal = try {
                    val parser = CalculatorParser(formula, angleMode, mapOf("x" to xVal))
                    parser.parse().toDouble()
                } catch (e: Throwable) {
                    Double.NaN
                }
                pointsList.add(Pair(xVal, yVal))
            }
        }
        pointsList
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("2D Scientific Function Plotter", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Formula in terms of x:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF475569)
                )

                OutlinedTextField(
                    value = formula,
                    onValueChange = { formula = it },
                    singleLine = true,
                    placeholder = { Text("e.g. sin(x) * x") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF0F172A)
                    )
                )

                // Quick presets
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(presets) { p ->
                        Surface(
                            onClick = { formula = p },
                            color = Color(0xFFEFF6FF),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                        ) {
                            Text(
                                text = p,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E40AF),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Canvas screen plot
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color(0xFF090D16), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val gridColor = Color(0xFF475569)
                        val axisLineColor = Color(0xFF94A3B8)
                        val curveColor = Color(0xFF10B981) // Neon emerald

                        val scaleX = { x: Double -> ((x - xMin) / (xMax - xMin) * size.width).toFloat() }
                        val scaleY = { y: Double -> (size.height - (y - yMin) / (yMax - yMin) * size.height).toFloat() }

                        // Horizontal lines
                        if (yMax > yMin) {
                            val step = max(1.0, (yMax - yMin) / 10.0)
                            var curr = floor(yMin)
                            while (curr <= ceil(yMax)) {
                                if (curr != 0.0) {
                                    val sy = scaleY(curr)
                                    drawLine(
                                        color = gridColor.copy(alpha = 0.25f),
                                        start = Offset(0f, sy),
                                        end = Offset(size.width, sy),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                }
                                curr += step
                            }
                        }

                        // Vertical lines
                        if (xMax > xMin) {
                            val step = max(1.0, (xMax - xMin) / 10.0)
                            var curr = floor(xMin)
                            while (curr <= ceil(xMax)) {
                                if (curr != 0.0) {
                                    val sx = scaleX(curr)
                                    drawLine(
                                        color = gridColor.copy(alpha = 0.25f),
                                        start = Offset(sx, 0f),
                                        end = Offset(sx, size.height),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                }
                                curr += step
                            }
                        }

                        // X axis
                        val axisY = scaleY(0.0)
                        if (axisY in 0f..size.height) {
                            drawLine(
                                color = axisLineColor,
                                start = Offset(0f, axisY),
                                end = Offset(size.width, axisY),
                                strokeWidth = 2.dp.toPx()
                            )
                        }

                        // Y axis
                        val axisX = scaleX(0.0)
                        if (axisX in 0f..size.width) {
                            drawLine(
                                color = axisLineColor,
                                start = Offset(axisX, 0f),
                                end = Offset(axisX, size.height),
                                strokeWidth = 2.dp.toPx()
                            )
                        }

                        // Draw Curve using precomputed points
                        if (graphPoints.isNotEmpty() && xMax > xMin && yMax > yMin) {
                            val curvePath = Path()
                            var isFirstPoint = true
                            
                            for (point in graphPoints) {
                                val xVal = point.first
                                val yVal = point.second
                                
                                if (yVal.isFinite()) {
                                    val sx = scaleX(xVal)
                                    val sy = scaleY(yVal)
                                    
                                    if (sx in 0f..size.width && sy in 0f..size.height) {
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
                                color = curveColor,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Scale controls inputs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = xMinStr,
                        onValueChange = { xMinStr = it },
                        label = { Text("xMin") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color(0xFF0F172A), unfocusedTextColor = Color(0xFF0F172A))
                    )
                    OutlinedTextField(
                        value = xMaxStr,
                        onValueChange = { xMaxStr = it },
                        label = { Text("xMax") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color(0xFF0F172A), unfocusedTextColor = Color(0xFF0F172A))
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = yMinStr,
                        onValueChange = { yMinStr = it },
                        label = { Text("yMin") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color(0xFF0F172A), unfocusedTextColor = Color(0xFF0F172A))
                    )
                    OutlinedTextField(
                        value = yMaxStr,
                        onValueChange = { yMaxStr = it },
                        label = { Text("yMax") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color(0xFF0F172A), unfocusedTextColor = Color(0xFF0F172A))
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E40AF))) {
                Text("Close Plotter", color = Color.White)
            }
        },
        containerColor = Color.White
    )
}
