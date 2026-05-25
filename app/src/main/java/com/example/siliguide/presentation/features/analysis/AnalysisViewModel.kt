    package com.example.siliguide.presentation.features.analysis

    import android.content.Context
    import android.graphics.Bitmap
    import android.util.Log
    import androidx.compose.runtime.getValue
    import androidx.compose.runtime.mutableStateOf
    import androidx.compose.runtime.setValue
    import androidx.lifecycle.SavedStateHandle
    import androidx.lifecycle.ViewModel
    import androidx.lifecycle.viewModelScope
    import com.example.siliguide.core.ml.GatekeeperResult
    import com.example.siliguide.core.ml.HeatmapGenerator
    import com.example.siliguide.core.ml.LabuyoLeafChecker
    import com.example.siliguide.core.ml.LeafClassifier
    import com.example.siliguide.core.ml.Prediction
    import com.example.siliguide.data.local.storage.InternalImageStorage
    import com.example.siliguide.domain.engine.RuleResult
    import com.example.siliguide.domain.engine.SiliGuideThresholds
    import com.example.siliguide.domain.engine.ThresholdEngine
    import com.example.siliguide.domain.model.AnalysisSession
    import com.example.siliguide.domain.usecase.AnalyzePlantUseCase
    import com.example.siliguide.domain.usecase.FetchSensorsUseCase
    import com.example.siliguide.domain.model.SensorResponse
    import com.example.siliguide.domain.repository.AuthRepository
    import com.example.siliguide.domain.usecase.SaveAnalysisSessionUseCase
    import com.example.siliguide.presentation.features.auth.AuthViewModel
    import dagger.hilt.android.lifecycle.HiltViewModel
    import dagger.hilt.android.qualifiers.ApplicationContext
    import kotlinx.coroutines.Dispatchers
    import kotlinx.coroutines.launch
    import kotlinx.coroutines.withContext
    import java.util.UUID
    import javax.inject.Inject

    @HiltViewModel
    class AnalysisViewModel @Inject constructor(
        private val analyzePlantUseCase: AnalyzePlantUseCase,
        private val fetchSensorsUseCase: FetchSensorsUseCase,
        private val saveAnalysisSessionUseCase: SaveAnalysisSessionUseCase,
        private val authRepository: AuthRepository,
        private val savedStateHandle: SavedStateHandle,
        private val internalImageStorage: InternalImageStorage,
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

        var sensorUiState by mutableStateOf<List<SensorRowData>>(emptyList())
            private set

        var predictionName by mutableStateOf<String?>(null)
            private set

        private var lastSensorData: SensorResponse? = null

        // Retrieve the ID from the navigation arguments
        private val deviceId: String? = savedStateHandle["deviceId"]

        fun analyzeImage(bitmap: Bitmap) {
            isAnalyzing = true
            analysisError = null

            // FIX: Explicitly reset ALL states so old heatmaps/results don't linger on the screen
            gatekeeperFeatureMaps = null
            prediction = null
            ruleResult = null
            heatmapOverlay = null

            sensorUiState = emptyList()

            predictionName = null

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
                            val data = fetchSensorsUseCase(deviceId)
                            Log.d("AnalysisViewModel", "Sensor Data Received: $data")
                            data
                        } catch (e: Exception) {
                            Log.e("AnalysisViewModel", "CRITICAL: Sensor Fetch Failed: ${e.message}", e)
                            null
                        }

                        withContext(Dispatchers.Main) {
                            prediction = finalResult
                            predictionName = formatPrediction(finalResult.label)
                            heatmapOverlay = generatedOverlay // Pass the generated image to the UI

                            // Map the raw data into UI State
                            if (sensorData != null) {
                                sensorUiState = buildSensorUiState(sensorData)
                                lastSensorData = sensorData
                            }

                            // 4. Pass results to Rule Engine
                            ruleResult = analyzePlantUseCase(finalResult.label, sensorData)
                            isAnalyzing = false
                        }
                    } else {

                        val generatedErrorOverlay = heatmapGen.generateHeatmap(
                            featureMaps = gatekeeperResult.featureMaps,
                            predictedClassIndex = 0, // 0 is the "non_siling_labuyo_leaf" class index
                            csvFilename = "weights/leaf_checker_model3_dense_weights.csv"
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

        /**
         * Packages the current analysis state into a session and saves it to the local database.
         */
        fun saveCurrentAnalysis(plantId: String, imagePath: String, onSuccess: () -> Unit) {
            val currentRuleResult = ruleResult ?: return
            val currentPrediction = prediction ?: return
            val sessionId = UUID.randomUUID().toString()

            viewModelScope.launch {
                val userId = authRepository.getUserId() ?: return@launch

                val permanentPath = withContext(Dispatchers.IO) {
                    internalImageStorage.saveImagePermanently(imagePath, sessionId)
                } ?: imagePath
                val session = AnalysisSession(
                    sessionId = sessionId,
                    plantId = plantId,
                    deviceId = this@AnalysisViewModel.deviceId,
                    userId = userId,
                    sessionTimestamp = System.currentTimeMillis(),
                    imagePath = permanentPath,
                    predictedClass = currentPrediction.label,
                    confidence = currentPrediction.confidence.toDouble(),

                    // Data from Rule Engine
                    plantCondition = currentRuleResult.condition,
                    possibleCauses = currentRuleResult.possibleCauses.joinToString(", "),
                    characteristics = currentRuleResult.characteristics.joinToString(", "),
                    recommendedActions = currentRuleResult.recommendations.joinToString(", "),

                    // Sensor values
                    soilMoisture = lastSensorData?.soil?.moisture,
                    soilPh = lastSensorData?.soil?.ph,
                    soilTemp = lastSensorData?.soil?.temperature,
                    electricalConductivity = lastSensorData?.soil?.ec,
                    nitrogen = lastSensorData?.soil?.nitrogen,
                    phosphorus = lastSensorData?.soil?.phosphorus,
                    potassium = lastSensorData?.soil?.potassium,
                    lightIntensity = lastSensorData?.environment?.light,
                    airTemp = lastSensorData?.environment?.airTemperature,

                    isPartial = (lastSensorData == null), // It's partial if no sensor data was captured,
                    isSynced = false
                )

                try {
                    saveAnalysisSessionUseCase(session)
                    withContext(Dispatchers.Main) {
                        onSuccess()
                    }
                } catch (e: Exception) {
                    Log.e("AnalysisViewModel", "Failed to save session", e)
                }
            }
        }


        // Strict MVVM Mapping: Turns Raw Data into UI-ready rows
        private fun buildSensorUiState(data: SensorResponse): List<SensorRowData> {
            return listOf(
                SensorRowData("Soil Temp", "${data.soil.temperature}°C", ThresholdEngine.evaluate(data.soil.temperature, SiliGuideThresholds.SOIL_TEMP), SensorIconType.SOIL_TEMP),
                SensorRowData("Soil pH", "${data.soil.ph}", ThresholdEngine.evaluate(data.soil.ph, SiliGuideThresholds.SOIL_PH), SensorIconType.SOIL_PH),
                SensorRowData("Soil EC", "${data.soil.ec} µS/cm", ThresholdEngine.evaluate(data.soil.ec.toDouble(), SiliGuideThresholds.SOIL_EC), SensorIconType.SOIL_EC),
                SensorRowData("Soil Moisture", "${data.soil.moisture}%", ThresholdEngine.evaluate(data.soil.moisture, SiliGuideThresholds.SOIL_MOISTURE), SensorIconType.SOIL_MOISTURE),
                SensorRowData("Soil Nitrogen (N)", "${data.soil.nitrogen} mg/kg", ThresholdEngine.evaluate(data.soil.nitrogen.toDouble(), SiliGuideThresholds.NITROGEN), SensorIconType.LETTER_N),
                SensorRowData("Soil Phosphorus (P)", "${data.soil.phosphorus} mg/kg", ThresholdEngine.evaluate(data.soil.phosphorus.toDouble(), SiliGuideThresholds.PHOSPHORUS), SensorIconType.LETTER_P),
                SensorRowData("Potassium (K)", "${data.soil.potassium} mg/kg", ThresholdEngine.evaluate(data.soil.potassium.toDouble(), SiliGuideThresholds.POTASSIUM), SensorIconType.LETTER_K),
                SensorRowData("Light", "${data.environment.light} lux", ThresholdEngine.evaluate(data.environment.light.toDouble(), SiliGuideThresholds.LIGHT_INTENSITY), SensorIconType.LIGHT),
                SensorRowData("Air Temp", "${data.environment.airTemperature}°C", ThresholdEngine.evaluate(data.environment.airTemperature, SiliGuideThresholds.AIR_TEMP), SensorIconType.AIR_TEMP)
            )
        }

        private fun formatPrediction(string: String): String {
            val formattedPrediction = when (string.lowercase()) {
                "curling" -> "Leaf Curling"
                "healthy" -> "Healthy Leaf"
                "spots" -> "Leaf Spots"
                "yellowing" -> "Yellowing Leaf"
                else -> "Unknown"
            }
            return formattedPrediction
        }
    }