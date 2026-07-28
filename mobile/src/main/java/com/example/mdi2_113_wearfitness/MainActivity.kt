package com.example.mdi2_113_wearfitness

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.mdi2_113_wearfitness.data.FirebaseRepository
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = FirebaseRepository()

        setContent {
            MaterialTheme {
                PhoneCompanionApp(repository = repository)
            }
        }
    }
}

/*
 * The different sections that can be selected from the list pane.
 */
enum class GoalSection(
    val title: String,
    val icon: ImageVector
) {
    Overview(
        title = "Overview",
        icon = Icons.Default.Home
    ),
    History(
        title = "History",
        icon = Icons.Default.Star
    ),
    Tips(
        title = "Tips",
        icon = Icons.Default.Info
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun PhoneCompanionApp(repository: FirebaseRepository) {

    val context = LocalContext.current

    var stepsGoal by remember {
        mutableIntStateOf(10000)
    }

    var heartRate by remember {
        mutableIntStateOf(72)
    }

    var steps by remember {
        mutableIntStateOf(0)
    }

    var sendStatus by remember {
        mutableStateOf("Not sent")
    }

    /*
     * Listen for changes from Firebase.
     */
    DisposableEffect(repository) {

        val listenerRegistration =
            repository.listenToFitnessData(
                onDataChanged = { fitnessData ->

                    stepsGoal =
                        fitnessData.dailyGoal.toInt()

                    heartRate =
                        fitnessData.heartRate.toInt()

                    steps =
                        fitnessData.steps.toInt()

                    sendStatus =
                        "Data received from Firebase"
                },
                onError = { exception ->

                    Log.e(
                        "SharedFirebasePhone",
                        "Error listening to fitness data",
                        exception
                    )
                }
            )

        onDispose {
            listenerRegistration.remove()
        }
    }

    /*
     * Navigator used by ListDetailPaneScaffold.
     *
     * On a phone, navigating to Detail displays the detail
     * as a separate screen.
     *
     * On a tablet, the list and detail panes can be displayed
     * next to each other.
     */
    val navigator =
        rememberListDetailPaneScaffoldNavigator<GoalSection>()

    val scope = rememberCoroutineScope()
    /*
     * Handle the Android back button.
     *
     * If the user is looking at a detail screen on a phone,
     * pressing Back returns to the list.
     */
    BackHandler(
        enabled = navigator.canNavigateBack()
    ) {
        scope.launch {
            navigator.navigateBack()
        }
    }

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,

        /*
         * LEFT PANE / LIST SCREEN
         */
        listPane = {

            AnimatedPane {

                GoalSectionsList(
                    selectedSection =
                        navigator.currentDestination?.contentKey,

                    onSectionSelected = { section ->

                        scope.launch {
                            navigator.navigateTo(
                                ListDetailPaneScaffoldRole.Detail,
                                section
                            )
                        }
                    }
                )
            }
        },

        /*
         * RIGHT PANE / DETAIL SCREEN
         */
        detailPane = {

            AnimatedPane {

                val selectedSection =
                    navigator.currentDestination?.contentKey

                when (selectedSection) {

                    GoalSection.Overview -> {

                        OverviewPane(
                            stepsGoal = stepsGoal,
                            steps = steps,
                            heartRate = heartRate,
                            sendStatus = sendStatus,

                            onDecreaseGoal = {
                                if (stepsGoal > 500) {
                                    stepsGoal -= 500
                                }
                            },

                            onIncreaseGoal = {
                                stepsGoal += 500
                            },

                            onSendToWatch = {

                                sendStatus = "Sending..."

                                sendStepsGoalToWatch(
                                    context = context,
                                    stepsGoal = stepsGoal,

                                    onSuccess = {
                                        sendStatus =
                                            "Sent to watch!"
                                    },

                                    onError = { error ->
                                        sendStatus =
                                            "Error: $error"
                                    }
                                )
                            },

                            onSaveFirebase = {

                                sendStatus =
                                    "Saving to Firebase..."

                                repository.updateDailyGoal(
                                    dailyGoal =
                                        stepsGoal.toLong(),

                                    onSuccess = {
                                        sendStatus =
                                            "Saved $stepsGoal in Firebase"
                                    },

                                    onError = { exception ->
                                        sendStatus =
                                            "Firebase Error: " +
                                                    (
                                                            exception.message
                                                                ?: "Unknown error"
                                                            )
                                    }
                                )
                            }
                        )
                    }

                    GoalSection.History -> {

                        HistoryPane(
                            stepsGoal = stepsGoal,
                            steps = steps,
                            heartRate = heartRate
                        )
                    }

                    GoalSection.Tips -> {

                        TipsPane(
                            stepsGoal = stepsGoal,
                            steps = steps
                        )
                    }

                    null -> {

                        EmptyDetailPane()
                    }
                }
            }
        }
    )
}

/*
 * =========================================================
 * LIST PANE
 * =========================================================
 */

@Composable
fun GoalSectionsList(
    selectedSection: GoalSection?,
    onSectionSelected: (GoalSection) -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {

        Text(
            text = "Fitness Goal",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "Explore your daily steps goal",
            style = MaterialTheme.typography.bodyMedium,
            color =
                MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        LazyColumn(
            verticalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            items(GoalSection.entries) { section ->

                Card(
                    modifier =
                        Modifier.fillMaxWidth(),

                    onClick = {
                        onSectionSelected(section)
                    }
                ) {

                    ListItem(
                        headlineContent = {
                            Text(section.title)
                        },

                        supportingContent = {

                            Text(
                                when (section) {

                                    GoalSection.Overview ->
                                        "Current goal and fitness data"

                                    GoalSection.History ->
                                        "View your progress"

                                    GoalSection.Tips ->
                                        "Tips to reach your goal"
                                }
                            )
                        },

                        leadingContent = {

                            Icon(
                                imageVector = section.icon,
                                contentDescription =
                                    section.title
                            )
                        },

                        trailingContent = {

                            if (selectedSection == section) {

                                Text(
                                    text = "Selected",
                                    style =
                                        MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

/*
 * =========================================================
 * OVERVIEW DETAIL
 * =========================================================
 */

@Composable
fun OverviewPane(
    stepsGoal: Int,
    steps: Int,
    heartRate: Int,
    sendStatus: String,
    onDecreaseGoal: () -> Unit,
    onIncreaseGoal: () -> Unit,
    onSendToWatch: () -> Unit,
    onSaveFirebase: () -> Unit
) {

    val progress =
        if (stepsGoal > 0) {
            (steps.toFloat() / stepsGoal.toFloat())
                .coerceIn(0f, 1f)
        } else {
            0f
        }

    val percentage =
        (progress * 100).toInt()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),

        verticalArrangement =
            Arrangement.spacedBy(20.dp)
    ) {

        item {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Face,

                    contentDescription =
                        "Walking",

                    tint =
                        MaterialTheme.colorScheme.primary
                )

                Spacer(
                    modifier = Modifier.width(12.dp)
                )

                Column {

                    Text(
                        text = "Daily Steps Goal",
                        style =
                            MaterialTheme.typography.headlineMedium
                    )

                    Text(
                        text =
                            "Track your progress for today",

                        style =
                            MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        /*
         * Goal card
         */
        item {

            Card(
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier =
                        Modifier.padding(24.dp),

                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "Goal",
                        style =
                            MaterialTheme.typography.titleMedium
                    )

                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically,

                        horizontalArrangement =
                            Arrangement.Center
                    ) {

                        Button(
                            onClick =
                                onDecreaseGoal
                        ) {
                            Text("-")
                        }

                        Spacer(
                            modifier =
                                Modifier.width(20.dp)
                        )

                        Text(
                            text =
                                "$stepsGoal",

                            style =
                                MaterialTheme.typography
                                    .headlineLarge
                        )

                        Spacer(
                            modifier =
                                Modifier.width(20.dp)
                        )

                        Button(
                            onClick =
                                onIncreaseGoal
                        ) {
                            Text("+")
                        }
                    }

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    Text(
                        text = "steps per day",
                        style =
                            MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        /*
         * Today's progress.
         */
        item {

            Card(
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier =
                        Modifier.padding(24.dp)
                ) {

                    Text(
                        text = "Today's Progress",
                        style =
                            MaterialTheme.typography.titleLarge
                    )

                    Spacer(
                        modifier =
                            Modifier.height(16.dp)
                    )

                    Text(
                        text = "$steps / $stepsGoal steps",
                        style =
                            MaterialTheme.typography.headlineSmall
                    )

                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )

                    LinearProgressIndicator(
                        progress = {
                            progress
                        },

                        modifier =
                            Modifier.fillMaxWidth()
                    )

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    Text(
                        text =
                            "$percentage% of your daily goal"
                    )
                }
            }
        }

        /*
         * Heart rate information.
         */
        item {

            Card(
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier =
                        Modifier.padding(24.dp)
                ) {

                    Text(
                        text = "Heart Rate",
                        style =
                            MaterialTheme.typography.titleMedium
                    )

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    Text(
                        text = "$heartRate BPM",
                        style =
                            MaterialTheme.typography.headlineMedium
                    )

                    Text(
                        text =
                            "Latest reading from your fitness data",

                        style =
                            MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        /*
         * Actions
         */
        item {

            Card(
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier =
                        Modifier.padding(24.dp)
                ) {

                    Text(
                        text = "Actions",
                        style =
                            MaterialTheme.typography.titleLarge
                    )

                    Spacer(
                        modifier =
                            Modifier.height(16.dp)
                    )

                    Button(
                        onClick =
                            onSendToWatch,

                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Text(
                            "Send Goal to Watch"
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )

                    Button(
                        onClick =
                            onSaveFirebase,

                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Text(
                            "Save Goal to Firebase"
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.height(16.dp)
                    )

                    Text(
                        text =
                            "Status: $sendStatus",

                        style =
                            MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

/*
 * =========================================================
 * HISTORY DETAIL
 * =========================================================
 */

@Composable
fun HistoryPane(
    stepsGoal: Int,
    steps: Int,
    heartRate: Int
) {

    val percentage =
        if (stepsGoal > 0) {
            (
                    steps.toFloat() /
                            stepsGoal.toFloat() *
                            100
                    )
                .toInt()
                .coerceAtMost(100)
        } else {
            0
        }

    val remaining =
        (stepsGoal - steps)
            .coerceAtLeast(0)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),

        verticalArrangement =
            Arrangement.spacedBy(16.dp)
    ) {

        item {

            Text(
                text = "Progress History",
                style =
                    MaterialTheme.typography.headlineMedium
            )

            Text(
                text =
                    "A summary of your current fitness progress.",

                style =
                    MaterialTheme.typography.bodyMedium
            )
        }

        item {

            HistoryCard(
                title = "Steps Today",
                value = "$steps steps",
                description =
                    "Your latest step count from Firebase."
            )
        }

        item {

            HistoryCard(
                title = "Daily Goal",
                value = "$stepsGoal steps",
                description =
                    "Your current daily steps target."
            )
        }

        item {

            HistoryCard(
                title = "Goal Progress",
                value = "$percentage%",
                description =
                    if (remaining > 0) {
                        "$remaining more steps needed to reach today's goal."
                    } else {
                        "You have reached today's goal!"
                    }
            )
        }

        item {

            HistoryCard(
                title = "Latest Heart Rate",
                value = "$heartRate BPM",
                description =
                    "Most recent heart rate reading."
            )
        }

        item {

            Card(
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier =
                        Modifier.padding(20.dp)
                ) {

                    Text(
                        text = "History Note",
                        style =
                            MaterialTheme.typography.titleMedium
                    )

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    Text(
                        text =
                            "This section uses the latest data available " +
                                    "from your repository. Historical daily " +
                                    "records can also be displayed here if " +
                                    "they are stored in Firebase."
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryCard(
    title: String,
    value: String,
    description: String
) {

    Card(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        Column(
            modifier =
                Modifier.padding(20.dp)
        ) {

            Text(
                text = title,
                style =
                    MaterialTheme.typography.titleMedium
            )

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            Text(
                text = value,
                style =
                    MaterialTheme.typography.headlineSmall
            )

            Spacer(
                modifier =
                    Modifier.height(4.dp)
            )

            Text(
                text = description,
                style =
                    MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/*
 * =========================================================
 * TIPS DETAIL
 * =========================================================
 */

@Composable
fun TipsPane(
    stepsGoal: Int,
    steps: Int
) {

    val remaining =
        (stepsGoal - steps)
            .coerceAtLeast(0)

    val progress =
        if (stepsGoal > 0) {
            steps.toFloat() /
                    stepsGoal.toFloat()
        } else {
            0f
        }

    val motivationalMessage =
        when {

            progress >= 1f ->
                "Great job! You reached your daily steps goal."

            progress >= 0.75f ->
                "You're almost there! A short walk could help you finish your goal."

            progress >= 0.5f ->
                "You're more than halfway there. Keep moving!"

            progress >= 0.25f ->
                "Good start. Try adding a short walk to your day."

            else ->
                "Start with a short walk and build your progress throughout the day."
        }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),

        verticalArrangement =
            Arrangement.spacedBy(16.dp)
    ) {

        item {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Info,

                    contentDescription =
                        "Tips",

                    tint =
                        MaterialTheme.colorScheme.primary
                )

                Spacer(
                    modifier =
                        Modifier.width(12.dp)
                )

                Text(
                    text = "Tips",
                    style =
                        MaterialTheme.typography.headlineMedium
                )
            }
        }

        item {

            Card(
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier =
                        Modifier.padding(20.dp)
                ) {

                    Text(
                        text = "Today's Tip",
                        style =
                            MaterialTheme.typography.titleLarge
                    )

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    Text(
                        text =
                            motivationalMessage
                    )

                    if (remaining > 0) {

                        Spacer(
                            modifier =
                                Modifier.height(12.dp)
                        )

                        Text(
                            text =
                                "You have $remaining steps remaining today."
                        )
                    }
                }
            }
        }

        item {

            TipCard(
                title = "Take short walks",
                description =
                    "A few short walks during the day can make your daily goal easier to reach."
            )
        }

        item {

            TipCard(
                title = "Use the stairs",
                description =
                    "Taking the stairs instead of the elevator is an easy way to add more steps."
            )
        }

        item {

            TipCard(
                title = "Walk after meals",
                description =
                    "Consider taking a short walk after breakfast, lunch, or dinner."
            )
        }

        item {

            TipCard(
                title = "Check your progress",
                description =
                    "Use your watch and this companion app to keep track of your progress throughout the day."
            )
        }
    }
}

@Composable
fun TipCard(
    title: String,
    description: String
) {

    Card(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        Row(
            modifier =
                Modifier.padding(20.dp),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Icon(
                imageVector =
                    Icons.Default.Face,

                contentDescription =
                    null
            )

            Spacer(
                modifier =
                    Modifier.width(16.dp)
            )

            Column {

                Text(
                    text = title,
                    style =
                        MaterialTheme.typography.titleMedium
                )

                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )

                Text(
                    text = description,
                    style =
                        MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/*
 * =========================================================
 * EMPTY DETAIL
 * =========================================================
 */

@Composable
fun EmptyDetailPane() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),

        verticalArrangement =
            Arrangement.Center,

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Icon(
            imageVector =
                Icons.Default.Face,

            contentDescription =
                null,

            tint =
                MaterialTheme.colorScheme.primary
        )

        Spacer(
            modifier =
                Modifier.height(16.dp)
        )

        Text(
            text = "Fitness Goal Details",
            style =
                MaterialTheme.typography.headlineMedium
        )

        Spacer(
            modifier =
                Modifier.height(8.dp)
        )

        Text(
            text =
                "Select Overview, History, or Tips to view more information.",

            style =
                MaterialTheme.typography.bodyMedium
        )
    }
}