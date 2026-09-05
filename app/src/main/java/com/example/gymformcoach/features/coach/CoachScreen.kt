package com.example.gymformcoach.features.coach

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymformcoach.core.coach.PlanDiffItem
import com.example.gymformcoach.core.coach.PlanValidator
import com.example.gymformcoach.core.designsystem.Error
import com.example.gymformcoach.core.designsystem.Primary
import com.example.gymformcoach.core.designsystem.Surface
import com.example.gymformcoach.core.designsystem.TextSecondary
import com.example.gymformcoach.core.utils.PreferenceManager

/**
 * §17D. Every output here is a proposal the user reviews and explicitly
 * accepts or rejects - nothing on this screen writes to a routine without an
 * explicit tap, and the screen states plainly that its output is generated
 * and unreviewed (§17D.4).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoachScreen(
    onBack: () -> Unit,
    viewModel: CoachViewModel = viewModel()
) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager(context) }
    var coachEnabled by remember { mutableStateOf(prefs.aiCoachEnabled) }
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Coach", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Enable AI Coach", fontWeight = FontWeight.Bold)
                    Text(
                        "Off by default. Proposes plans and revisions - never applies anything without your confirmation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Switch(
                    checked = coachEnabled,
                    onCheckedChange = { coachEnabled = it; prefs.aiCoachEnabled = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = Primary)
                )
            }

            if (!coachEnabled) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    "With the Coach off, the app behaves exactly as it does everywhere else - manual routine building, deterministic progression.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                return@Column
            }

            Spacer(modifier = Modifier.height(24.dp))

            when (val current = state) {
                is CoachUiState.Idle -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { viewModel.generatePlan() }, colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                            Text("Generate a plan", color = androidx.compose.ui.graphics.Color.Black, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(onClick = { viewModel.revisePlan() }, colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary)) {
                            Text("Suggest revisions")
                        }
                    }
                }
                is CoachUiState.Loading -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Primary, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Thinking...", color = TextSecondary)
                    }
                }
                is CoachUiState.Unavailable -> {
                    // §17D.5: identical posture to §9.5 - a designed unavailable state, app continues fully functional.
                    Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Coach unavailable", fontWeight = FontWeight.Bold, color = Error)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(current.reason, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = { viewModel.dismiss() }) { Text("Dismiss", color = Primary) }
                }
                is CoachUiState.Applied -> {
                    Text(current.message, color = Primary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = { viewModel.dismiss() }) { Text("Done", color = Primary) }
                }
                is CoachUiState.ProposalReady -> ProposalReview(
                    plan = current.plan,
                    onAccept = { viewModel.applyPlan(current.plan) },
                    onReject = { viewModel.dismiss() }
                )
                is CoachUiState.RevisionReady -> RevisionReview(
                    revision = current.revision,
                    onApply = { items -> viewModel.applyRevisionItems(items) },
                    onDismiss = { viewModel.dismiss() }
                )
            }
        }
    }
}

@Composable
private fun ProposalReview(
    plan: PlanValidator.ValidatedPlan,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Column {
        // §17D.4: states plainly that this is generated and unreviewed.
        Text(
            "This plan is generated and unreviewed. Progression targets remain deterministic regardless of what's proposed here.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(plan.proposal.planName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(plan.proposal.rationale, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)

        if (plan.droppedExerciseCount > 0 || plan.droppedRoutineNames.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "${plan.droppedExerciseCount} suggestion(s) were skipped" +
                    if (plan.droppedRoutineNames.isNotEmpty()) " (dropped: ${plan.droppedRoutineNames.joinToString(", ")})" else "",
                style = MaterialTheme.typography.labelSmall,
                color = Error
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
            items(plan.proposal.routines) { routine ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(routine.name, fontWeight = FontWeight.Bold)
                        Text(routine.progressionRule, style = MaterialTheme.typography.labelSmall, color = Primary)
                        routine.exercises.forEach { exercise ->
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "${exercise.exerciseId} - ${exercise.sets}x${exercise.repRangeMin}-${exercise.repRangeMax}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (exercise.why.isNotBlank()) {
                                Text(exercise.why, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onAccept, colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                Text("Accept plan", color = androidx.compose.ui.graphics.Color.Black, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(onClick = onReject) { Text("Reject") }
        }
    }
}

@Composable
private fun RevisionReview(
    revision: com.example.gymformcoach.core.coach.PlanRevision,
    onApply: (List<PlanDiffItem>) -> Unit,
    onDismiss: () -> Unit
) {
    val accepted = remember { mutableStateMapOf<Int, Boolean>() }

    Column {
        Text(
            "Suggested changes are unreviewed. Each is independently acceptable - nothing applies until you confirm.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(revision.summary, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
            items(revision.items.size) { index ->
                val item = revision.items[index]
                val isChecked = accepted[index] ?: false
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { accepted[index] = it },
                        colors = CheckboxDefaults.colors(checkedColor = Primary)
                    )
                    Column {
                        Text(describeDiffItem(item), style = MaterialTheme.typography.bodyMedium)
                        Text(item.why, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    val selected = revision.items.filterIndexed { i, _ -> accepted[i] == true }
                    onApply(selected)
                },
                enabled = accepted.values.any { it },
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("Apply selected", color = androidx.compose.ui.graphics.Color.Black, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(onClick = onDismiss) { Text("Dismiss") }
        }
    }
}

private fun describeDiffItem(item: PlanDiffItem): String = when (item) {
    is PlanDiffItem.SwapExercise -> "${item.routineName}: swap ${item.oldExerciseId} -> ${item.newExerciseId}"
    is PlanDiffItem.AdjustVolume -> "${item.routineName}: ${item.exerciseId} -> ${item.newSets} sets"
    is PlanDiffItem.AddExercise -> "${item.routineName}: add ${item.exerciseId} (${item.sets}x${item.repRangeMin}-${item.repRangeMax})"
}
