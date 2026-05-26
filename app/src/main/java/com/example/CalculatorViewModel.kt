package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.db.HistoryDatabase
import com.example.db.HistoryItem
import com.example.db.HistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

class CalculatorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HistoryRepository

    init {
        val database = HistoryDatabase.getDatabase(application)
        repository = HistoryRepository(database.historyDao())
    }

    // Expose histories reactively
    val historyState: StateFlow<List<HistoryItem>> = repository.allHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _expression = MutableStateFlow("")
    val expression: StateFlow<String> = _expression.asStateFlow()

    private val _previewResult = MutableStateFlow("")
    val previewResult: StateFlow<String> = _previewResult.asStateFlow()

    private val _isScientificExpanded = MutableStateFlow(false)
    val isScientificExpanded: StateFlow<Boolean> = _isScientificExpanded.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Scientific angle and base modes matching TI-36X Pro
    private val _angleMode = MutableStateFlow("DEG")
    val angleMode: StateFlow<String> = _angleMode.asStateFlow()

    private val _baseMode = MutableStateFlow("DEC")
    val baseMode: StateFlow<String> = _baseMode.asStateFlow()

    // 8 Memory Stored Variables: x, y, z, t, a, b, c, d
    private val _variables = MutableStateFlow<Map<String, Double>>(
        mapOf(
            "x" to 0.0, "y" to 0.0, "z" to 0.0, "t" to 0.0,
            "a" to 0.0, "b" to 0.0, "c" to 0.0, "d" to 0.0
        )
    )
    val variables: StateFlow<Map<String, Double>> = _variables.asStateFlow()

    fun toggleScientific() {
        _isScientificExpanded.value = !_isScientificExpanded.value
    }

    fun toggleAngleMode() {
        _angleMode.value = if (_angleMode.value == "DEG") "RAD" else "DEG"
        updatePreview()
    }

    fun setBaseMode(mode: String) {
        _baseMode.value = mode
        // Auto convert expression or clear if base is not DEC
        if (mode != "DEC") {
            _expression.value = ""
            _previewResult.value = ""
        }
        updatePreview()
    }

    fun storeVariable(name: String, valueStr: String) {
        val cleanName = name.lowercase()
        val parsedVal = valueStr.toDoubleOrNull() ?: _previewResult.value.toDoubleOrNull() ?: _expression.value.toDoubleOrNull() ?: 0.0
        val currentVars = _variables.value.toMutableMap()
        currentVars[cleanName] = parsedVal
        _variables.value = currentVars
    }

    fun clearVariables() {
        _variables.value = mapOf(
            "x" to 0.0, "y" to 0.0, "z" to 0.0, "t" to 0.0,
            "a" to 0.0, "b" to 0.0, "c" to 0.0, "d" to 0.0
        )
    }

    fun onDigitClick(digit: String) {
        _errorMessage.value = null
        _expression.value += digit
        updatePreview()
    }

    fun onOperatorClick(operator: String) {
        _errorMessage.value = null
        val current = _expression.value
        if (current.isEmpty() && (operator == "-" || operator == "−")) {
            _expression.value = "-"
            return
        }
        
        if (current.isNotEmpty()) {
            val lastChar = current.last()
            if (isOperatorSymbol(lastChar)) {
                _expression.value = current.dropLast(1) + operator
            } else {
                _expression.value += operator
            }
        }
        updatePreview()
    }

    fun onFunctionClick(funcName: String) {
        _errorMessage.value = null
        _expression.value += funcName
        updatePreview()
    }

    fun onConstantClick(constant: String) {
        _errorMessage.value = null
        _expression.value += constant
        updatePreview()
    }

    fun onParenthesisClick() {
        _errorMessage.value = null
        val expr = _expression.value
        val openCount = expr.count { it == '(' }
        val closeCount = expr.count { it == ')' }
        val lastChar = expr.lastOrNull()
        
        if (openCount > closeCount && (lastChar?.isDigit() == true || lastChar == ')' || lastChar == '%')) {
            _expression.value += ")"
        } else {
            if (lastChar != null && (lastChar.isDigit() || lastChar == ')' || lastChar == '%')) {
                _expression.value += "×("
            } else {
                _expression.value += "("
            }
        }
        updatePreview()
    }

    fun onClearClick() {
        _expression.value = ""
        _previewResult.value = ""
        _errorMessage.value = null
    }

    fun onBackspaceClick() {
        _errorMessage.value = null
        val current = _expression.value
        if (current.isNotEmpty()) {
            // Check list of longer suffixes first
            val suffixes = listOf(
                "asin(", "acos(", "atan(", "sinh(", "cosh(", "tanh(", "asinh(", "acosh(", "atanh(",
                "sqrt(", "cbrt(", "iPart(", "fPart(", "min(", "max(", "mod(", "lcm(", "gcd(", "nPr(", "nCr(",
                "sin(", "cos(", "tan(", "log(", "ln(", "abs(", "int("
            )
            var matched = false
            for (suffix in suffixes) {
                if (current.endsWith(suffix)) {
                    _expression.value = current.dropLast(suffix.length)
                    matched = true
                    break
                }
            }
            if (!matched) {
                if (current.endsWith("nCr") || current.endsWith("nPr")) {
                    _expression.value = current.dropLast(3)
                } else {
                    _expression.value = current.dropLast(1)
                }
            }
        }
        updatePreview()
    }

    fun onNegateClick() {
        _errorMessage.value = null
        val current = _expression.value
        if (current.isEmpty()) {
            _expression.value = "-"
        } else if (current.startsWith("-")) {
            _expression.value = current.substring(1)
        } else {
            _expression.value = "-$current"
        }
        updatePreview()
    }

    fun onEqualClick() {
        val currentExpr = _expression.value
        if (currentExpr.isBlank()) return

        try {
            val compiled = CalculatorParser(currentExpr, _angleMode.value, _variables.value).parse()
            val formatted = formatResult(compiled)
            
            // Save to database
            viewModelScope.launch {
                repository.insert(HistoryItem(expression = currentExpr, result = formatted))
            }
            
            _expression.value = formatted
            _previewResult.value = ""
            _errorMessage.value = null
        } catch (e: Exception) {
            _errorMessage.value = e.message ?: "Format Error"
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun selectHistoryItem(item: HistoryItem) {
        _expression.value = item.expression
        _previewResult.value = item.result
        _errorMessage.value = null
    }

    private fun updatePreview() {
        val currentExpr = _expression.value
        if (currentExpr.isBlank()) {
            _previewResult.value = ""
            return
        }
        try {
            val compiled = CalculatorParser(currentExpr, _angleMode.value, _variables.value).parse()
            _previewResult.value = formatResult(compiled)
        } catch (e: Exception) {
            _previewResult.value = ""
        }
    }

    private fun isOperatorSymbol(c: Char): Boolean {
        return c == '+' || c == '-' || c == '×' || c == '÷' || c == '*' || c == '/' || c == '^' || c == '%'
    }

    private fun formatResult(value: BigVal): String {
        return value.toFormattedString()
    }
}
