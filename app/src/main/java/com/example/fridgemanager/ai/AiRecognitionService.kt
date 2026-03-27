package com.example.fridgemanager.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonObject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

data class RecognitionResult(
    val name: String,
    val quantity: String,
    val expiryInfo: String,   // 原始识别文本，如 "2025/03/15" 或 "保质期12个月"
    val categoryHint: String
)

sealed class AiResult {
    data class Success(val result: RecognitionResult) : AiResult()
    data class Error(val message: String) : AiResult()
}

@Singleton
class AiRecognitionService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(75, java.util.concurrent.TimeUnit.SECONDS)
        .callTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    suspend fun recognizeImage(imageUri: Uri, apiKey: String): AiResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext AiResult.Error("请在设置中配置 AI API Key")

        try {
            val base64Image = encodeImageToBase64(imageUri)
                ?: return@withContext AiResult.Error("无法读取图片")

            val prompt = """
                请识别图片中的食品/食材包装或冰箱内容，以JSON格式返回以下字段：
                {
                  "name": "食品名称（如：纯牛奶、草莓、鸡蛋）",
                  "quantity": "规格或数量（如：250ml、1kg、6个，没有则为空）",
                  "expiry_info": "到期日或保质期原文（如：2025/12/01、保质期12个月，识别不到则为空）",
                  "category": "推荐分类（蔬果/乳制品/肉类/零食/饮料/调料/冷冻食品/其他 之一）"
                }
                只输出JSON，不要有任何其他文字。
            """.trimIndent()

            val requestBody = buildRequestBody(base64Image, prompt)
            val request = Request.Builder()
                .url("https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val bodyStr = response.body?.string() ?: return@withContext AiResult.Error("响应为空")

            if (!response.isSuccessful) {
                return@withContext AiResult.Error("API 错误 ${response.code}: ${bodyStr.take(200)}")
            }

            parseResponse(bodyStr)
        } catch (e: Exception) {
            AiResult.Error("识别失败：${e.message}")
        }
    }

    private fun encodeImageToBase64(uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bitmap = BitmapFactory.decodeStream(stream) ?: return@use null
                val scaled = scaleBitmap(bitmap, 768)
                val baos = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, 75, baos)
                Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
            }
        } catch (e: Exception) { null }
    }

    private fun scaleBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxSize && height <= maxSize) return bitmap
        val scale = maxSize.toFloat() / maxOf(width, height)
        return Bitmap.createScaledBitmap(bitmap, (width * scale).toInt(), (height * scale).toInt(), true)
    }

    private fun buildRequestBody(base64Image: String, prompt: String): String {
        return gson.toJson(mapOf(
            "model" to "qwen3.5-plus",
            "max_tokens" to 500,
            "messages" to listOf(
                mapOf(
                    "role" to "user",
                    "content" to listOf(
                        mapOf("type" to "text", "text" to prompt),
                        mapOf(
                            "type" to "image_url",
                            "image_url" to mapOf("url" to "data:image/jpeg;base64,$base64Image")
                        )
                    )
                )
            )
        ))
    }

    private fun parseResponse(bodyStr: String): AiResult {
        return try {
            val root = gson.fromJson(bodyStr, JsonObject::class.java)
            val content = root
                .getAsJsonArray("choices")
                .get(0).asJsonObject
                .getAsJsonObject("message")
                .get("content").asString
                .trim()
                .removePrefix("```json")
                .removeSuffix("```")
                .trim()

            val json = gson.fromJson(content, JsonObject::class.java)
            AiResult.Success(
                RecognitionResult(
                    name = json.get("name")?.asString ?: "",
                    quantity = json.get("quantity")?.asString ?: "",
                    expiryInfo = json.get("expiry_info")?.asString ?: "",
                    categoryHint = json.get("category")?.asString ?: "其他"
                )
            )
        } catch (e: Exception) {
            AiResult.Error("解析识别结果失败：${e.message}")
        }
    }
}
