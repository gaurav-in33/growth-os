package com.growthos.personal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.growthos.personal.data.AiClient
import com.growthos.personal.data.AiStore
import com.growthos.personal.data.ProfileStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AiCard() {
    val c = Growth.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { AiStore(context) }
    var enabled by remember { mutableStateOf(store.enabled()) }
    var share by remember { mutableStateOf(store.shareData()) }
    var hasKey by remember { mutableStateOf(store.key().isNotEmpty()) }
    var keyText by remember { mutableStateOf("") }
    var modelText by remember { mutableStateOf(store.model()) }
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<String?>(null) }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "AI assistant", color = c.textPrimary, fontSize = 18.sp)
                    Text(text = if (enabled) "On" else "Off", color = c.textSecondary, fontSize = 13.sp)
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = {
                        enabled = it
                        store.setEnabled(it)
                    },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = c.accent,
                        checkedThumbColor = Color.White
                    )
                )
            }
            if (enabled) {
                Text(
                    text = "Uses your own free Gemini key. Money and notes are never sent. " +
                        "Free-tier Google may use the data to improve its products.",
                    color = c.textSecondary,
                    fontSize = 12.sp
                )
                if (!hasKey) {
                    OutlinedTextField(
                        value = keyText,
                        onValueChange = { keyText = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Paste Gemini key here") },
                        visualTransformation = PasswordVisualTransformation(),
                        colors = glassFieldColors()
                    )
                    SoftButton(
                        text = "Save key",
                        onClick = {
                            if (keyText.trim().length >= 20) {
                                store.setKey(keyText)
                                keyText = ""
                                hasKey = true
                                note = null
                            } else {
                                note = "That key looks too short."
                            }
                        }
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Key saved on this phone",
                            color = c.textPrimary,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        SoftButton(
                            text = "Remove key",
                            onClick = {
                                store.setKey("")
                                hasKey = false
                                answer = null
                            }
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Share goals, habits and tasks",
                            color = c.textPrimary,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = share,
                            onCheckedChange = {
                                share = it
                                store.setShareData(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = c.accent,
                                checkedThumbColor = Color.White
                            )
                        )
                    }
                    OutlinedTextField(
                        value = modelText,
                        onValueChange = {
                            modelText = it
                            store.setModel(it)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Model name") },
                        colors = glassFieldColors()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MiniChip(label = "Plan my day", selected = false, onClick = { question = "Plan my day from my open tasks." })
                        MiniChip(label = "What to do now?", selected = false, onClick = { question = "What should I do right now?" })
                    }
                    OutlinedTextField(
                        value = question,
                        onValueChange = { question = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Ask anything") },
                        colors = glassFieldColors()
                    )
                    GlowButton(
                        text = if (busy) "Thinking..." else "Ask",
                        enabled = !busy && question.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val q = question.trim()
                            busy = true
                            answer = null
                            scope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    val ctx = if (store.shareData()) {
                                        try {
                                            AiClient.buildContext(context, ProfileStore(context).name())
                                        } catch (e: Exception) {
                                            ""
                                        }
                                    } else {
                                        ""
                                    }
                                    AiClient.ask(store, q, ctx)
                                }
                                answer = result
                                busy = false
                            }
                        }
                    )
                }
                val msg = note
                if (msg != null) Text(text = msg, color = c.textSecondary, fontSize = 13.sp)
                val reply = answer
                if (reply != null) Text(text = reply, color = c.textPrimary, fontSize = 15.sp)
            }
        }
    }
}
