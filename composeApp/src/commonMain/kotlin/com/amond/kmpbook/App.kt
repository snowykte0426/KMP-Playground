package com.amond.kmpbook

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun App() {
    val viewModel: MealViewModel = viewModel { MealViewModel() }
    var showSchoolSetup by remember { mutableStateOf(!viewModel.prefs.isConfigured) }
    var isChangingSchool by remember { mutableStateOf(false) }

    if (showSchoolSetup) {
        SchoolSetupScreen(
            viewModel = viewModel,
            isChanging = isChangingSchool,
            onDone = {
                if (viewModel.prefs.isConfigured) {
                    showSchoolSetup = false
                    isChangingSchool = false
                }
            }
        )
    } else {
        MealScreen(
            viewModel = viewModel,
            onChangeSchool = {
                isChangingSchool = true
                showSchoolSetup = true
            }
        )
    }
}
