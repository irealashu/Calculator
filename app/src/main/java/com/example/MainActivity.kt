package com.example

import android.os.Bundle
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
        setContent {
            MyApplicationTheme {
                CalculatorScreen()
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

@Composable
fun CalculatorScreen(viewModel: CalculatorViewModel = viewModel()) {
    val expr by viewModel.expression.collectAsState()
    val preview by viewModel.previewResult.collectAsState()
    val error by viewModel.errorMessage.collectAsState()
    val isScientificExpanded by viewModel.isScientificExpanded.collectAsState()
    val angleMode by viewModel.angleMode.collectAsState()
    val baseMode by viewModel.baseMode.collectAsState()
    val variables by viewModel.variables.collectAsState()
    val historyList by viewModel.historyState.collectAsState()

    var showHistory by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf("TRIG") }
    
    // Dialog/Form triggers
    var showSolverDialog by remember { mutableStateOf<String?>(null) } // "QUAD", "SYSTEM"
    var showConversionDialog by remember { mutableStateOf(false) }
    var showVariableStoreDialog by remember { mutableStateOf<String?>(null) } // Variable name selected

    val haptic = LocalHapticFeedback.current

    // Premium Prism Ice Blue Light Theme Layout
    val backgroundColor = Color(0xFFEDF2F8)     // High-contrast clean icy background
    val darkSurfaceColor = Color(0xFFD3E0EA)    // Soft blue-grey secondary background/surface
    val operatorColor = Color(0xFF1E40AF)       // Rich Indigo Blue for operators and equal keys
    val actionColor = Color(0xFFE0F2FE)         // Light blue accent container (sky-100)
    val digitColor = Color(0xFFFFFFFF)          // Ultra-crisp paper white background for numbers
    val clearColor = Color(0xFFFCA5A5)          // Soft coral rose for clear action (AC indicator)

    val clearTextColor = Color(0xFF991B1B)      // High contrast deep red for clear text
    val operatorTextColor = Color(0xFFFFFFFF)   // Clean white text on operator keys
    val expressionTextColor = Color(0xFF0F172A) // Deep charcoal / slate-900 for max formula contrast
    val previewResultColor = Color(0xFF475569)  // Deep slate grey for live preview calculation result
    val functionalTextColor = Color(0xFF1D4ED8) // Rich royal cobalt blue for scientific helper texts

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
            // Mode Banner (DEG status, base status, titles)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Clicking Angle Mode toggles DEG/RAD
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.toggleAngleMode()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = actionColor),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(
                        text = angleMode,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = functionalTextColor
                    )
                }

                Text(
                    text = "IRA05-26",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = operatorColor,
                    letterSpacing = 1.sp
                )

                // HIST Button
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showHistory = true
                    },
                    modifier = Modifier
                        .background(actionColor, RoundedCornerShape(12.dp))
                        .size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Show History",
                        tint = functionalTextColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Real-time scientific indicator status bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "VARS: [x=${variables["x"]}, y=${variables["y"]}, z=${variables["z"]}]",
                    fontSize = 11.sp,
                    color = previewResultColor.copy(alpha = 0.8f)
                )
            }

            // High-fidelity seamless display expression panel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.0f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .border(1.5.dp, Color(0xFFBFDBFE), RoundedCornerShape(20.dp))
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
                            color = Color(0xFFB91C1C),
                            textAlign = TextAlign.End
                        )
                    } else if (preview.isNotEmpty()) {
                        Text(
                            text = preview,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Light,
                            color = previewResultColor,
                            textAlign = TextAlign.End,
                            maxLines = 1
                        )
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
                            fontSize = if (expr.length > 8) 40.sp else 54.sp,
                            fontWeight = FontWeight.Light,
                            color = expressionTextColor,
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            letterSpacing = (-0.5).sp
                        )
                    }
                }
            }

            // Scrollable Menu Tabs for math, trig, probability, solvers, memory
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
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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

            // Active Tab helper ribbon buttons
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
                                RibbonButton(label = item.second, onClick = { viewModel.onFunctionClick(item.first) })
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
                                RibbonButton(label = item.second, onClick = { viewModel.onFunctionClick(item.first) })
                            }
                        }
                    }
                    "PRB" -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RibbonButton(label = "nCr", onClick = { viewModel.onOperatorClick("nCr") })
                            RibbonButton(label = "nPr", onClick = { viewModel.onOperatorClick("nPr") })
                            RibbonButton(label = "Factorial (x!)", onClick = { viewModel.onDigitClick("!") })
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
                                RibbonButton(label = "RCL $variable", onClick = { viewModel.onDigitClick(variable) })
                                RibbonButton(label = "STO $variable", onClick = { showVariableStoreDialog = variable })
                            }
                            RibbonButton(label = "Clear Mem", onClick = { viewModel.clearVariables() })
                        }
                    }
                    "SOLVERS" -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RibbonButton(label = "Quadratic Solver (ax²+bx+c=0)", onClick = { showSolverDialog = "QUAD" })
                            RibbonButton(label = "2x2 Linear System", onClick = { showSolverDialog = "SYSTEM" })
                        }
                    }
                    "CONV" -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RibbonButton(label = "Conversion Menu", onClick = { showConversionDialog = true })
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
                                RibbonButton(label = item.second, onClick = { viewModel.onConstantClick(item.first) })
                            }
                        }
                    }
                }
            }

            // Scientific keypad layout - permanently visible
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(3.5f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Unified scientific and standard keys
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
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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

        // Sliding History Drawer
        AnimatedVisibility(
            visible = showHistory,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it },
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
                        .fillMaxWidth()
                        .fillMaxHeight(0.7f)
                        .align(Alignment.BottomCenter)
                        .clickable(enabled = false) {},
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    colors = CardDefaults.cardColors(containerColor = darkSurfaceColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "History Log",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = expressionTextColor
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (historyList.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.clearHistory()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Clear all recordings",
                                            tint = Color(0xFFB91C1C)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        showHistory = false
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close history pane",
                                        tint = expressionTextColor
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = expressionTextColor.copy(alpha = 0.12f), thickness = 1.dp)

                        if (historyList.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = previewResultColor,
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "No history available yet",
                                        color = previewResultColor,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(historyList) { item ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(backgroundColor.copy(alpha = 0.5f))
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                viewModel.selectHistoryItem(item)
                                                showHistory = false
                                            }
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text(
                                            text = item.expression,
                                            fontSize = 16.sp,
                                            color = previewResultColor,
                                            textAlign = TextAlign.End
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "= ${item.result}",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = expressionTextColor,
                                            textAlign = TextAlign.End
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
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
fun RibbonButton(label: String, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Surface(
        modifier = Modifier
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        color = Color(0xFFD3E0EA),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E40AF),
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
