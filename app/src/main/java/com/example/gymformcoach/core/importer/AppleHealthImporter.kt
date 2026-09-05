package com.example.gymformcoach.core.importer

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.zip.ZipInputStream

data class AppleHealthWeightEntry(val date: LocalDate, val weightKg: Float)

/**
 * §17C.2 Apple Health: body weight only, not workouts - a materially
 * different parser (XML inside a zip, potentially hundreds of MB for a
 * multi-year export) that doesn't fit [WorkoutImporter]'s CSV-shaped
 * interface, per the spec's explicit instruction not to force it through
 * that path.
 *
 * §17C.3: EXPERIMENTAL - `HKQuantityTypeIdentifierBodyMass` is Apple's
 * documented record type, but the exact XML shape has not been verified
 * against a real export.
 *
 * Stream-parses via XmlPullParser rather than loading the file into memory -
 * a multi-year export is routinely hundreds of MB.
 */
object AppleHealthImporter {
    const val isExperimental = true

    /** [zipInput] is the raw .zip stream (Apple Health exports as export.zip containing export.xml). */
    fun parseWeightFromZip(zipInput: InputStream): List<AppleHealthWeightEntry> {
        ZipInputStream(zipInput).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name.endsWith("export.xml", ignoreCase = true)) {
                    return parseWeightFromXml(zip)
                }
                entry = zip.nextEntry
            }
        }
        return emptyList()
    }

    private fun parseWeightFromXml(input: InputStream): List<AppleHealthWeightEntry> {
        val results = mutableListOf<AppleHealthWeightEntry>()
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(input, "UTF-8")

        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z")

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "Record") {
                val type = parser.getAttributeValue(null, "type")
                if (type == "HKQuantityTypeIdentifierBodyMass") {
                    val value = parser.getAttributeValue(null, "value")?.toFloatOrNull()
                    val unit = parser.getAttributeValue(null, "unit")
                    val startDate = parser.getAttributeValue(null, "startDate")
                    if (value != null && startDate != null) {
                        val weightKg = when {
                            unit.equals("kg", true) -> value
                            unit.equals("lb", true) -> lbsToKg(value)
                            else -> value // unknown unit: best effort, still surfaced rather than dropped
                        }
                        val date = runCatching {
                            java.time.OffsetDateTime.parse(startDate, dateFormatter).toLocalDate()
                        }.getOrNull()
                        if (date != null) results += AppleHealthWeightEntry(date, weightKg)
                    }
                }
            }
            eventType = parser.next()
        }
        return results
    }
}
