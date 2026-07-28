package com.example.mdi2_113_wearfitness

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass
import com.example.mdi2_113_wearfitness.data.FirebaseRepository


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = FirebaseRepository()

        setContent {
            MaterialTheme {
                PhoneCompanionApp(
                    repository = repository
                )
            }
        }
    }
}


@Composable
fun PhoneCompanionApp(
    repository: FirebaseRepository
) {
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


    // =====================================================
    // WINDOW SIZE CLASS
    // =====================================================

    val windowSizeClass =
        currentWindowAdaptiveInfo().windowSizeClass

    val isTablet =
        windowSizeClass.windowWidthSizeClass !=
                WindowWidthSizeClass.COMPACT


    // =====================================================
    // FIREBASE LISTENER
    // =====================================================

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


    // =====================================================
    // FUNCIONES COMPARTIDAS
    // =====================================================

    val decreaseGoal: () -> Unit = {
        if (stepsGoal > 500) {
            stepsGoal -= 500
        }
    }

    val increaseGoal: () -> Unit = {
        stepsGoal += 500
    }


    val sendToWatch: () -> Unit = {

        sendStatus = "Sending..."

        sendStepsGoalToWatch(
            context = context,
            stepsGoal = stepsGoal,

            onSuccess = {
                sendStatus = "Sent to watch!"
            },

            onError = { error ->
                sendStatus = "Error: $error"
            }
        )
    }


    val saveToFirebase: () -> Unit = {

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
                            (exception.message
                                ?: "Unknown error")
            }
        )
    }


    // =====================================================
    // SELECCIONAR LAYOUT
    // =====================================================

    if (isTablet) {

        TabletLayout(
            stepsGoal = stepsGoal,
            steps = steps,
            heartRate = heartRate,
            sendStatus = sendStatus,

            onDecreaseGoal = decreaseGoal,
            onIncreaseGoal = increaseGoal,

            onSendToWatch = sendToWatch,
            onSaveFirebase = saveToFirebase
        )

    } else {

        PhoneLayout(
            stepsGoal = stepsGoal,
            steps = steps,
            heartRate = heartRate,
            sendStatus = sendStatus,

            onDecreaseGoal = decreaseGoal,
            onIncreaseGoal = increaseGoal,

            onSendToWatch = sendToWatch,
            onSaveFirebase = saveToFirebase
        )
    }
}


// =========================================================
// PHONE LAYOUT
// =========================================================

@Composable
fun PhoneLayout(
    stepsGoal: Int,
    steps: Int,
    heartRate: Int,
    sendStatus: String,
    onDecreaseGoal: () -> Unit,
    onIncreaseGoal: () -> Unit,
    onSendToWatch: () -> Unit,
    onSaveFirebase: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),

        verticalArrangement =
            Arrangement.Center,

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Text(
            text = "Wear Fitness",
            style =
                MaterialTheme.typography.headlineMedium
        )


        Spacer(
            modifier = Modifier.height(24.dp)
        )


        // -----------------------------
        // STEPS GOAL
        // -----------------------------

        Text(
            text = "Steps Goal",
            style =
                MaterialTheme.typography.titleMedium
        )


        Spacer(
            modifier = Modifier.height(12.dp)
        )


        Row(
            verticalAlignment =
                Alignment.CenterVertically,

            horizontalArrangement =
                Arrangement.Center
        ) {

            Button(
                onClick = onDecreaseGoal
            ) {
                Text("-")
            }


            Spacer(
                modifier = Modifier.width(16.dp)
            )


            Text(
                text = stepsGoal.toString(),
                style =
                    MaterialTheme.typography.headlineSmall
            )


            Spacer(
                modifier = Modifier.width(16.dp)
            )


            Button(
                onClick = onIncreaseGoal
            ) {
                Text("+")
            }
        }


        Spacer(
            modifier = Modifier.height(24.dp)
        )


        // -----------------------------
        // STEPS TODAY
        // -----------------------------

        Text(
            text = "Steps Today",
            style =
                MaterialTheme.typography.titleMedium
        )


        Spacer(
            modifier = Modifier.height(12.dp)
        )


        Text(
            text = steps.toString(),
            style =
                MaterialTheme.typography.headlineSmall
        )


        Spacer(
            modifier = Modifier.height(24.dp)
        )


        // -----------------------------
        // HEART RATE
        // -----------------------------

        Text(
            text = "Heart Rate",
            style =
                MaterialTheme.typography.titleMedium
        )


        Spacer(
            modifier = Modifier.height(12.dp)
        )


        Text(
            text = "$heartRate BPM",
            style =
                MaterialTheme.typography.headlineSmall
        )


        Spacer(
            modifier = Modifier.height(24.dp)
        )


        // -----------------------------
        // SEND TO WATCH
        // -----------------------------

        Button(
            onClick = onSendToWatch
        ) {
            Text("Send to Watch")
        }


        Spacer(
            modifier = Modifier.height(12.dp)
        )


        // -----------------------------
        // FIREBASE
        // -----------------------------

        Button(
            onClick = onSaveFirebase
        ) {
            Text("Save to Firebase")
        }


        Spacer(
            modifier = Modifier.height(16.dp)
        )


        Text(
            text = "Status: $sendStatus"
        )
    }
}


// =========================================================
// TABLET LAYOUT
// =========================================================

@Composable
fun TabletLayout(
    stepsGoal: Int,
    steps: Int,
    heartRate: Int,
    sendStatus: String,
    onDecreaseGoal: () -> Unit,
    onIncreaseGoal: () -> Unit,
    onSendToWatch: () -> Unit,
    onSaveFirebase: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),

        verticalAlignment =
            Alignment.CenterVertically
    ) {


        // =================================================
        // MITAD IZQUIERDA
        // =================================================

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(24.dp),

            horizontalAlignment =
                Alignment.CenterHorizontally,

            verticalArrangement =
                Arrangement.Center
        ) {

            Text(
                text = "Wear Fitness",
                style =
                    MaterialTheme.typography.headlineMedium
            )


            Spacer(
                modifier = Modifier.height(32.dp)
            )


            Text(
                text = "Steps Goal",
                style =
                    MaterialTheme.typography.titleMedium
            )


            Spacer(
                modifier = Modifier.height(16.dp)
            )


            Row(
                verticalAlignment =
                    Alignment.CenterVertically,

                horizontalArrangement =
                    Arrangement.Center
            ) {

                Button(
                    onClick = onDecreaseGoal
                ) {
                    Text("-")
                }


                Spacer(
                    modifier = Modifier.width(24.dp)
                )


                Text(
                    text = stepsGoal.toString(),
                    style =
                        MaterialTheme.typography.headlineSmall
                )


                Spacer(
                    modifier = Modifier.width(24.dp)
                )


                Button(
                    onClick = onIncreaseGoal
                ) {
                    Text("+")
                }
            }
        }


        // =================================================
        // MITAD DERECHA
        // =================================================

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(24.dp),

            horizontalAlignment =
                Alignment.CenterHorizontally,

            verticalArrangement =
                Arrangement.Center
        ) {


            // -----------------------------
            // STEPS TODAY
            // -----------------------------

            Text(
                text = "Steps Today",
                style =
                    MaterialTheme.typography.titleMedium
            )


            Spacer(
                modifier = Modifier.height(12.dp)
            )


            Text(
                text = steps.toString(),
                style =
                    MaterialTheme.typography.headlineSmall
            )


            Spacer(
                modifier = Modifier.height(32.dp)
            )


            // -----------------------------
            // HEART RATE
            // -----------------------------

            Text(
                text = "Heart Rate",
                style =
                    MaterialTheme.typography.titleMedium
            )


            Spacer(
                modifier = Modifier.height(12.dp)
            )


            Text(
                text = "$heartRate BPM",
                style =
                    MaterialTheme.typography.headlineSmall
            )


            Spacer(
                modifier = Modifier.height(32.dp)
            )


            // -----------------------------
            // SEND TO WATCH
            // -----------------------------

            Button(
                onClick = onSendToWatch
            ) {
                Text("Send to Watch")
            }


            Spacer(
                modifier = Modifier.height(12.dp)
            )


            // -----------------------------
            // FIREBASE
            // -----------------------------

            Button(
                onClick = onSaveFirebase
            ) {
                Text("Save to Firebase")
            }


            Spacer(
                modifier = Modifier.height(16.dp)
            )


            Text(
                text = "Status: $sendStatus"
            )
        }
    }
}