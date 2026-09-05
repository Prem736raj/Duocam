package com.example

import org.json.JSONArray
import org.json.JSONObject
import java.io.Serializable

data class WatermarkConfig(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val type: String, // "IMAGE" or "TEXT"
    val imagePath: String? = null, // Path to local copied file
    val text: String = "",
    val font: String = "Roboto", // Roboto, Monospace, Serif, Sans-Serif
    val fontSize: Int = 24, // font point size
    val color: String = "#FFFFFF", // Hex code string
    val opacity: Float = 0.8f, // 0f to 1f
    val xPercent: Float = 0.1f, // Relative position from left (0.0 to 1.0)
    val yPercent: Float = 0.1f, // Relative position from top (0.0 to 1.0)
    val scale: Float = 1.0f, // Multiplier for size/scale (pinch-zoom)
    val showByDefault: Boolean = false, // Automatically add to all recordings
    val isAutoTime: Boolean = false // Adds real-time running date/time overlay
) : Serializable {

    fun toJsonObject(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("name", name)
        json.put("type", type)
        json.put("imagePath", imagePath ?: JSONObject.NULL)
        json.put("text", text)
        json.put("font", font)
        json.put("fontSize", fontSize)
        json.put("color", color)
        json.put("opacity", opacity.toDouble())
        json.put("xPercent", xPercent.toDouble())
        json.put("yPercent", yPercent.toDouble())
        json.put("scale", scale.toDouble())
        json.put("showByDefault", showByDefault)
        json.put("isAutoTime", isAutoTime)
        return json
    }

    companion object {
        fun fromJsonObject(json: JSONObject): WatermarkConfig {
            return WatermarkConfig(
                id = json.optString("id", java.util.UUID.randomUUID().toString()),
                name = json.optString("name", "Brand Mark"),
                type = json.optString("type", "TEXT"),
                imagePath = if (json.isNull("imagePath")) null else json.optString("imagePath"),
                text = json.optString("text", "DuoCam Studio"),
                font = json.optString("font", "Roboto"),
                fontSize = json.optInt("fontSize", 24),
                color = json.optString("color", "#FFFFFF"),
                opacity = json.optDouble("opacity", 0.8).toFloat(),
                xPercent = json.optDouble("xPercent", 0.1).toFloat(),
                yPercent = json.optDouble("yPercent", 0.1).toFloat(),
                scale = json.optDouble("scale", 1.0).toFloat(),
                showByDefault = json.optBoolean("showByDefault", false),
                isAutoTime = json.optBoolean("isAutoTime", false)
            )
        }

        fun saveList(prefs: android.content.SharedPreferences, list: List<WatermarkConfig>) {
            val array = JSONArray()
            for (config in list) {
                array.put(config.toJsonObject())
            }
            prefs.edit().putString("saved_watermarks_json", array.toString()).apply()
        }

        fun loadList(prefs: android.content.SharedPreferences): List<WatermarkConfig> {
            val jsonStr = prefs.getString("saved_watermarks_json", null) ?: return emptyList()
            try {
                val array = JSONArray(jsonStr)
                val list = mutableListOf<WatermarkConfig>()
                for (i in 0 until array.length()) {
                    list.add(fromJsonObject(array.getJSONObject(i)))
                }
                return list
            } catch (e: Exception) {
                return emptyList()
            }
        }

        fun copyUriToPrivateWatermarks(context: android.content.Context, uri: android.net.Uri): String? {
            return try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return null
                val watermarksDir = java.io.File(context.filesDir, "watermarks")
                if (!watermarksDir.exists()) {
                    watermarksDir.mkdirs()
                }
                val filename = "wm_${System.currentTimeMillis()}.png"
                val destFile = java.io.File(watermarksDir, filename)
                val outputStream = java.io.FileOutputStream(destFile)
                inputStream.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }
                destFile.absolutePath
            } catch (e: Exception) {
                android.util.Log.e("Watermark", "Failed to copy custom watermark image", e)
                null
            }
        }
    }
}
