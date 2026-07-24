package com.example.siliguide.presentation.features.analysis

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.siliguide.core.ml.LeafSegmenter
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
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import com.example.siliguide.core.ml.DetectionRenderer
import java.io.File

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

    // Replaced the old Prediction object with explicit state variables
    var currentPredictionLabel by mutableStateOf<String?>(null)
        private set
    var currentPredictionConfidence by mutableFloatStateOf(0f)
        private set

    var analysisError by mutableStateOf<String?>(null)
        private set
    var isAnalyzing by mutableStateOf(false)
        private set

    // Actual visual image the UI will draw containing all segmentation masks
    var segmentationOverlay by mutableStateOf<Bitmap?>(null)
        private set

    var sensorUiState by mutableStateOf<List<SensorRowData>>(emptyList())
        private set

    var predictionName by mutableStateOf<String?>(null)
        private set

    private var lastSensorData: SensorResponse? = null

    // Retrieve the ID from the navigation arguments
    private val deviceId: String? = savedStateHandle["deviceId"]

    // Map your YOLO class IDs to String labels (Ensure this matches your model's training order)
    private val classNames = listOf("curling", "healthy", "spots", "yellowing")

    fun analyzeImage(bitmap: Bitmap) {
        isAnalyzing = true
        analysisError = null

        // Explicitly reset ALL states so old masks/results don't linger on the screen
        currentPredictionLabel = null
        currentPredictionConfidence = 0f
        ruleResult = null
        segmentationOverlay = null
        sensorUiState = emptyList()
        predictionName = null

        viewModelScope.launch(Dispatchers.Default) {
            try {
                // Run YOLO Instance Segmentation
                val segmenter = LeafSegmenter(context, "models/best.tflite")
                val detections = try {
                    segmenter.segment(bitmap)
                } finally {
                    segmenter.close() // Always close to prevent memory leaks
                }

                // --- LOGGING OUTPUTS FOR DEBUGGING ---
                Log.d("AnalysisViewModel", "========== YOLO SEGMENTATION RESULTS ==========")
                Log.d("AnalysisViewModel", "Total Objects Detected: ${detections.size}")

                if (detections.isEmpty()) {
                    Log.w("AnalysisViewModel", "No leaves or diseases detected.")
                    withContext(Dispatchers.Main) {
                        analysisError = "No Siling Labuyo leaves or distinct features detected. Please retake the photo."
                        isAnalyzing = false
                    }
                    return@launch
                }

                var hasCurling = false
                var hasSpots = false
                var hasYellowing = false
                var hasHealthy = false

                var finalConfidence = 0f

                for ((index, det) in detections.withIndex()) {

                    val label = classNames.getOrElse(det.classId) { "unknown" }

                    Log.d(
                        "AnalysisViewModel",
                        "Detection #$index -> Class: $label | Conf: ${det.confidence} | Box: ${det.box}"
                    )

                    finalConfidence = maxOf(finalConfidence, det.confidence)

                    when (label) {
                        "curling" -> hasCurling = true
                        "spots" -> hasSpots = true
                        "yellowing" -> hasYellowing = true
                        "healthy" -> hasHealthy = true
                    }
                }

                Log.d("AnalysisViewModel", "===============================================")

                val renderer = DetectionRenderer()

                val overlay = renderer.render(
                    imageWidth = bitmap.width,
                    imageHeight = bitmap.height,
                    detections = detections,
                    classNames = classNames
                )

                val file = File(context.cacheDir, "overlay.png")

                file.outputStream().use {
                    overlay.compress(Bitmap.CompressFormat.PNG, 100, it)
                }

                Log.d("Overlay", file.absolutePath)

                Log.d(
                    "AnalysisViewModel",
                    "Bitmap size = ${bitmap.width} x ${bitmap.height}"
                )

                Log.d(
                    "AnalysisViewModel",
                    "Overlay size = ${overlay.width} x ${overlay.height}"
                )

                val finalLabel = when {
                    hasCurling -> "curling"
                    hasSpots -> "spots"
                    hasYellowing -> "yellowing"
                    hasHealthy -> "healthy"
                    else -> "unknown"
                }

                // 3. Try to get Sensor Data
                val sensorData = try {
                    val data = fetchSensorsUseCase(deviceId)
                    Log.d("AnalysisViewModel", "Sensor Data Received: $data")
                    data
                } catch (e: Exception) {
                    Log.e("AnalysisViewModel", "CRITICAL: Sensor Fetch Failed: ${e.message}", e)
                    null
                }

                // 4. Update UI State
                withContext(Dispatchers.Main) {
                    currentPredictionLabel = finalLabel
                    currentPredictionConfidence = finalConfidence
                    predictionName = formatPrediction(finalLabel)

                    // Pass the combined transparent mask image to the UI
                    segmentationOverlay = overlay

                    // Map the raw data into UI State
                    if (sensorData != null) {
                        sensorUiState = buildSensorUiState(sensorData)
                        lastSensorData = sensorData
                    }

                    // Pass results to Rule Engine using the most confident detection
                    ruleResult = analyzePlantUseCase(finalLabel, sensorData)
                    isAnalyzing = false
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
        val label = currentPredictionLabel ?: return
        val confidence = currentPredictionConfidence
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
                predictedClass = label,
                confidence = confidence.toDouble(),

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

                isPartial = (lastSensorData == null),
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
        return when (string.lowercase()) {
            "curling" -> "Leaf Curling"
            "healthy" -> "Healthy Leaf"
            "spots" -> "Leaf Spots"
            "yellowing" -> "Yellowing Leaf"
            else -> "Unknown"
        }
    }
}