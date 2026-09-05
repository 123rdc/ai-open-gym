package com.example.gymformcoach.core.designsystem.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.example.gymformcoach.core.designsystem.Primary
import com.example.gymformcoach.core.navigation.Screen

@Composable
fun GymBottomNavigation(
    currentRoute: String?,
    onTabSelected: (String) -> Unit
) {
    val items = listOf(
        Triple(Screen.Home.route, "Home", Icons.Default.Home),
        Triple(Screen.WeeklyPlan.route, "Plan", Icons.Default.CalendarMonth),
        Triple(Screen.Progress.route, "Stats", Icons.Default.TrendingUp),
        Triple(Screen.ExerciseLibrary.route, "Exercises", Icons.Default.FitnessCenter),
        Triple(Screen.Profile.route, "Profile", Icons.Default.Person)
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = Primary
    ) {
        items.forEach { (route, label, icon) ->
            NavigationBarItem(
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) },
                selected = currentRoute == route,
                onClick = { onTabSelected(route) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Primary,
                    selectedTextColor = Primary,
                    indicatorColor = Primary.copy(alpha = 0.1f)
                )
            )
        }
    }
}
