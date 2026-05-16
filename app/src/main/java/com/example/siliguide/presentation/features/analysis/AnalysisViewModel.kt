    package com.example.siliguide.presentation.features.analysis

    import android.content.Context
    import android.graphics.Bitmap
    import android.util.Log
    import androidx.compose.runtime.getValue
    import androidx.compose.runtime.mutableStateOf
    import androidx.compose.runtime.setValue
    import androidx.lifecycle.ViewModel
    import androidx.lifecycle.viewModelScope
    import com.example.siliguide.core.ml.GatekeeperResult
    import com.example.siliguide.core.ml.HeatmapGenerator
    import com.example.siliguide.core.ml.LabuyoLeafChecker
    import com.example.siliguide.core.ml.LeafClassifier
    import com.example.siliguide.core.ml.Prediction
    import com.example.siliguide.domain.engine.RuleResult
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

        var gatekeeperFeatureMaps by mutableStateOf<Array<Array<Array<FloatArray>>>?>(null)
            private set

        // actual visual image the UI will draw
        var heatmapOverlay by mutableStateOf<Bitmap?>(null)
            private set

        fun analyzeImage(bitmap: Bitmap) {
            isAnalyzing = true
            analysisError = null

            // FIX: Explicitly reset ALL states so old heatmaps/results don't linger on the screen
            gatekeeperFeatureMaps = null
            prediction = null
            ruleResult = null
            heatmapOverlay = null

            viewModelScope.launch(Dispatchers.Default) {
                try {
                    // Instantiate Heatmap Generator once
                    val heatmapGen = HeatmapGenerator(context)

                    // 1. Run the Gatekeeper
                    val leafChecker = LabuyoLeafChecker(context)
                    val gatekeeperResult: GatekeeperResult = try {
                        leafChecker.checkLeaf(bitmap)
                    } finally {
                        leafChecker.close()
                    }

                    if (gatekeeperResult.isLeaf) {
                        // 2. Run ML Classification
                        val classifier = LeafClassifier(context)
                        val finalResult = try { classifier.predict(bitmap) } finally { classifier.close() }

                        // Generate LeafClassifier Heatmap Overlay
                        // the index of the predicted label to get the right weights
                        val classNames = listOf("curling", "healthy", "spots", "yellowing")
                        val classIndex = classNames.indexOf(finalResult.label)

                        val generatedOverlay = heatmapGen.generateHeatmap(
                            featureMaps = finalResult.featureMaps,
                            predictedClassIndex = classIndex,
                            csvFilename = "weights/leaf_classifier_model2_dense_weights.csv"
                        )

                        // 3. Try to get Sensor Data
                        val sensorData = try {
                            val data = fetchSensorsUseCase()
                            Log.d("AnalysisViewModel", "Sensor Data Received: $data")
                            data
                        } catch (e: Exception) {
                            Log.e("AnalysisViewModel", "CRITICAL: Sensor Fetch Failed: ${e.message}", e)
                            null
                        }

                        withContext(Dispatchers.Main) {
                            prediction = finalResult
                            heatmapOverlay = generatedOverlay // Pass the generated image to the UI

                            // 4. Pass results to Rule Engine
                            ruleResult = analyzePlantUseCase(finalResult.label, sensorData)
                            isAnalyzing = false
                        }
                    } else {

                        val generatedErrorOverlay = heatmapGen.generateHeatmap(
                            featureMaps = gatekeeperResult.featureMaps,
                            predictedClassIndex = 0, // 0 is the "non_siling_labuyo_leaf" class index
                            csvFilename = "weights/leaf_checker2_dense_weights.csv"
                        )

                        withContext(Dispatchers.Main) {
                            gatekeeperFeatureMaps = gatekeeperResult.featureMaps
                            heatmapOverlay = generatedErrorOverlay // Pass the error image to the UI
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