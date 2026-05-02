package com.example.siliguide.domain.engine

data class RuleResult(
    val condition: String? = null,
    val symptoms: List<String>,
    val possibleCauses: List<String>,
    val recommendations: List<String> = emptyList(),
    val isUsingSensors: Boolean
)