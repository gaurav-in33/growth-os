package com.growthos.personal.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.growthos.personal.R

@Composable
fun ProfileScreen(name: String, onSaveName: (String) -> Unit) {
    val c = Growth.colors
    var section by rememberSaveable { mutableIntStateOf(0) }
    var editingName by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ScreenTitle("More")
            Segmented(listOf("Report", "Money", "Notes", "Settings"), section, { section = it })
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (section) {
                0 -> {
                    ProfileHeader(name = name)
                    ScoreCard()
                    DailyReviewCard()
                    WeeklyReportCard()
                }
                1 -> FinanceCard()
                2 -> NotesCard()
                else -> {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Name",
                                    color = c.textSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (name.isBlank()) "Not set" else name,
                                    color = c.textPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            SoftButton(text = "Edit", onClick = { editingName = true })
                        }
                    }
                    SettingsCard()
                    ReviewReminderCard()
                }
            }
        }
    }

    if (editingName) {
        TextEditDialog(
            heading = "Your name",
            initial = name,
            maxLength = 30,
            onSave = { newName ->
                onSaveName(newName)
                editingName = false
            },
            onDismiss = { editingName = false }
        )
    }
}

@Composable
private fun ProfileHeader(name: String) {
    val c = Growth.colors
    val stats = rememberDayStats()
    GlassCard(modifier = Modifier.fillMaxWidth(), glow = true, padding = 18.dp) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            RingProgress(progress = stats.levelFraction, diameter = 82.dp, stroke = 6.dp) {
                Image(
                    painter = painterResource(id = R.drawable.profile_photo),
                    contentDescription = "Profile photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(62.dp).clip(CircleShape)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (name.isBlank()) "Your profile" else name,
                    color = c.textPrimary,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Level ${stats.level}",
                    color = c.accent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${stats.xpToNext} XP to next level",
                    color = c.textSecondary,
                    fontSize = 12.5.sp
                )
            }
        }
    }
}
