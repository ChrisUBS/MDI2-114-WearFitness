package com.example.mdi2_113_wearfitness.presentation

import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem

class WearDataListener(
    private val onStepsGoalChange: (Int) -> Unit
): DataClient.OnDataChangedListener {
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED &&
                event.dataItem.uri.path == FITNESS_GOALS_PATH) {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val stepsGoal = dataMap.getInt(STEPS_GOAL_KEY, 10000)
                onStepsGoalChange(stepsGoal)
            }
        }
    }
}