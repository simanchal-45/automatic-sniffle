package com.abrogani.superstream.api.util.old

import com.abrogani.superstream.BuildConfig
import com.flixclusive.core.util.network.json.fromJson
import com.flixclusive.core.util.network.okhttp.HttpMethod
import com.flixclusive.core.util.network.okhttp.formRequest
import com.abrogani.superstream.api.util.old.Constants.API_IV
import com.abrogani.superstream.api.util.old.Constants.API_KEY
import com.abrogani.superstream.api.util.old.Constants.APP_KEY
import com.abrogani.superstream.api.util.old.Constants.APP_VERSION_CODE
import com.abrogani.superstream.api.util.old.Constants.COMMON_HEADERS
import okhttp3.Headers.Companion.toHeaders
import okhttp3.OkHttpClient
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Locale

internal object SuperStreamUtil {
    internal inline fun <reified T : Any> OkHttpClient.superStreamCall(query: String): T? {
        val encryptedQuery = CipherUtil.encrypt(
            str = query,
            key = API_KEY,
            iv = API_IV
        )!!
        val appKeyHash = MD5Util.md5(hash = APP_KEY)!!
        val verify = CipherUtil.getVerify(encryptedQuery, APP_KEY, API_KEY)
        val newBody =
            """{"app_key":"$appKeyHash","verify":"$verify","encrypt_data":"$encryptedQuery"}"""

        val data = mapOf(
            "data" to Base64.getEncoder().encodeToString(newBody.toByteArray()),
            "appid" to "27",
            "platform" to "android",
            "version" to APP_VERSION_CODE,
            "medium" to "Website&token${randomToken()}"
        )

        val errorMessage = "[SuperStream 1]> Failed to fetch SuperStream API"
        val url = BuildConfig.SUPERSTREAM_FIFTH_API
        val response = this.formRequest(
            url = url,
            method = HttpMethod.POST,
            body = data,
            headers = COMMON_HEADERS.toHeaders()
        ).execute()

        val responseBody = response.body?.string()
            ?: throw Exception(errorMessage + " [${response.code}]")

        if (
            responseBody.contains(
                other = """"msg":"success""",
                ignoreCase = true
            ).not()
        ) {
            print(responseBody)
            throw Exception("$errorMessage [${response.code}]")
        }

        if (response.isSuccessful && responseBody.isNotBlank())
            return fromJson(responseBody)
        else throw Exception("$errorMessage: [${responseBody}]")
    }

    // Random 32 length string
    fun randomToken(): String {
        return (0..31).joinToString("") {
            (('0'..'9') + ('a'..'f')).random().toString()
        }
    }

    fun getExpiryDate(): Long {
        // Current time + 12 hours
        return (System.currentTimeMillis() / 1000) + 60 * 60 * 12
    }

    fun String.raiseOnError(lazyMessage: String) {
        if (
            contains(
                other = "error",
                ignoreCase = true
            )
        ) throw Exception(lazyMessage)
    }

    fun String?.toValidReleaseDate(format: String = "MMMM d, yyyy"): String? {
        if (isNullOrBlank())
            return null

        val inputFormat = SimpleDateFormat(format, Locale.US)
        val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        return try {
            val date = inputFormat.parse(this)
            outputFormat.format(date)
        } catch (e: Exception) {
            // throw Exception("Cannot parse release date of show: $this")
            this
        }
    }
}