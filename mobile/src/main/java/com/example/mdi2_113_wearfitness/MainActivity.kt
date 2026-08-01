package com.example.mdi2_113_wearfitness

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.mdi2_113_wearfitness.data.FirebaseRepository

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create the repository once when the activity starts.
        val repository = FirebaseRepository()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    PhoneCompanionApp(
                        repository = repository
                    )
                }
            }
        }
    }
}

/**
 * Main application composable.
 *
 * This composable owns the screen state and business actions.
 * The visual content is separated into GoalControlSection
 * and ActionSection.
 */
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun PhoneCompanionApp(
    repository: FirebaseRepository
) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }

    // Current daily step goal.
    var stepsGoal by remember {
        mutableIntStateOf(10_000)
    }

    // Current heart-rate value received from Firebase.
    var heartRate by remember {
        mutableIntStateOf(72)
    }

    // Current number of steps received from Firebase.
    var steps by remember {
        mutableIntStateOf(0)
    }

    // Message displayed after sending or saving data.
    var sendStatus by remember {
        mutableStateOf("Not sent")
    }

    /*
     * Listen for fitness-data changes in Firebase.
     *
     * DisposableEffect removes the listener when this composable
     * leaves the composition.
     */
    DisposableEffect(repository) {
        val listenerRegistration = repository.listenToFitnessData(
            onDataChanged = { fitnessData ->
                stepsGoal = fitnessData.dailyGoal.toInt()
                heartRate = fitnessData.heartRate.toInt()
                steps = fitnessData.steps.toInt()
                sendStatus = "Data received from Firebase"
            },
            onError = { exception ->
                Log.e(
                    "SharedFirebasePhone",
                    "Error listening to fitness data",
                    exception
                )

                sendStatus =
                    "Firebase listener error: " +
                            (exception.message ?: "Unknown error")
            }
        )

        onDispose {
            listenerRegistration.remove()
        }
    }

    /*
     * Decrease the daily goal by 500 steps.
     *
     * The goal cannot be lower than 500.
     */
    val decreaseGoal: () -> Unit = {
        if (stepsGoal > 500) {
            stepsGoal -= 500
        }
    }

    // Increase the daily goal by 500 steps.
    val increaseGoal: () -> Unit = {
        stepsGoal += 500
    }

    // Send the selected goal to the Wear OS watch.
    val sendToWatch: () -> Unit = {
        sendStatus = "Sending..."

        sendStepsGoalToWatch(
            context = context,
            stepsGoal = stepsGoal,
            onSuccess = {
                sendStatus = "Sent $stepsGoal to the watch"
            },
            onError = { errorMessage ->
                sendStatus = "Watch error: $errorMessage"
            }
        )
    }

    // Save the selected daily goal in Firebase.
    val saveToFirebase: () -> Unit = {
        sendStatus = "Saving to Firebase..."

        repository.updateDailyGoal(
            dailyGoal = stepsGoal.toLong(),
            onSuccess = {
                sendStatus = "Saved $stepsGoal in Firebase"
            },
            onError = { exception ->
                sendStatus =
                    "Firebase error: " +
                            (exception.message ?: "Unknown error")
            }
        )
    }

    /*
     * Request keyboard focus after the UI enters the composition.
     *
     * This allows the root container to receive keyboard shortcuts.
     */
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    /*
     * Root screen container.
     *
     * This is not divided into PhoneLayout and TabletLayout.
     * The same two sections are used on every device.
     *
     * BoxWithConstraints only changes how the sections are arranged:
     * - Narrow screen: sections are displayed vertically.
     * - Wide screen: sections are displayed horizontally.
     */
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { keyEvent ->
                handleKeyboardShortcut(
                    keyEventType = keyEvent.type,
                    key = keyEvent.key,
                    isCtrlPressed = keyEvent.isCtrlPressed,
                    onIncreaseGoal = increaseGoal,
                    onDecreaseGoal = decreaseGoal,
                    onSendToWatch = sendToWatch,
                    onSaveToFirebase = saveToFirebase
                )
            }
    ) {
        val useHorizontalArrangement = maxWidth >= 700.dp

        if (useHorizontalArrangement) {
            HorizontalContent(
                stepsGoal = stepsGoal,
                steps = steps,
                heartRate = heartRate,
                sendStatus = sendStatus,
                onIncreaseGoal = increaseGoal,
                onDecreaseGoal = decreaseGoal,
                onSendToWatch = sendToWatch,
                onSaveToFirebase = saveToFirebase
            )
        } else {
            VerticalContent(
                stepsGoal = stepsGoal,
                steps = steps,
                heartRate = heartRate,
                sendStatus = sendStatus,
                onIncreaseGoal = increaseGoal,
                onDecreaseGoal = decreaseGoal,
                onSendToWatch = sendToWatch,
                onSaveToFirebase = saveToFirebase
            )
        }
    }
}

/**
 * Displays the two application sections horizontally.
 *
 * The same GoalControlSection and ActionSection composables
 * are also used in the vertical arrangement.
 */
@Composable
private fun HorizontalContent(
    stepsGoal: Int,
    steps: Int,
    heartRate: Int,
    sendStatus: String,
    onIncreaseGoal: () -> Unit,
    onDecreaseGoal: () -> Unit,
    onSendToWatch: () -> Unit,
    onSaveToFirebase: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GoalControlSection(
            stepsGoal = stepsGoal,
            onIncrease = onIncreaseGoal,
            onDecrease = onDecreaseGoal,
            modifier = Modifier.weight(1f)
        )

        ActionSection(
            steps = steps,
            heartRate = heartRate,
            sendStatus = sendStatus,
            onSendToWatch = onSendToWatch,
            onSaveToFirebase = onSaveToFirebase,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Displays the two application sections vertically.
 *
 * This arrangement is used when there is not enough horizontal space.
 */
@Composable
private fun VerticalContent(
    stepsGoal: Int,
    steps: Int,
    heartRate: Int,
    sendStatus: String,
    onIncreaseGoal: () -> Unit,
    onDecreaseGoal: () -> Unit,
    onSendToWatch: () -> Unit,
    onSaveToFirebase: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GoalControlSection(
            stepsGoal = stepsGoal,
            onIncrease = onIncreaseGoal,
            onDecrease = onDecreaseGoal,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        HorizontalDivider()

        ActionSection(
            steps = steps,
            heartRate = heartRate,
            sendStatus = sendStatus,
            onSendToWatch = onSendToWatch,
            onSaveToFirebase = onSaveToFirebase,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
    }
}

/**
 * Section responsible for displaying and modifying the daily step goal.
 *
 * This composable does not know anything about Firebase or Wear OS.
 * It only receives the current value and callback functions.
 */
@Composable
fun GoalControlSection(
    stepsGoal: Int,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    modifier: Modifier = Modifier
) {
    val recompositionCount = remember {
//        mutableIntStateOf(0)
        intArrayOf(0)
    }

    // Increment the counter after every successful recomposition.
    SideEffect {
//        recompositionCount.intValue++
        recompositionCount[0]++
    }

    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Wear Fitness",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(
            modifier = Modifier.height(32.dp)
        )

        Text(
            text = "Steps Goal",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        Text(
            text = "Use Ctrl + Minus/Plus to adjust the goal",
            style = MaterialTheme.typography.labelSmall
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = onDecrease,
                enabled = stepsGoal > 500
            ) {
                Text(
                    text = "-"
                )
            }

            Spacer(
                modifier = Modifier.width(24.dp)
            )

            Text(
                text = stepsGoal.toString(),
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(
                modifier = Modifier.width(24.dp)
            )

            Button(
                onClick = onIncrease
            ) {
                Text(
                    text = "+"
                )
            }
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Text(
            text = "GoalControlSection recompositions: " +
//                    recompositionCount.intValue,
                    recompositionCount[0],
            style = MaterialTheme.typography.labelSmall
        )
    }
}

/**
 * Section responsible for displaying fitness information
 * and the available application actions.
 *
 * This composable does not directly access Firebase or Wear OS.
 * It invokes callback functions provided by its parent.
 */
@Composable
fun ActionSection(
    steps: Int,
    heartRate: Int,
    sendStatus: String,
    onSendToWatch: () -> Unit,
    onSaveToFirebase: () -> Unit,
    modifier: Modifier = Modifier
) {
    val recompositionCount = remember {
//        mutableIntStateOf(0)
        intArrayOf(0)
    }

    // Increment the counter after every successful recomposition.
    SideEffect {
//        recompositionCount.intValue++
        recompositionCount[0]++
    }

    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Steps Today",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Text(
            text = steps.toString(),
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        Text(
            text = "Heart Rate",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Text(
            text = "$heartRate BPM",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        Text(
            text = "Use Ctrl + W/F to send or save",
            style = MaterialTheme.typography.labelSmall
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Button(
            onClick = onSendToWatch
        ) {
            Text(
                text = "Send to Watch"
            )
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Button(
            onClick = onSaveToFirebase
        ) {
            Text(
                text = "Save to Firebase"
            )
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Text(
            text = "Status: $sendStatus",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Text(
            text = "ActionSection recompositions: " +
//                    recompositionCount.intValue,
                    recompositionCount[0],
            style = MaterialTheme.typography.labelSmall
        )
    }
}

/**
 * Processes keyboard shortcuts used by the application.
 *
 * Available shortcuts:
 * Ctrl + Plus  -> Increase the step goal.
 * Ctrl + Minus -> Decrease the step goal.
 * Ctrl + W     -> Send the goal to the watch.
 * Ctrl + F     -> Save the goal in Firebase.
 *
 * The function returns true when the event was handled.
 */
private fun handleKeyboardShortcut(
    keyEventType: KeyEventType,
    key: Key,
    isCtrlPressed: Boolean,
    onIncreaseGoal: () -> Unit,
    onDecreaseGoal: () -> Unit,
    onSendToWatch: () -> Unit,
    onSaveToFirebase: () -> Unit
): Boolean {
    if (
        keyEventType != KeyEventType.KeyDown ||
        !isCtrlPressed
    ) {
        return false
    }

    return when (key) {
        Key.Equals,
        Key.Plus -> {
            onIncreaseGoal()
            true
        }

        Key.Minus -> {
            onDecreaseGoal()
            true
        }

        Key.W -> {
            onSendToWatch()
            true
        }

        Key.F -> {
            onSaveToFirebase()
            true
        }

        else -> false
    }
}