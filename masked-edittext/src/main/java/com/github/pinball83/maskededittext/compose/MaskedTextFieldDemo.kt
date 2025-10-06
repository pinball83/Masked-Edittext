package com.github.pinball83.maskededittext.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Demo screen showcasing various MaskedTextField implementations
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaskedTextFieldDemo() {
    var phoneNumber by remember { mutableStateOf("") }
    var creditCard by remember { mutableStateOf("") }
    var socialSecurity by remember { mutableStateOf("") }
    var customFormat by remember { mutableStateOf("") }

    // State holders for advanced examples
    val phoneState = rememberMaskedTextFieldState(
        maskedOptions = MaskedOptions.phone { oldState, newState, event ->
            println("Phone state changed: $oldState -> $newState (event: $event)")
        }
    )

    val cardState = rememberMaskedTextFieldState(
        maskedOptions = MaskedOptions.creditCard()
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Masked TextField Demo",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        HorizontalDivider()

        // Phone Number Example
        Text(
            text = "Phone Number",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )

        MaskedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            maskedOptions = MaskedOptions.phone(),
            label = { Text("Phone Number") },
            trailingIcon = {
                IconButton(onClick = { phoneNumber = "" }) {
                    Text("X")
                }
            },
            inputOptions = MaskedInputOptions(
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone
                )
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Unmasked: $phoneNumber",
            style = MaterialTheme.typography.bodySmall
        )

        HorizontalDivider()

        // Credit Card Example
        Text(
            text = "Credit Card",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )

        MaskedTextField(
            value = creditCard,
            onValueChange = { creditCard = it },
            maskedOptions = MaskedOptions.creditCard(),
            label = { Text("Credit Card Number") },
            inputOptions = MaskedInputOptions(
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                )
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Unmasked: $creditCard",
            style = MaterialTheme.typography.bodySmall
        )

        HorizontalDivider()

        // Social Security Number Example
        Text(
            text = "Social Security Number",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )

        MaskedTextField(
            value = socialSecurity,
            onValueChange = { socialSecurity = it },
            maskedOptions = MaskedOptions.socialSecurity(),
            label = { Text("SSN") },
            inputOptions = MaskedInputOptions(
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                )
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Unmasked: $socialSecurity",
            style = MaterialTheme.typography.bodySmall
        )

        HorizontalDivider()

        // Custom Format Example
        Text(
            text = "Custom Format (with reordering)",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )

        MaskedTextField(
            value = customFormat,
            onValueChange = { customFormat = it },
            maskedOptions = MaskedOptions.custom(
                mask = "8 (***) *** **-**",
                format = "[1][2][3] [4][5][6]-[7][8]-[10][9]" // Swap last two digits
            ),
            label = { Text("Phone with Custom Format") },
            inputOptions = MaskedInputOptions(
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone
                )
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Unmasked: $customFormat",
            style = MaterialTheme.typography.bodySmall
        )

        HorizontalDivider()

        // Advanced State Management Example
        Text(
            text = "Advanced State Management",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )

        OutlinedTextField(
            value = phoneState.textFieldValue,
            onValueChange = { phoneState.updateValue(it) },
            label = { Text("Phone with State Machine") },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { phoneState.onFocusChanged(it.isFocused) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone
            )
        )

        // State information
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "State Information",
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Current State: ${phoneState.currentState}")
                Text("Is Complete: ${phoneState.isComplete}")
                Text("Is Valid: ${phoneState.isValid}")
                Text("Is Empty: ${phoneState.isEmpty}")
                Text("Unmasked Value: ${phoneState.unmaskedValue}")
                Text("Formatted Value: ${phoneState.getFormattedValue()}")

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { phoneState.validate() }
                    ) {
                        Text("Validate")
                    }

                    Button(
                        onClick = { phoneState.clear() }
                    ) {
                        Text("Clear")
                    }

                    Button(
                        onClick = { phoneState.updateValue("1234567890") }
                    ) {
                        Text("Set Sample")
                    }
                }
            }
        }

        HorizontalDivider()

        // Different Input Types
        Text(
            text = "Different Input Types",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )

        val alphaOptions = remember {
            MaskedOptions.custom(
                mask = "Q***************",
                notMaskedSymbol = '*'
            )
        }
        var alphaNumeric by remember { mutableStateOf("") }
        MaskedTextField(
            value = alphaNumeric,
            onValueChange = { alphaNumeric = it },
            maskedOptions = alphaOptions,
            label = { Text("Alphanumeric (Q + 15 chars)") },
            modifier = Modifier.fillMaxWidth()
        )

        val dateOptions = remember {
            MaskedOptions.custom(mask = "**/**/**", notMaskedSymbol = '*')
        }
        var dateInput by remember { mutableStateOf("") }
        MaskedTextField(
            value = dateInput,
            onValueChange = { dateInput = it },
            maskedOptions = dateOptions,
            label = { Text("Date (MM/DD/YY)") },
            inputOptions = MaskedInputOptions(
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Number
                )
            ),
            modifier = Modifier.fillMaxWidth()
        )

        val timeOptions = remember {
            MaskedOptions.custom(mask = "**:**", notMaskedSymbol = '*')
        }
        var timeInput by remember { mutableStateOf("") }
        MaskedTextField(
            value = timeInput,
            onValueChange = { timeInput = it },
            maskedOptions = timeOptions,
            label = { Text("Time (HH:MM)") },
            inputOptions = MaskedInputOptions(
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Number
                )
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MaskedTextFieldDemoPreview() {
    MaterialTheme {
        MaskedTextFieldDemo()
    }
}
