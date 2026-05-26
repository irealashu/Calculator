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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.db.HistoryItem

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

// Key structure for beautiful keypad rendering
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
    val historyList by viewModel.historyState.collectAsState()

    var showHistory by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    // Professional Polish color scheme matching Material Design 3 Dark spec
    val backgroundColor = Color(0xFF1C1B1F)     // Main theme background
    val darkSurfaceColor = Color(0xFF211F26)    // Surface obsidian / key background
    val operatorColor = Color(0xFFD0BCFF)       // M3 Primary lilac accent
    val actionColor = Color(0xFF4A4458)         // M3 Secondary / container
    val digitColor = Color(0xFF211F26)          // Surface digits / values
    val clearColor = Color(0xFFEFB8C8)          // M3 Tertiary pinkish AC accent

    val clearTextColor = Color(0xFF492532)      // Dark slate pink text
    val operatorTextColor = Color(0xFF381E72)   // Dark purple text
    val expressionTextColor = Color(0xFFE6E1E5) // Pure light off-white expression
    val previewResultColor = Color(0xFFCAC4D0)  // Muted grey-lilac preview
    val functionalTextColor = Color(0xFFD0BCFF) // Lilac scientific text accent

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
            // Header: title, with SCI and HIST triggers perfectly integrated
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // SCI toggle button
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.toggleScientific()
                    },
                    modifier = Modifier
                        .background(
                            if (isScientificExpanded) operatorColor else actionColor,
                            RoundedCornerShape(14.dp)
                        )
                        .size(48.dp)
                ) {
                    Text(
                        text = "SCI",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isScientificExpanded) operatorTextColor else functionalTextColor
                    )
                }

                Text(
                    text = "Calculator",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = expressionTextColor,
                    textAlign = TextAlign.Center
                )

                // HIST trigger button
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showHistory = true
                    },
                    modifier = Modifier
                        .background(actionColor, RoundedCornerShape(14.dp))
                        .size(48.dp)
                ) {
                    Text(
                        text = "HIST",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = functionalTextColor
                    )
                }
            }

            // High-fidelity seamless display expression panel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.3f)
                    .background(backgroundColor)
                    .padding(horizontal = 8.dp, vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.End
                ) {
                    // Previous/Live preview on top (M3 secondary style)
                    if (error != null) {
                        Text(
                            text = error ?: "Format Error",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFFF2B8B5),
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

                    Spacer(modifier = Modifier.height(8.dp))

                    // Scrolling main input expression matching visual tracking values
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
                            fontSize = if (expr.length > 8) 48.sp else 64.sp,
                            fontWeight = FontWeight.Normal,
                            color = expressionTextColor,
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            letterSpacing = (-1).sp
                        )
                    }
                }
            }

            // Clean decorative action bar containing constants, slider visual line, mode indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Constants quick insert
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(actionColor)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.onConstantClick("π")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "π",
                            color = Color(0xFFE8DEF8),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(actionColor)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.onConstantClick("e")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "e",
                            color = Color(0xFFE8DEF8),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Decorative horizontal divider line (with opacity match)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 24.dp)
                        .height(2.dp)
                        .background(actionColor.copy(alpha = 0.3f))
                )

                // Current standard/science operation label
                Text(
                    text = if (isScientificExpanded) "SCIENTIFIC" else "STANDARD",
                    color = functionalTextColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.5.sp
                )
            }

            // Responsive high-tactile Keypad Sector
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(2.7f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Expanding scientific layouts
                AnimatedVisibility(
                    visible = isScientificExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        val row1 = listOf(
                            CalcKey.Scientific("sin(", "sin"),
                            CalcKey.Scientific("cos(", "cos"),
                            CalcKey.Scientific("tan(", "tan"),
                            CalcKey.Scientific("sqrt(", "√")
                        )
                        val row2 = listOf(
                            CalcKey.Scientific("log(", "log"),
                            CalcKey.Scientific("ln(", "ln"),
                            CalcKey.Scientific("^", "^"),
                            CalcKey.Scientific("π", "π"),
                            CalcKey.Scientific("e", "e")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            row1.forEach { key ->
                                KeypadButton(
                                    key = key,
                                    bgColor = darkSurfaceColor,
                                    textColor = functionalTextColor,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        when (key) {
                                            is CalcKey.Scientific -> viewModel.onFunctionClick(key.value)
                                            else -> {}
                                        }
                                    }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            row2.forEach { key ->
                                KeypadButton(
                                    key = key,
                                    bgColor = darkSurfaceColor,
                                    textColor = functionalTextColor,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        when (key) {
                                            is CalcKey.Scientific -> {
                                                if (key.value == "π" || key.value == "e") {
                                                    viewModel.onConstantClick(key.value)
                                                } else if (key.value == "^") {
                                                    viewModel.onOperatorClick(key.value)
                                                } else {
                                                    viewModel.onFunctionClick(key.value)
                                                }
                                            }
                                            else -> {}
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Standard keypad definitions based on Professional Polish layout
                val standardGrid = listOf(
                    listOf(CalcKey.Clear, CalcKey.Parenthesis("( )"), CalcKey.Percent, CalcKey.Operator("÷")),
                    listOf(CalcKey.Digit("7"), CalcKey.Digit("8"), CalcKey.Digit("9"), CalcKey.Operator("×")),
                    listOf(CalcKey.Digit("4"), CalcKey.Digit("5"), CalcKey.Digit("6"), CalcKey.Operator("-")),
                    listOf(CalcKey.Digit("1"), CalcKey.Digit("2"), CalcKey.Digit("3"), CalcKey.Operator("+")),
                    listOf(CalcKey.Digit("0"), CalcKey.Digit("."), CalcKey.Backspace, CalcKey.Equal)
                )

                // Render standard keyboard
                standardGrid.forEach { rowKeys ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowKeys.forEach { key ->
                            val currentBgColor = when (key) {
                                is CalcKey.Clear -> clearColor
                                is CalcKey.Parenthesis, is CalcKey.Percent -> actionColor
                                is CalcKey.Operator, is CalcKey.Equal -> operatorColor
                                else -> digitColor
                            }

                            val currentTextColor = when (key) {
                                is CalcKey.Clear -> clearTextColor
                                is CalcKey.Parenthesis, is CalcKey.Percent -> functionalTextColor
                                is CalcKey.Operator, is CalcKey.Equal -> operatorTextColor
                                is CalcKey.Backspace -> functionalTextColor
                                else -> expressionTextColor
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
                                        is CalcKey.Parenthesis -> viewModel.onParenthesisClick()
                                        else -> {}
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Sliding History Drawer (Material 3 Surface styled precisely)
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
                        // History Header controls
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
                                            tint = Color(0xFFF2B8B5)
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
            .clip(RoundedCornerShape(28.dp))
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
            is CalcKey.Scientific -> 16.sp
            is CalcKey.Clear, is CalcKey.Backspace, is CalcKey.Negate -> 20.sp
            else -> 24.sp
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
