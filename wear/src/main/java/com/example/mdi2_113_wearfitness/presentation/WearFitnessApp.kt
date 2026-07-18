package com.example.mdi2_113_wearfitness.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text

@Composable
fun WearFitnessApp(
    heartRateSensorValue: Int,
    hasHeartRateSensor: Boolean,
    stepsGoalFromPhone: Int,
    onStepsChanged: (Int) -> Unit
){
    val navController = rememberNavController()
    val context = LocalContext.current

    var heartRateNotificationSent by remember {
        mutableStateOf(false)
    }
    var stepsNotificationSent by remember {
        mutableStateOf(false)
    }

    var notificationPermissionGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                    || ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        notificationPermissionGranted = isGranted
    }
    LaunchedEffect(Unit) {
        if(
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && !notificationPermissionGranted
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    var steps by remember { mutableIntStateOf(30) }
    var displayedStepsGoal by remember { mutableIntStateOf(stepsGoalFromPhone) }
    var calories by remember { mutableIntStateOf(25) }
    var caloriesGoal by remember { mutableIntStateOf(800) }
    var manualHeartRate by remember { mutableIntStateOf(78) }

    val displayedHeartRate =
        if (hasHeartRateSensor) {
            heartRateSensorValue
        } else {
            manualHeartRate
        }

    LaunchedEffect(stepsGoalFromPhone) {
        displayedStepsGoal = stepsGoalFromPhone
    }

    LaunchedEffect(
        displayedHeartRate,
        notificationPermissionGranted
    ) {
        if (
            displayedHeartRate >= 100 &&
            !heartRateNotificationSent &&
            notificationPermissionGranted
        ){
            showNotification(
                context=context,
                notificationId = HEART_RATE_NOTIFICATION_ID,
                title="High Heart Rate Detect",
                message = "Your heart rate reached $displayedHeartRate BPM"
            )
            heartRateNotificationSent = true
        }

        if (displayedHeartRate < 100){
            heartRateNotificationSent = false
        }
    }

    LaunchedEffect(
        steps,
        notificationPermissionGranted
    ) {
        if (
            steps >= 1000 &&
            !stepsNotificationSent &&
            notificationPermissionGranted
        ){
            showNotification(
                context=context,
                notificationId = STEPS_NOTIFICATION_ID,
                title="Step Count Achieved!",
                message = "You’ve walked $steps steps today!, keep going!!"
            )
            stepsNotificationSent = true
        }

        if (steps < 1000){
            stepsNotificationSent = false
        }
    }

    SwipeNavigationContainer(
        navController = navController
    ) {
        NavHost(
            navController = navController,
            startDestination = "progress"
        ) {
            composable("progress") {
                DailyProgressScreen(
                    steps = steps,
                    calories = calories,
                    stepsGoal = displayedStepsGoal,
                    caloriesGoal = caloriesGoal,
                    onAddStep = {
                        steps+=100
                        calories+=100
                        onStepsChanged(steps)
                    }
                )
            }
            composable("heart") {
                HeartRateScreen(
                    heartBeat = displayedHeartRate,
                    hasHeartRateSensor = hasHeartRateSensor,
                    onAddHeartBeat = { manualHeartRate++ },
                    onSubtractHeartBeat = { manualHeartRate-- }
                )
            }
            composable("modify") {
                ModifyGoalScreen(
                    stepsGoal = displayedStepsGoal,
                    caloriesGoal = caloriesGoal,
                    onAddStepsGoal = { displayedStepsGoal += 50 },
                    onSubtractStepsGoal = { displayedStepsGoal -= 50 },
                    onAddCaloriesGoal = { caloriesGoal += 10 },
                    onSubtractCaloriesGoal = { caloriesGoal -= 10 }
                )
            }
        }
    }
}

@Composable
fun SwipeNavigationContainer(
    navController: NavHostController,
    content: @Composable () -> Unit
){
    val routes = listOf(
        "progress",
        "heart",
        "modify"
    )
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: "progress"
    val currentIndex = routes.indexOf(currentRoute)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(currentRoute){
                var totalDrag = 0f
                detectHorizontalDragGestures(
                    onDragStart = {
                        totalDrag = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        totalDrag += dragAmount
                    },
                    onDragEnd = {
                        // Move forward
                        if (
                            totalDrag < -60 &&
                            currentIndex < routes.lastIndex
                        ) {
                            navController.navigate(
                                routes[currentIndex + 1]
                            ){
                                launchSingleTop = true
                            }
                        }

                        // Move backward
                        if(
                            totalDrag > 60 &&
                            currentIndex > 0
                        ) {
                            navController.navigate(
                                routes[currentIndex - 1]
                            ){
                                launchSingleTop = true
                            }
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ){
        content()
    }
}

@Composable
fun DailyProgressScreen(
    steps:Int,
    calories: Int,
    stepsGoal: Int,
    caloriesGoal: Int,
    onAddStep: () -> Unit
){
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Daily Progress",
            color = Color.Cyan,
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Steps",
            color = Color.Green,
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "$steps / $stepsGoal",
            color = Color.White,
            style = MaterialTheme.typography.labelMedium
        )
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Calories",
            color = Color.Yellow,
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "$calories / $caloriesGoal",
            color = Color.White,
            style = MaterialTheme.typography.labelMedium

        )
        Spacer(modifier = Modifier.height(10.dp))
        Button( onClick =  onAddStep ){
            Text("Add Step")
        }
    }
}

@Composable
fun HeartRateScreen(
    heartBeat: Int,
    hasHeartRateSensor: Boolean,
    onAddHeartBeat: () -> Unit,
    onSubtractHeartBeat: () -> Unit
){
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Heart Rate",
            style = MaterialTheme.typography.titleLarge,
            color = Color(0XFFEF2D56)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!hasHeartRateSensor){
                Button(
                    onClick = onSubtractHeartBeat,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.Red
                    )
                ) {
                    Text(
                        text = "-",
                        style = MaterialTheme.typography.displayMedium
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            Text(
                text = "$heartBeat BPM ♥️",
                style = MaterialTheme.typography.labelMedium
            )

            if (!hasHeartRateSensor) {
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = onAddHeartBeat,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.Green
                    )
                ) {
                    Text(
                        text = "+",
                        style = MaterialTheme.typography.displayMedium
                    )
                }
            }
        }
    }
}

@Composable
fun ModifyGoalScreen(
    stepsGoal: Int,
    caloriesGoal: Int,
    onAddStepsGoal: () -> Unit,
    onAddCaloriesGoal: () -> Unit,
    onSubtractStepsGoal: () -> Unit,
    onSubtractCaloriesGoal: () -> Unit
){
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Modify Goal",
            color = Color.Cyan,
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text= "Steps",
            style = MaterialTheme.typography.titleLarge,
            color = Color.Green
        )
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onSubtractStepsGoal,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.Red
                )
            ) {
                Text(
                    text = "-",
                    style = MaterialTheme.typography.displayMedium
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "$stepsGoal",
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = onAddStepsGoal,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.Green
                )
            ) {
                Text(
                    text = "+",
                    style = MaterialTheme.typography.displayMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Calories",
            color = Color.Yellow,
            style = MaterialTheme.typography.titleLarge
        )

        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = onSubtractCaloriesGoal,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.Red
                )
            ) {
                Text(
                    text = "-",
                    style = MaterialTheme.typography.displayMedium
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "$caloriesGoal",
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = onAddCaloriesGoal,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.Green
                )
            ) {
                Text(
                    text = "+",
                    style = MaterialTheme.typography.displayMedium
                )
            }
        }
    }
}
