package com.example.gymformcoach.features.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.gymformcoach.core.data.AppDatabase
import com.example.gymformcoach.core.data.ExerciseSessionRepository
import com.example.gymformcoach.core.designsystem.Background
import com.example.gymformcoach.core.designsystem.Primary
import com.example.gymformcoach.core.designsystem.TextSecondary
import com.example.gymformcoach.core.utils.PreferenceManager
import com.example.gymformcoach.core.utils.PrEstimator
import com.example.gymformcoach.core.utils.WeightUnit

import com.example.gymformcoach.core.designsystem.components.GymBottomNavigation
import com.example.gymformcoach.core.navigation.Screen

@Composable
fun ProfileScreen(
    onNavigateToTab: (String) -> Unit,
    onOpenSettings: () -> Unit = {},
    onOpenBodyWeight: () -> Unit = {},
    onOpenWeeklyPlan: () -> Unit = {},
    onOpenDataPortability: () -> Unit = {},
    onOpenCoach: () -> Unit = {}
) {
    val context = LocalContext.current
    val unit = remember { PreferenceManager(context).weightUnit }
    val sessions by remember { ExerciseSessionRepository(AppDatabase.getInstance(context)).getAllSessions() }
        .collectAsState(initial = emptyList())
    val personalRecords = remember(sessions) {
        sessions.groupBy { it.exerciseId }
            .mapNotNull { (exerciseId, records) ->
                records.maxByOrNull { PrEstimator.estimatedOneRepMax(it.weightKg, it.reps) }
                    ?.let { exerciseId to it }
            }
            .sortedBy { it.first }
    }

    Scaffold(
        bottomBar = {
            GymBottomNavigation(
                currentRoute = Screen.Profile.route,
                onTabSelected = onNavigateToTab
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Background)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Text(
                text = "Profile",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

        Spacer(modifier = Modifier.height(32.dp))

        // Profile Info
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
            ) {
                AsyncImage(
                    model = "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?q=80&w=2070&auto=format&fit=crop",
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text(text = "Sarah Jenkins", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(text = "sarah.j@example.com", color = Color.White.copy(alpha = 0.6f))
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        if (personalRecords.isNotEmpty()) {
            Text(
                text = "Personal Records",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                personalRecords.forEach { (exerciseId, best) ->
                    PersonalRecordRow(
                        exerciseName = exerciseId,
                        weightLabel = "${WeightUnit.formatKg(best.weightKg, unit)}$unit × ${best.reps}",
                        estimatedOneRepMax = PrEstimator.estimatedOneRepMax(best.weightKg, best.reps),
                        unit = unit
                    )
                }
            }
            Spacer(modifier = Modifier.height(48.dp))
        }

        // Settings Items
        ProfileItem(label = "Edit Profile")
        ProfileItem(label = "Body Weight", onClick = onOpenBodyWeight)
        ProfileItem(label = "Weekly Plan", onClick = onOpenWeeklyPlan)
        ProfileItem(label = "Data & Backup", onClick = onOpenDataPortability)
        ProfileItem(label = "AI Coach", onClick = onOpenCoach)
        ProfileItem(label = "Privacy Policy")
        ProfileItem(label = "Settings", onClick = onOpenSettings)
        ProfileItem(label = "Help Center")
        ProfileItem(label = "Logout", color = Color.Red)
        }
    }
}

@Composable
fun PersonalRecordRow(
    exerciseName: String,
    weightLabel: String,
    estimatedOneRepMax: Float,
    unit: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = exerciseName, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = weightLabel, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Text(
                text = "e1RM ${WeightUnit.formatKg(estimatedOneRepMax, unit)}$unit",
                color = Primary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun ProfileItem(label: String, color: Color = Color.White, onClick: () -> Unit = {}) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        color = Color.Transparent,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, color = color, fontWeight = FontWeight.Medium)
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
        }
    }
}
