package com.amond.kmpbook

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set

private const val KEY_SCHOOL_CODE = "school_code"
private const val KEY_OFFICE_CODE = "office_code"
private const val KEY_SCHOOL_NAME = "school_name"
private const val KEY_API_KEY = "api_key"

class SchoolPreferences {
    private val settings: Settings = Settings()

    var schoolCode: String
        get() = settings.getString(KEY_SCHOOL_CODE, "")
        set(value) { settings[KEY_SCHOOL_CODE] = value }

    var officeCode: String
        get() = settings.getString(KEY_OFFICE_CODE, "")
        set(value) { settings[KEY_OFFICE_CODE] = value }

    var schoolName: String
        get() = settings.getString(KEY_SCHOOL_NAME, "")
        set(value) { settings[KEY_SCHOOL_NAME] = value }

    var apiKey: String
        get() = settings.getString(KEY_API_KEY, "")
        set(value) { settings[KEY_API_KEY] = value }

    val isConfigured: Boolean
        get() = schoolCode.isNotBlank() && officeCode.isNotBlank()

    fun saveSchool(school: SchoolInfo) {
        schoolCode = school.schoolCode
        officeCode = school.officeCode
        schoolName = school.schoolName
    }

    fun clear() {
        schoolCode = ""
        officeCode = ""
        schoolName = ""
    }
}