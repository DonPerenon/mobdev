package io.github.mobdev

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

private val Orange = Color(0xFFFF9500)
private val DarkGray = Color(0xFF333333)
private val LightGray = Color(0xFFA5A5A5)

private enum class BtnType { NUMBER, FUNCTION, OPERATOR }
private data class CalcBtn(val label: String, val type: BtnType)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CalculatorScreen()
        }
    }
}

@Composable
fun CalculatorScreen(viewModel: CalculatorViewModel = viewModel()) {
    val buttons = listOf(
        listOf(
            CalcBtn("⌫", BtnType.FUNCTION),
            CalcBtn("AC", BtnType.FUNCTION),
            CalcBtn("%", BtnType.FUNCTION),
            CalcBtn("÷", BtnType.OPERATOR)
        ),
        listOf(
            CalcBtn("7", BtnType.NUMBER),
            CalcBtn("8", BtnType.NUMBER),
            CalcBtn("9", BtnType.NUMBER),
            CalcBtn("×", BtnType.OPERATOR)
        ),
        listOf(
            CalcBtn("4", BtnType.NUMBER),
            CalcBtn("5", BtnType.NUMBER),
            CalcBtn("6", BtnType.NUMBER),
            CalcBtn("−", BtnType.OPERATOR)
        ),
        listOf(
            CalcBtn("1", BtnType.NUMBER),
            CalcBtn("2", BtnType.NUMBER),
            CalcBtn("3", BtnType.NUMBER),
            CalcBtn("+", BtnType.OPERATOR)
        ),
        listOf(
            CalcBtn("±", BtnType.NUMBER),
            CalcBtn("0", BtnType.NUMBER),
            CalcBtn(",", BtnType.NUMBER),
            CalcBtn("=", BtnType.OPERATOR)
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(end = 8.dp, bottom = 12.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Text(
                text = viewModel.displayResult,
                color = Color.White,
                fontSize = 72.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            for (row in buttons) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for (btn in row) {
                        val bgColor = when (btn.type) {
                            BtnType.FUNCTION -> LightGray
                            BtnType.NUMBER -> DarkGray
                            BtnType.OPERATOR -> Orange
                        }
                        val textColor = when (btn.type) {
                            BtnType.FUNCTION -> Color.Black
                            else -> Color.White
                        }

                        Button(
                            onClick = { viewModel.onButton(btn.label) },
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = bgColor),
                            contentPadding = PaddingValues(0.dp),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 0.dp,
                                pressedElevation = 0.dp
                            )
                        ) {
                            Text(
                                text = btn.label,
                                fontSize = 28.sp,
                                color = textColor
                            )
                        }
                    }
                }
            }
        }
    }
}
