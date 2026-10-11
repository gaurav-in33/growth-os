package com.growthos.personal.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.growthos.personal.R

@Composable
fun OnboardingScreen(onDone: (String) -> Unit) {
    val c = Growth.colors
    var name by rememberSaveable { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.app_logo),
            contentDescription = "Growth OS logo",
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(28.dp))
        )
        Text(
            text = "Growth OS",
            color = c.textPrimary,
            fontSize = 32.sp,
            modifier = Modifier.padding(top = 20.dp)
        )
        Text(
            text = "Your personal operating system for self-improvement.",
            color = c.textSecondary,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 28.dp)
        )

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "What should I call you?", color = c.textPrimary, fontSize = 16.sp)
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorText = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Your name (optional)") },
                    isError = errorText != null,
                    supportingText = {
                        val message = errorText
                        if (message != null) Text(message)
                    },
                    colors = glassFieldColors()
                )
                Button(
                    onClick = {
                        val trimmed = name.trim()
                        if (trimmed.length > 30) {
                            errorText = "Keep it under 30 characters"
                        } else {
                            onDone(trimmed)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = c.accent,
                        contentColor = Color.White
                    )
                ) {
                    Text("Get started")
                }
                TextButton(
                    onClick = { onDone("") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Skip for now", color = c.textSecondary)
                }
            }
        }
    }
}
