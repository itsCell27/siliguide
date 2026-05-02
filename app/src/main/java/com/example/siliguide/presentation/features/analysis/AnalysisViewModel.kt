package com.example.siliguide.presentation.features.analysis

import android.content.Context
import android.graphics.Bitmap
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
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    private val analyzePlantUseCase: AnalyzePlantUseCase,
    @param:ApplicationContext private val context: Context// Need Context to load ML Models!
) : ViewModel() {

    var ruleResult by mutableStateOf<RuleResult?>(null)
        private set
    var prediction by mutableStateOf<Prediction?>(null)
        private set
    var analysisError by mutableStateOf<String?>(null)
        private set
    var isAnalyzing by mutableStateOf(false)
        private set

    fun analyzeImage(bitmap: Bitmap, sensorData: SensorResponse?) {
        isAnalyzing = true
        analysisError = null

        viewModelScope.launch(Dispatchers.Default) {
            try {
                // 1. Run Gatekeeper
                val leafChecker = LabuyoLeafChecker(context)
                val isLabuyo = try { leafChecker.isLeaf(bitmap) } finally { leafChecker.close() }

                if (isLabuyo) {
                    // 2. Run Classifier
                    val classifier = LeafClassifier(context)
                    val finalResult = try { classifier.predict(bitmap) } finally { classifier.close() }

                    withContext(Dispatchers.Main) {
                        prediction = finalResult
                        // 3. Trigger Rule Engine
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
                withContext(Dispatchers.Main) {
                    analysisError = "Analysis failed: ${e.localizedMessage}"
                    isAnalyzing = false
                }
            }
        }
    }
}