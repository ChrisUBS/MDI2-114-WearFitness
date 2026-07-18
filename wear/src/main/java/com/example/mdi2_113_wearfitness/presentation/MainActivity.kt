/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package com.example.mdi2_113_wearfitness.presentation


import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.example.mdi2_113_wearfitness.presentation.theme.MDI2113WearFitnessTheme
import androidx.activity.result.contract.ActivityResultContracts
import com.example.mdi2_113_wearfitness.data.FirebaseRepository
import com.google.android.gms.wearable.Wearable
import com.google.firebase.firestore.ListenerRegistration

class MainActivity : ComponentActivity() {
    private var heartRate by mutableIntStateOf(78)
    private var stepsGoal by mutableIntStateOf(10000)
    private lateinit var wearDataListener:  WearDataListener
    private lateinit var heartRateSensorManager: HeartRateSensorManager
    private lateinit var repository: FirebaseRepository
    private var firebaseListener: ListenerRegistration? = null
    private var lastUploadedHeartRate = -1
    private var lastHeartRateUploadTime = 0L
    private val heartRatePermissionLauncher = registerForActivityResult(ActivityResultContracts
        .RequestPermission()) { isGranted ->
        if (isGranted){
            heartRateSensorManager.startListening()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        repository = FirebaseRepository()

        createNotificationChannel(this)
        heartRateSensorManager = HeartRateSensorManager(context = this, onHeartRateChange = {
                newHeartRate ->
            runOnUiThread {
                heartRate = newHeartRate
                uploadHeartRateIfNeeded(newHeartRate)
            }
        })

        if(heartRateSensorManager.hasHeartRateSensor && !heartRateSensorManager.hasPermission()) {
            heartRatePermissionLauncher.launch(
                heartRateSensorManager.requiredPermission
            )
        }

        wearDataListener = WearDataListener(onStepsGoalChange = { newGoal ->
            runOnUiThread { stepsGoal = newGoal }
        })
        setContent {
            MDI2113WearFitnessTheme {
                WearFitnessApp(
                    heartRateSensorValue = heartRate,
                    hasHeartRateSensor = heartRateSensorManager.hasHeartRateSensor,
                    stepsGoalFromPhone = stepsGoal,
                    onStepsChanged = { newSteps -> uploadSteps(newSteps) }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::heartRateSensorManager.isInitialized){
            heartRateSensorManager.startListening()
        }
        if (::wearDataListener.isInitialized){
            Wearable.getDataClient(this).addListener(wearDataListener)
        }
        if (::repository.isInitialized){
            startFirebaseListener()
        }
    }


    override fun onStop() {
        super.onStop()
        if (::heartRateSensorManager.isInitialized){
            heartRateSensorManager.stopListening()
        }
        if (::wearDataListener.isInitialized){
            Wearable.getDataClient(this).removeListener(wearDataListener)
        }
        stopFirebaseListener()
    }

    private fun startFirebaseListener() {
        if (firebaseListener != null) {
            return
        }
        firebaseListener = repository.listenToFitnessData(
            onDataChanged = { fitnessData ->
                runOnUiThread { stepsGoal = fitnessData.dailyGoal.toInt() }
            },
            onError = { exception ->
                Log.e("SharedFirebaseWear", "Error listening to fitness data", exception)
            }
        )
    }

    private fun stopFirebaseListener() {
        firebaseListener?.remove()
        firebaseListener = null
    }

    private fun uploadHeartRateIfNeeded(newHeartRate: Int) {
        if (newHeartRate <= 0) {
            return
        }

        val now = SystemClock.elapsedRealtime()
        val hasChanged = newHeartRate != lastUploadedHeartRate
        val uploadIntervalElapsed = now - lastHeartRateUploadTime >= HEART_RATE_UPLOAD_INTERVAL_MS
        if (!hasChanged && !uploadIntervalElapsed) {
            return
        }

        lastUploadedHeartRate = newHeartRate
        lastHeartRateUploadTime = now
        repository.updateHeartRate(
            heartRate = newHeartRate.toLong(),
            onError = { exception ->
                Log.e("SharedFirebaseWear", "Error uploading heart rate", exception)
            }
        )
    }

    private fun uploadSteps(newSteps: Int) {
        repository.updateSteps(
            steps = newSteps.toLong(),
            onError = { exception ->
                Log.e("SharedFirebaseWear", "Error uploading steps", exception)
            }
        )
    }

    private companion object {
        const val HEART_RATE_UPLOAD_INTERVAL_MS = 5_000L
    }
}
