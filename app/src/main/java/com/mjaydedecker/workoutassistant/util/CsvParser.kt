package com.mjaydedecker.workoutassistant.util

import com.mjaydedecker.workoutassistant.data.db.entity.ExerciseEntity
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.InputStream
import java.io.InputStreamReader

object CsvParser {

    /**
     * Parses exercises.csv from the given InputStream into a list of ExerciseEntity objects.
     *
     * CSV columns: exercise, url, primary_muscle, secondary_muscles, equipment, type, force, video, instructions
     *
     * The structured data (description, videos, images, equipment, muscles) lives in the JSON blob
     * in the "type" column (index 5), which is a Schema.org ExerciseAction object.
     */
    fun parseExercises(inputStream: InputStream): List<ExerciseEntity> {
        val results = mutableListOf<ExerciseEntity>()

        val reader = InputStreamReader(inputStream, Charsets.UTF_8)
        val csvParser = CSVParser(
            reader,
            CSVFormat.DEFAULT
                .builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build()
        )

        csvParser.use { parser ->
            for (record in parser) {
                try {
                    val name = record.get("exercise").takeIf { it.isNotBlank() } ?: continue
                    val forceValue = record.get("force").takeIf { it.isNotBlank() }

                    // The "type" column contains the Schema.org JSON blob
                    val jsonBlob = record.get("type").takeIf { it.isNotBlank() }

                    var equipment: String? = null
                    var primaryMuscles: String? = null
                    var secondaryMuscles: String? = null
                    var description: String? = null
                    var videoUrls: String? = null
                    var imageRefs: String? = null

                    if (jsonBlob != null) {
                        try {
                            val json = JSONObject(jsonBlob)

                            // Equipment
                            equipment = json.optJSONObject("exerciseRelatedEquipment")
                                ?.optString("name")
                                ?.takeIf { it.isNotBlank() }

                            // Primary muscles from muscleAction array
                            val muscleActions = json.optJSONArray("muscleAction")
                            if (muscleActions != null) {
                                val muscles = mutableListOf<String>()
                                for (i in 0 until muscleActions.length()) {
                                    muscleActions.optJSONObject(i)
                                        ?.optString("name")
                                        ?.takeIf { it.isNotBlank() }
                                        ?.let { muscles.add(it) }
                                }
                                if (muscles.isNotEmpty()) {
                                    primaryMuscles = muscles.joinToString(",")
                                }
                            }

                            // Description
                            description = json.optString("description").takeIf { it.isNotBlank() }

                            // Video URLs
                            val videos = json.optJSONArray("video")
                            if (videos != null) {
                                val urls = mutableListOf<String>()
                                for (i in 0 until videos.length()) {
                                    videos.optJSONObject(i)
                                        ?.optString("contentUrl")
                                        ?.takeIf { it.isNotBlank() }
                                        ?.let { urls.add(it) }
                                }
                                if (urls.isNotEmpty()) {
                                    val jsonArray = JSONArray()
                                    urls.forEach { jsonArray.put(it) }
                                    videoUrls = jsonArray.toString()
                                }
                            }

                            // Image refs
                            val images = json.optJSONArray("image")
                            if (images != null && images.length() > 0) {
                                imageRefs = images.toString()
                            }

                        } catch (e: JSONException) {
                            // Skip JSON parse errors for individual records
                        }
                    }

                    // Parse secondary muscles from instructions column (format: "Primary1,Primary2 Secondary1,Secondary2 instructions...")
                    // The instructions field contains muscles followed by tutorial links and instructions
                    // Secondary muscles appear on the second line separated from primary by a space
                    val instructionsField = record.get("instructions").takeIf { it.isNotBlank() }
                    if (instructionsField != null && secondaryMuscles == null) {
                        // The field starts with "Primary1,Primary2 Secondary1,Secondary2 Watch Tutorial..."
                        // Extract the second word-group as secondary muscles
                        val parts = instructionsField.trimStart().split(" ")
                        if (parts.size >= 2) {
                            val secondPart = parts[1]
                            // Secondary muscles are comma-separated names without spaces
                            if (secondPart.isNotBlank() && !secondPart.contains("Watch") &&
                                !secondPart.contains("http") && !secondPart.contains("\n")
                            ) {
                                secondaryMuscles = secondPart.takeIf { it.isNotBlank() }
                            }
                        }
                    }

                    results.add(
                        ExerciseEntity(
                            name = name,
                            isCustom = false,
                            force = forceValue,
                            equipment = equipment,
                            primaryMuscles = primaryMuscles,
                            secondaryMuscles = secondaryMuscles,
                            description = description,
                            videoUrls = videoUrls,
                            imageRefs = imageRefs
                        )
                    )
                } catch (e: Exception) {
                    // Skip malformed records
                }
            }
        }

        return results
    }
}
