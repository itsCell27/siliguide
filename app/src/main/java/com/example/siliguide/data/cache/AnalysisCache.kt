package com.example.siliguide.data.cache

import com.example.siliguide.presentation.features.analysis.AnalysisCacheKey
import com.example.siliguide.presentation.features.analysis.AnalysisUiState
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalysisCache @Inject constructor() {

    private val cache =
        ConcurrentHashMap<AnalysisCacheKey, AnalysisCacheEntry>()

    fun get(
        key: AnalysisCacheKey
    ): AnalysisCacheEntry? {
        return cache[key]
    }

    fun put(
        entry: AnalysisCacheEntry
    ) {
        cache[entry.key] = entry
    }

    fun remove(key: AnalysisCacheKey) {
        cache.remove(key)
    }

    fun clear() {
        cache.clear()
    }
}