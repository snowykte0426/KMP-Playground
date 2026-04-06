package com.amond.kmpbook

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

private const val BASE_URL = "https://open.neis.go.kr/hub"

object NeisApi {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    // ContentNegotiation 없이 — Accept 헤더를 붙이지 않아야 NEIS 서버가 정상 응답
    private val client = HttpClient {
        expectSuccess = false
    }

    suspend fun getMeals(
        officeCode: String,
        schoolCode: String,
        date: String,
        apiKey: String = ""
    ): Result<List<MealInfo>> = runCatching {
        val response = client.get("$BASE_URL/mealServiceDietInfo") {
            parameter("Type", "json")
            parameter("pIndex", "1")
            parameter("pSize", "10")
            parameter("ATPT_OFCDC_SC_CODE", officeCode)
            parameter("SD_SCHUL_CODE", schoolCode)
            parameter("MLSV_YMD", date)
            if (apiKey.isNotBlank()) parameter("KEY", apiKey)
        }

        if (!response.status.isSuccess()) {
            throw Exception("서버 오류 (${response.status.value})")
        }

        val body = json.decodeFromString<MealServiceResponse>(response.bodyAsText())

        body.error?.let { err ->
            if (err.code == "INFO-200") return@runCatching emptyList()
            if (err.code != "INFO-000") throw Exception(err.message)
        }

        val blocks = body.data ?: return@runCatching emptyList()

        val resultCode = blocks
            .flatMap { it.head ?: emptyList() }
            .firstOrNull { it.result != null }
            ?.result
        if (resultCode != null && resultCode.code != "INFO-000") {
            if (resultCode.code == "INFO-200") return@runCatching emptyList()
            throw Exception(resultCode.message)
        }

        blocks.flatMap { it.row ?: emptyList() }
    }

    suspend fun searchSchools(
        schoolName: String,
        apiKey: String = ""
    ): Result<List<SchoolInfo>> = runCatching {
        val response = client.get("$BASE_URL/schoolInfo") {
            parameter("Type", "json")
            parameter("pIndex", "1")
            parameter("pSize", "50")
            parameter("SCHUL_NM", schoolName)
            if (apiKey.isNotBlank()) parameter("KEY", apiKey)
        }

        if (!response.status.isSuccess()) {
            throw Exception("서버 오류 (${response.status.value})")
        }

        val body = json.decodeFromString<SchoolInfoResponse>(response.bodyAsText())

        body.error?.let { err ->
            if (err.code == "INFO-200") return@runCatching emptyList()
            if (err.code != "INFO-000") throw Exception(err.message)
        }

        val blocks = body.data ?: return@runCatching emptyList()

        val resultCode = blocks
            .flatMap { it.head ?: emptyList() }
            .firstOrNull { it.result != null }
            ?.result
        if (resultCode != null && resultCode.code != "INFO-000") {
            if (resultCode.code == "INFO-200") return@runCatching emptyList()
            throw Exception(resultCode.message)
        }

        blocks.flatMap { it.row ?: emptyList() }
    }
}