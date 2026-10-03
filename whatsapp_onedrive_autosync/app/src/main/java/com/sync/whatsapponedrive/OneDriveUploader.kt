package com.sync.whatsapponedrive

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object OneDriveUploader {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun uploadFile(
        accessToken: String,
        folderName: String,
        fileName: String,
        file: File
    ): Boolean {
        try {
            val encodedFolder = URLEncoder.encode(folderName, "UTF-8").replace("+", "%20")
            val encodedFile = URLEncoder.encode(fileName, "UTF-8").replace("+", "%20")

            // Microsoft Graph API Endpoint:
            // PUT /me/drive/root:/WhatsApp_Backup/{CustomerName}/{FileName}:/content
            val url = "https://graph.microsoft.com/v1.0/me/drive/root:/WhatsApp_Backup/$encodedFolder/$encodedFile:/content"

            val mediaType = "image/jpeg".toMediaTypeOrNull()
            val requestBody = file.asRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .put(requestBody)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "image/jpeg")
                .build()

            val response = client.newCall(request).execute()
            return response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}
