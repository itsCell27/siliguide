package com.example.siliguide.presentation.features.analysis

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.siliguide.core.ml.LabuyoLeafChecker
import com.example.siliguide.core.ml.LeafClassifier
import com.example.siliguide.core.ml.Prediction
import com.example.siliguide.domain.engine.RuleResult
import com.example.siliguide.domain.model.SensorResponse
import com.example.siliguide.domain.usecase.AnalyzePlantUseCase
import com.example.siliguide.domain.usecase.FetchSensorsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    private val analyzePlantUseCase: AnalyzePlantUseCase,
    private val fetchSensorsUseCase: FetchSensorsUseCase,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    var ruleResult by mutableStateOf<RuleResult?>(null)
        private set
    var prediction by mutableStateOf<Prediction?>(null)
        private set
    var analysisError by mutableStateOf<String?>(null)
        private set
    var isAnalyzing by mutableStateOf(false)
        private set

    fun analyzeImage(bitmap: Bitmap) {
        isAnalyzing = true
        analysisError = null // Clear previous errors

        viewModelScope.launch(Dispatchers.Default) {
            try {
                // 1. Check if it's a leaf
                val leafChecker = LabuyoLeafChecker(context)
                val isLabuyo = try { leafChecker.isLeaf(bitmap) } finally { leafChecker.close() }

                if (isLabuyo) {
                    // 2. Run ML Classification
                    val classifier = LeafClassifier(context)
                    val finalResult = try { classifier.predict(bitmap) } finally { classifier.close() }

                    // 3. Try to get Sensor Data
                    val sensorData = try {
                        val data = fetchSensorsUseCase()
                        Log.d("AnalysisViewModel", "Sensor Data Received: $data")
                        data
                    } catch (e: Exception) {
                        // LOG THE EXACT ERROR: This will tell you if it's a Timeout or Wrong IP
                        Log.e("AnalysisViewModel", "CRITICAL: Sensor Fetch Failed: ${e.message}", e)
                        null 
                    }

                    withContext(Dispatchers.Main) {
                        prediction = finalResult
                        // 4. Pass results to Rule Engine
                        ruleResult = analyzePlantUseCase(finalResult.label, sensorData)
                        isAnalyzing = false
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        analysisError = "Not a Siling Labuyo leaf. Please retake."
                        isAnalyzing = false
                    }
                }
            } catch (e: Exception) {
                Log.e("AnalysisViewModel", "Analysis Pipeline Crashed", e)
                withContext(Dispatchers.Main) {
                    analysisError = "Analysis failed: ${e.localizedMessage}"
                    isAnalyzing = false
                }
            }
        }
    }
}