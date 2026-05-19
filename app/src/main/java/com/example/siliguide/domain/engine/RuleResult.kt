package com.example.siliguide.domain.engine

data class RuleResult(
    val condition: String? = null,
    val conditionDescription: String? = null,
    val characteristics: List<String> = emptyList(),
    val possibleCauses: List<String> = emptyList(),
    val recommendations: List<String> = emptyList(),
    val isUsingSensors: Boolean
)