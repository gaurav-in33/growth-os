package com.growthos.personal.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.growthos.personal.R

@Composable
fun ProfileScreen(name: String, onSaveName: (String) -> Unit) {
    val c = Growth.colors
    var draft by remember(name) { mutableStateOf(name) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var savedNote by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "More", color = c.textPrimary, fontSize = 26.sp)

        ScoreCard()

        DailyReviewCard()

        WeeklyReportCard()

        Image(
            painter = painterResource(id = R.drawable.profile_cover),
            contentDescription = "Profile cover photo",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .clip(RoundedCornerShape(24.dp))
        )

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.profile_photo),
                        contentDescription = "Profile photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                    )
                    Text(
                        text = if (name.isBlank()) "Your profile" else name,
                        color = c.textPrimary,
                        fontSize = 20.sp
                    )
                }
                OutlinedTextField(
                    value = draft,
                    onValueChange = {
                        draft = it
                        errorText = null
                        savedNote = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Name") },
                    isError = errorText != null,
                    supportingText = {
                        val message = errorText
                        if (message != null) Text(message)
                        else if (savedNote) Text("Name saved")
                    },
                    colors = glassFieldColors()
                )
                Button(
                    onClick = {
                        val trimmed = draft.trim()
                        if (trimmed.length > 30) {
                            errorText = "Keep it under 30 characters"
                        } else {
                            onSaveName(trimmed)
                            savedNote = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = c.accent,
                        contentColor = Color.White
                    )
                ) {
                    Text("Save name")
                }
            }
        }

        FinanceCard()

        NotesCard()

        SettingsCard()
    }
}
