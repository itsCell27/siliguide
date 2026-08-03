package com.example.siliguide.presentation.features.analysis

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
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
import com.example.siliguide.data.cache.AnalysisCache
import com.example.siliguide.data.cache.AnalysisCacheEntry
import com.example.siliguide.data.local.mapper.JsonMapper
import com.example.siliguide.domain.model.BoundingBox
import com.example.siliguide.domain.model.SegmentationDetection
import com.example.siliguide.domain.usecase.GetAnalysisSessionByIdUseCase
import java.io.File

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    private val analyzePlantUseCase: AnalyzePlantUseCase,
    private val fetchSensorsUseCase: FetchSensorsUseCase,
    private val saveAnalysisSessionUseCase: SaveAnalysisSessionUseCase,
    private val authRepository: AuthRepository,
    private val savedStateHandle: SavedStateHandle,
    private val internalImageStorage: InternalImageStorage,
    private val analysisCache: AnalysisCache,
    private val getAnalysisSessionByIdUseCase: GetAnalysisSessionByIdUseCase,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    var analysisUiState by mutableStateOf(
        AnalysisUiState()
    )
        private set

    private var lastSensorData: SensorResponse? = null

    // Retrieve the ID from the navigation arguments
    private val deviceId: String? = savedStateHandle["deviceId"]
    private val plantId: String? = savedStateHandle["plantId"]

    // Map your YOLO class IDs to String labels (Ensure this matches your model's training order)
    private val classNames = listOf("curling", "healthy", "spots", "yellowing")

    // Tells the ViewModel the image is already initialized
    private var initializedImageUri: String? = null

    private var currentDetections: List<LeafSegmenter.Detection> = emptyList()

    init {
        Log.d(
            "AnalysisViewModel",
            "ViewModel created: ${this.hashCode()}"
        )
    }

    /**
     * Initializes the analysis for a newly opened image.
     *
     * This prevents the same image from being analyzed multiple times when
     * AnalysisResultScreen is recreated whether after locking/unlocking the phone
     * or other Compose lifecycle events. If the image has already been
     * initialized, the function returns immediately.
     */
    fun initialize(
        imageUri: String
    ) {
        Log.d(
            "AnalysisViewModel",
            "initialize() hash=${this.hashCode()} initializedImageUri=$initializedImageUri"
        )

        // Same ViewModel + same image -> do nothing
        if (initializedImageUri == imageUri) {
            Log.d(
                "AnalysisViewModel",
                "Already initialized for this image."
            )
            return
        }

        val cacheKey = AnalysisCacheKey(
            imageUri = imageUri,
            plantId = plantId,
            deviceId = deviceId
        )

        val cached = analysisCache.get(cacheKey)

        // If we already analyzed this image before,
        // restore everything and skip YOLO.
        if (cached != null) {

            applyUiState(
                state = cached.uiState,
                sensorData = cached.sensorData
            )

            initializedImageUri = imageUri

            Log.d(
                "AnalysisViewModel",
                "Analysis restored from cache."
            )

            return
        }

        Log.d(
            "AnalysisViewModel",
            "No cached analysis found. Running YOLO..."
        )

        initializedImageUri = imageUri

        val uri = imageUri.toUri()

        val stream = context.contentResolver.openInputStream(uri)

        val bitmap = BitmapFactory.decodeStream(stream)

        stream?.close()

        if (bitmap != null) {
            analyzeImage(bitmap)
        } else {
            analysisUiState = analysisUiState.copy(
                isLoading = false,
                analysisError = "Failed to load image."
            )
        }
    }

    fun analyzeImage(bitmap: Bitmap) {

        // clear analysis data first
        currentDetections = emptyList()
        analysisUiState = AnalysisUiState(
            isLoading = true
        )
        analysisUiState = analysisUiState.copy(
            ruleResult = null,
            analysisError = null,

            predictionLabel = null,
            predictionConfidence = 0f,

            predictionName = null,
            segmentationOverlay = null,
            sensorUiState = emptyList()
        )

        viewModelScope.launch(Dispatchers.Default) {
            try {
                // Run YOLO Instance Segmentation
                val segmenter = LeafSegmenter(context, "models/best.tflite")
                val detections = segmenter.use { segmenter ->
                    segmenter.segment(bitmap)
                }
                currentDetections = detections


                // --- LOGGING OUTPUTS FOR DEBUGGING ---
                Log.d("AnalysisViewModel", "========== YOLO SEGMENTATION RESULTS ==========")
                Log.d("AnalysisViewModel", "Total Objects Detected: ${detections.size}")

                if (detections.isEmpty()) {

                    Log.w("AnalysisViewModel", "No leaves or diseases detected.")

                    withContext(Dispatchers.Main) {

                        val errorMessage =
                            "No Siling Labuyo leaves or distinct features detected. Please retake the photo."

                        analysisUiState = analysisUiState.copy(
                            bitmap = bitmap,
                            analysisError = errorMessage,
                            isLoading = false
                        )


                        // Cache this failed analysis so we don't run YOLO again
                        val imageUri = initializedImageUri ?: return@withContext

                        val cacheKey = AnalysisCacheKey(
                            imageUri = imageUri,
                            plantId = plantId,
                            deviceId = deviceId
                        )

                        analysisCache.put(
                            AnalysisCacheEntry(
                                key = cacheKey,
                                uiState = AnalysisUiState(
                                    predictionLabel = null,
                                    predictionName = null,
                                    predictionConfidence = 0f,
                                    ruleResult = null,
                                    segmentationOverlay = null,
                                    sensorUiState = emptyList(),
                                    isSoilSensorOk = false,
                                    isAirTempSensorOk = false,
                                    analysisError = errorMessage,
                                    isLoading = false
                                ),
                                sensorData = null
                            )
                        )

                        Log.d(
                            "AnalysisViewModel",
                            "Failed analysis cached successfully."
                        )
                    }

                    return@launch
                }

                detections.forEach { detection ->
                    Log.d("AnalysisViewModel", "Detection: $detection")
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
                val sensorData = if (deviceId != null) {
                    try {
                        val data = fetchSensorsUseCase(deviceId)
                        Log.d("AnalysisViewModel", "Sensor Data Received: $data")
                        data
                    } catch (e: Exception) {
                        Log.e(
                            "AnalysisViewModel",
                            "Sensor Fetch Failed: ${e.message}",
                            e
                        )
                        null
                    }
                } else {
                    Log.d(
                        "AnalysisViewModel",
                        "No IoT device assigned. Skipping sensor fetch."
                    )
                    null
                }

                // 4. Update UI State
                withContext(Dispatchers.Main) {

                    // Pass results to Rule Engine using the most confident detection
                    val newRuleResult = analyzePlantUseCase(
                        finalLabel,
                        sensorData
                    )

                    // Map the raw data into UI State
                    val newSensorUiState =
                        sensorData?.let(::buildSensorUiState) ?: emptyList()

                    applyUiState(
                        state = AnalysisUiState(
                            isLoading = false,
                            predictionLabel = finalLabel,
                            predictionName = formatPrediction(finalLabel),
                            predictionConfidence = finalConfidence,
                            ruleResult = newRuleResult,

                            // pass the image to UI
                            bitmap = bitmap,
                            // Pass the combined transparent mask image to the UI
                            segmentationOverlay = overlay,

                            sensorUiState = newSensorUiState,
                            isSoilSensorOk = sensorData?.system?.sensorOK ?: false,
                            isAirTempSensorOk = sensorData?.system?.dhtOK ?: false,
                            analysisError = null
                        ),
                        sensorData = sensorData
                    )

                    // store in cache
                    val imageUri = initializedImageUri ?: return@withContext
                    val cacheKey = AnalysisCacheKey(
                        imageUri = imageUri,
                        plantId = plantId,
                        deviceId = deviceId
                    )

                    analysisCache.put(
                        AnalysisCacheEntry(
                            key = cacheKey,
                            uiState = analysisUiState,
                            sensorData = lastSensorData
                        )
                    )

                    Log.d(
                        "AnalysisViewModel",
                        "Analysis cached successfully."
                    )
                }

            } catch (e: Exception) {
                Log.e("AnalysisViewModel", "Analysis Pipeline Crashed", e)
                withContext(Dispatchers.Main) {
                    analysisUiState = analysisUiState.copy(
                        analysisError = "Analysis failed: ${e.localizedMessage}",
                        isLoading = false
                    )

                }
            }
        }
    }

    /**
     * Packages the current analysis state into a session and saves it to the local database.
     */
    fun saveCurrentAnalysis(plantId: String, imagePath: String, onSuccess: () -> Unit) {
        val currentRuleResult = analysisUiState.ruleResult ?: return
        val label = analysisUiState.predictionLabel ?: return
        val confidence = analysisUiState.predictionConfidence
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

                segmentationResult = currentDetections.map { detection ->
                    SegmentationDetection(
                        classId = detection.classId,
                        label = classNames.getOrElse(detection.classId) { "unknown" },
                        confidence = detection.confidence,
                        boundingBox = BoundingBox(
                            left = detection.box.left,
                            top = detection.box.top,
                            right = detection.box.right,
                            bottom = detection.box.bottom
                        ),
                        polygon = detection.polygon
                    )
                },

                // Data from Rule Engine
                plantCondition = currentRuleResult.condition,
                plantConditionDescription = currentRuleResult.conditionDescription,
                possibleCauses = JsonMapper.encodeStringList(currentRuleResult.possibleCauses),
                characteristics = JsonMapper.encodeStringList(currentRuleResult.characteristics),
                recommendedActions = JsonMapper.encodeStringList(currentRuleResult.recommendations),

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

    private fun buildSensorUiState(session: AnalysisSession): List<SensorRowData> {

        if (session.isPartial) {
            return emptyList()
        }

        return listOf(
            SensorRowData(
                "Soil Temp",
                "${session.soilTemp}°C",
                ThresholdEngine.evaluate(
                    session.soilTemp ?: 0.0,
                    SiliGuideThresholds.SOIL_TEMP
                ),
                SensorIconType.SOIL_TEMP
            ),

            SensorRowData(
                "Soil pH",
                "${session.soilPh}",
                ThresholdEngine.evaluate(
                    session.soilPh ?: 0.0,
                    SiliGuideThresholds.SOIL_PH
                ),
                SensorIconType.SOIL_PH
            ),

            SensorRowData(
                "Soil EC",
                "${session.electricalConductivity} µS/cm",
                ThresholdEngine.evaluate(
                    (session.electricalConductivity ?: 0).toDouble(),
                    SiliGuideThresholds.SOIL_EC
                ),
                SensorIconType.SOIL_EC
            ),

            SensorRowData(
                "Soil Moisture",
                "${session.soilMoisture}%",
                ThresholdEngine.evaluate(
                    session.soilMoisture ?: 0.0,
                    SiliGuideThresholds.SOIL_MOISTURE
                ),
                SensorIconType.SOIL_MOISTURE
            ),

            SensorRowData(
                "Soil Nitrogen (N)",
                "${session.nitrogen} mg/kg",
                ThresholdEngine.evaluate(
                    (session.nitrogen ?: 0).toDouble(),
                    SiliGuideThresholds.NITROGEN
                ),
                SensorIconType.LETTER_N
            ),

            SensorRowData(
                "Soil Phosphorus (P)",
                "${session.phosphorus} mg/kg",
                ThresholdEngine.evaluate(
                    (session.phosphorus ?: 0).toDouble(),
                    SiliGuideThresholds.PHOSPHORUS
                ),
                SensorIconType.LETTER_P
            ),

            SensorRowData(
                "Potassium (K)",
                "${session.potassium} mg/kg",
                ThresholdEngine.evaluate(
                    (session.potassium ?: 0).toDouble(),
                    SiliGuideThresholds.POTASSIUM
                ),
                SensorIconType.LETTER_K
            ),

            SensorRowData(
                "Light",
                "${session.lightIntensity} lux",
                ThresholdEngine.evaluate(
                    (session.lightIntensity ?: 0).toDouble(),
                    SiliGuideThresholds.LIGHT_INTENSITY
                ),
                SensorIconType.LIGHT
            ),

            SensorRowData(
                "Air Temp",
                "${session.airTemp}°C",
                ThresholdEngine.evaluate(
                    session.airTemp ?: 0.0,
                    SiliGuideThresholds.AIR_TEMP
                ),
                SensorIconType.AIR_TEMP
            )
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

    private fun applyUiState(
        state: AnalysisUiState,
        sensorData: SensorResponse? = null
    ) {

        analysisUiState = state
        lastSensorData = sensorData
    }

    private fun buildUiStateFromSession(
        session: AnalysisSession,
        bitmap: Bitmap?,
        overlay: Bitmap?
    ): AnalysisUiState {

        val predictionLabel = session.predictedClass

        val predictionName = formatPrediction(
            session.predictedClass
        )

        val ruleResult = RuleResult(
            condition = session.plantCondition,
            conditionDescription = session.plantConditionDescription,
            characteristics = JsonMapper.decodeStringList(session.characteristics),
            possibleCauses = JsonMapper.decodeStringList(session.possibleCauses),
            recommendations = JsonMapper.decodeStringList(session.recommendedActions),
            isUsingSensors = !session.isPartial
        )

        return AnalysisUiState(
            isLoading = false,
            imagePath = session.imagePath,
            predictionLabel = predictionLabel,
            predictionName = predictionName,
            predictionConfidence = session.confidence.toFloat(),
            ruleResult = ruleResult,

            bitmap = bitmap,
            segmentationOverlay = overlay,

            sensorUiState = buildSensorUiState(session),

            isSoilSensorOk = !session.isPartial,
            isAirTempSensorOk = !session.isPartial,

            analysisError = null
        )

    }

    fun loadAnalysisSession(sessionId: String) {

        viewModelScope.launch {

            analysisUiState = analysisUiState.copy(
                isLoading = true
            )

            val t0 = System.currentTimeMillis()

            val session = getAnalysisSessionByIdUseCase(sessionId)

            Log.d("PERF", "DB = ${System.currentTimeMillis()-t0}ms")

            if (session == null) {
                analysisUiState = analysisUiState.copy(
                    isLoading = false,
                    analysisError = "Analysis session not found."
                )
                return@launch
            }

            Log.d(
                "AnalysisViewModel",
                "Loaded session: $session"
            )

            val (bitmap, overlay) = withContext(Dispatchers.Default) {

                val bitmap = BitmapFactory.decodeFile(session.imagePath)

                val overlay =
                    bitmap?.let {

                        session.segmentationResult?.let { detections ->

                            DetectionRenderer().render(
                                imageWidth = it.width,
                                imageHeight = it.height,
                                detections = detections
                            )
                        }
                    }

                bitmap to overlay
            }

            val t3 = System.currentTimeMillis()
            applyUiState(
                state = buildUiStateFromSession(
                    session = session,
                    bitmap = bitmap,
                    overlay = overlay
                ),
                sensorData = null
            )
            Log.d("PERF", "UI = ${System.currentTimeMillis()-t3}ms")
        }
    }

    fun clearCurrentCache() {

        val imageUri = initializedImageUri ?: return

        val cacheKey = AnalysisCacheKey(
            imageUri = imageUri,
            plantId = plantId,
            deviceId = deviceId
        )

        analysisCache.remove(cacheKey)

        Log.d(
            "AnalysisViewModel",
            "Analysis cache removed for image: $imageUri"
        )

        initializedImageUri = null
    }
}