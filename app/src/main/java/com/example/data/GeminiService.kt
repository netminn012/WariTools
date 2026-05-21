package com.example.data

import android.graphics.Bitmap
import android.util.Base64
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

// --- Gemini API Request Models ---

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    val mimeType: String,
    val data: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val responseMimeType: String? = null,
    val responseSchema: ResponseSchema? = null,
    val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class ResponseSchema(
    val type: String,
    val description: String? = null,
    val properties: Map<String, ResponseSchema>? = null,
    val items: ResponseSchema? = null,
    val required: List<String>? = null
)

// --- Gemini API Response Models ---

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content?
)

// --- Scan Output Struct ---

@JsonClass(generateAdapter = true)
data class ScanResultItem(
    val name: String,
    val amount: Double
)

// --- Retrofit Setup ---

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

// --- Bitmap utilities ---

fun Bitmap.toBase64(): String {
    val outputStream = ByteArrayOutputStream()
    // Compress helper to balance quality and speed
    compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
    return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
}

// --- API Implementation ---

class GeminiRepository {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    suspend fun scanReceipt(bitmap: Bitmap, apiKey: String): List<ScanResultItem> {
        val prompt = "Analyze this receipt or total bill and extract all listed purchased items, dishes, or charges with their prices. Format the output STRICTLY as a JSON array of items each having 'name' representing the item dish or charge name (in Japanese if the receipt is Japanese) and 'amount' representing the price or cost of that item as an integer."

        val itemSchema = ResponseSchema(
            type = "OBJECT",
            properties = mapOf(
                "name" to ResponseSchema(
                    type = "STRING",
                    description = "The Japanese name of the item or dish on the receipt"
                ),
                "amount" to ResponseSchema(
                    type = "INTEGER",
                    description = "The individual item price/cost in Japanese Yen"
                )
            ),
            required = listOf("name", "amount")
        )

        val rootSchema = ResponseSchema(
            type = "ARRAY",
            items = itemSchema,
            description = "List of extracted receipt items"
        )

        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = prompt),
                        Part(inlineData = InlineData(mimeType = "image/jpeg", data = bitmap.toBase64()))
                    )
                )
            ),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                responseSchema = rootSchema,
                temperature = 0.2f
            )
        )

        return try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: return emptyList()

            val listType = Types.newParameterizedType(List::class.java, ScanResultItem::class.java)
            val adapter = moshi.adapter<List<ScanResultItem>>(listType)
            adapter.fromJson(jsonText) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
