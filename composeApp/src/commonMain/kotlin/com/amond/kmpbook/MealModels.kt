package com.amond.kmpbook

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SchoolInfo(
    @SerialName("ATPT_OFCDC_SC_CODE") val officeCode: String,
    @SerialName("ATPT_OFCDC_SC_NM") val officeName: String,
    @SerialName("SD_SCHUL_CODE") val schoolCode: String,
    @SerialName("SCHUL_NM") val schoolName: String,
    @SerialName("SCHUL_KND_SC_NM") val schoolKind: String = "",
    @SerialName("LCTN_SC_NM") val location: String = "",
    @SerialName("ORG_RDNMA") val address: String = ""
)

@Serializable
data class MealInfo(
    @SerialName("ATPT_OFCDC_SC_CODE") val officeCode: String,
    @SerialName("SD_SCHUL_CODE") val schoolCode: String,
    @SerialName("SCHUL_NM") val schoolName: String,
    @SerialName("MMEAL_SC_CODE") val mealTypeCode: String,
    @SerialName("MMEAL_SC_NM") val mealTypeName: String,
    @SerialName("MLSV_YMD") val date: String,
    @SerialName("DDISH_NM") val dishes: String,
    @SerialName("ORPLC_INFO") val origins: String = "",
    @SerialName("CAL_INFO") val calories: String = "",
    @SerialName("NTR_INFO") val nutrition: String = ""
) {
    fun dishList(): List<String> = dishes
        .split("<br/>", "<br>")
        .map { it.trim() }
        .map { it.replace(Regex("\\(.*?\\)"), "").trim() }
        .filter { it.isNotBlank() }

    fun originMap(): Map<String, String> = origins
        .split("<br/>", "<br>")
        .mapNotNull { line ->
            val parts = line.split(":")
            if (parts.size == 2) parts[0].trim() to parts[1].trim() else null
        }
        .toMap()

    fun nutritionList(): List<Pair<String, String>> = nutrition
        .split("<br/>", "<br>")
        .mapNotNull { line ->
            val parts = line.split(":")
            if (parts.size == 2) parts[0].trim() to parts[1].trim() else null
        }
}

@Serializable
data class NeisResultHead(
    @SerialName("list_total_count") val totalCount: Int? = null,
    @SerialName("RESULT") val result: NeisResult? = null
)

@Serializable
data class NeisResult(
    @SerialName("CODE") val code: String,
    @SerialName("MESSAGE") val message: String
)

@Serializable
data class MealServiceResponse(
    @SerialName("mealServiceDietInfo") val data: List<MealServiceBlock>? = null,
    @SerialName("RESULT") val error: NeisResult? = null
)

@Serializable
data class MealServiceBlock(
    @SerialName("head") val head: List<NeisResultHead>? = null,
    @SerialName("row") val row: List<MealInfo>? = null
)

@Serializable
data class SchoolInfoResponse(
    @SerialName("schoolInfo") val data: List<SchoolInfoBlock>? = null,
    @SerialName("RESULT") val error: NeisResult? = null
)

@Serializable
data class SchoolInfoBlock(
    @SerialName("head") val head: List<NeisResultHead>? = null,
    @SerialName("row") val row: List<SchoolInfo>? = null
)

sealed class MealUiState {
    data object Loading : MealUiState()
    data class Success(val meals: List<MealInfo>) : MealUiState()
    data class Empty(val message: String = "급식 정보가 없습니다") : MealUiState()
    data class Error(val message: String) : MealUiState()
}

sealed class SchoolSearchState {
    data object Idle : SchoolSearchState()
    data object Loading : SchoolSearchState()
    data class Success(val schools: List<SchoolInfo>) : SchoolSearchState()
    data class Error(val message: String) : SchoolSearchState()
}