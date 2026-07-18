package com.example.mdi2_113_wearfitness.presentation

import com.example.mdi2_113_wearfitness.model.FitnessData

data class FitnessUIState (
    val fitnessData: FitnessData = FitnessData(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)