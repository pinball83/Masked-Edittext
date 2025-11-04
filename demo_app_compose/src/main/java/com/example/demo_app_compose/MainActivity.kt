package com.example.demo_app_compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.demo_app_compose.ui.theme.MaskedEdittextTheme
import com.github.pinball83.maskededittext.InputState
import com.github.pinball83.maskededittext.compose.MaskedOptions
import com.github.pinball83.maskededittext.compose.MaskedTextField
import com.github.pinball83.maskededittext.compose.MaskedVisualOptions
import com.github.pinball83.maskededittext.compose.MaskedInputOptions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaskedEdittextTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    DemoScreen()
                }
            }
        }
    }
}

@Composable
private fun DemoScreen() {
    var phoneValue by remember { mutableStateOf("9261234567") }
    var phoneState by remember { mutableStateOf(InputState.EMPTY) }

    var cardValue by remember { mutableStateOf("4111111111111111") }
    var cardState by remember { mutableStateOf(InputState.EMPTY) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Compose MaskedTextField",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )

        Text(text = "Phone number")
        MaskedTextField(
            value = phoneValue,
            onValueChange = { phoneValue = it.filter { ch -> ch.isDigit() } },
            maskedOptions = MaskedOptions.phone { _, newState, _ -> phoneState = newState },
            modifier = Modifier.fillMaxWidth(),
            inputOptions = MaskedInputOptions(
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        )
        Text(text = "State: ${phoneState.name.lowercase()}")

        Text(text = "Card number")
        MaskedTextField(
            value = cardValue,
            onValueChange = { cardValue = it.filter { ch -> ch.isDigit() } },
            maskedOptions = MaskedOptions.creditCard { _, newState, _ -> cardState = newState },
            modifier = Modifier.fillMaxWidth(),
            visualOptions = MaskedVisualOptions(singleLine = true),
            inputOptions = MaskedInputOptions(
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        )
        Text(text = "Formatted: ${cardValue.chunked(4).joinToString("-")}")
        Text(text = "State: ${cardState.name.lowercase()}")
    }
}
