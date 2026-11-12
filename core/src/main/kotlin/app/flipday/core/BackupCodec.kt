package app.flipday.core

import java.time.LocalDate
import java.time.LocalTime

class BackupException(message: String, val newerVersion: Boolean = false) : Exception(message)

data class BackupContents(val events: List<Event>, val skipped: Int)

/**
 * Export file format: one JSON document, version 1. Only events are in it. Settings belong to the device and widgets cannot be
 * restored from a file, so neither is exported. Ids are not exported: events get new ids on import.
 */
object BackupCodec {
    const val VERSION = 1
    private const val APP = "flipday"

    fun encode(events: List<Event>, exportedOn: LocalDate): String {
        val sb = StringBuilder()
        sb.append("{\"app\":\"").append(APP).append("\",\"version\":").append(VERSION).append(",\"exportedOn\":\"").append(exportedOn).append("\",\"events\":[")
        events.forEachIndexed { i, e ->
            if (i > 0) sb.append(',')
            sb.append("{\"title\":").append(quote(e.title)).append(",\"type\":\"").append(e.type.name).append("\",\"date\":\"").append(e.date).append('"')
            e.time?.let { sb.append(",\"time\":\"").append(it).append('"') }
            e.startDate?.let { sb.append(",\"startDate\":\"").append(it).append('"') }
            e.endDate?.let { sb.append(",\"endDate\":\"").append(it).append('"') }
            e.archivedOn?.let { sb.append(",\"archivedOn\":\"").append(it).append('"') }
            e.milestoneEveryDays?.let { sb.append(",\"milestoneEveryDays\":").append(it) }
            sb.append(",\"repeatYearly\":").append(e.repeatYearly)
            sb.append(",\"reminders\":[")
            e.reminders.forEachIndexed { j, r ->
                if (j > 0) sb.append(',')
                sb.append("{\"daysBefore\":").append(r.daysBefore).append(",\"at\":\"").append(r.at).append("\"}")
            }
            sb.append("]}")
        }
        return sb.append("]}").toString()
    }

    /** Throws [BackupException] when the text is not a Flipday export. Single events that cannot be read are skipped and counted. */
    fun decode(text: String): BackupContents {
        val root = try { Json(text).parse() } catch (e: BackupException) { throw e } catch (e: RuntimeException) { throw BackupException("Not valid JSON") }
        val obj = root as? Map<*, *> ?: throw BackupException("Not a Flipday backup")
        if (obj["app"] != APP) throw BackupException("Not a Flipday backup")
        val version = (obj["version"] as? Long) ?: throw BackupException("Missing version")
        if (version > VERSION) throw BackupException("This backup was made by a newer version of the app", newerVersion = true)
        val list = obj["events"] as? List<*> ?: throw BackupException("Missing events")
        val events = mutableListOf<Event>()
        var skipped = 0
        for (item in list) {
            val e = runCatching { toEvent(item as Map<*, *>) }.getOrNull()
            if (e == null) skipped++ else events += e
        }
        return BackupContents(events, skipped)
    }

    /** True when [a] and [b] are the same event for import purposes (ids are ignored). */
    fun sameEvent(a: Event, b: Event) = a.title == b.title && a.type == b.type && a.date == b.date && a.endDate == b.endDate

    private fun toEvent(m: Map<*, *>): Event {
        fun str(k: String) = m[k] as? String
        fun date(k: String) = str(k)?.let(LocalDate::parse)
        val reminders = (m["reminders"] as? List<*> ?: emptyList<Any>()).map { r ->
            r as Map<*, *>
            ReminderRule((r["daysBefore"] as Long).toInt(), LocalTime.parse(r["at"] as String))
        }
        return Event(
            title = str("title")!!, type = EventType.valueOf(str("type")!!), date = date("date")!!,
            time = str("time")?.let(LocalTime::parse), startDate = date("startDate"), reminders = reminders,
            repeatYearly = m["repeatYearly"] as? Boolean ?: false, archivedOn = date("archivedOn"),
            milestoneEveryDays = (m["milestoneEveryDays"] as? Long)?.toInt(), endDate = date("endDate"),
        )
    }

    private fun quote(s: String): String {
        val sb = StringBuilder("\"")
        for (c in s) when {
            c == '"' -> sb.append("\\\""); c == '\\' -> sb.append("\\\\")
            c == '\n' -> sb.append("\\n"); c == '\r' -> sb.append("\\r"); c == '\t' -> sb.append("\\t")
            c < ' ' -> sb.append(String.format("\\u%04x", c.code))
            else -> sb.append(c)
        }
        return sb.append('"').toString()
    }

    /** Small JSON reader: objects, arrays, strings, integers, booleans, null. Enough for the export format. */
    private class Json(private val s: String) {
        private var i = 0
        fun parse(): Any? { val v = value(); ws(); if (i != s.length) fail(); return v }
        private fun fail(): Nothing = throw BackupException("Not valid JSON")
        private fun ws() { while (i < s.length && s[i].isWhitespace()) i++ }
        private fun value(): Any? {
            ws(); if (i >= s.length) fail()
            return when (s[i]) {
                '{' -> obj(); '[' -> arr(); '"' -> str()
                't' -> lit("true", true); 'f' -> lit("false", false); 'n' -> lit("null", null)
                else -> num()
            }
        }
        private fun lit(w: String, v: Any?): Any? { if (!s.startsWith(w, i)) fail(); i += w.length; return v }
        private fun num(): Long { val st = i; if (i < s.length && s[i] == '-') i++; while (i < s.length && s[i].isDigit()) i++; if (st == i) fail(); return s.substring(st, i).toLong() }
        private fun str(): String {
            i++; val sb = StringBuilder()
            while (true) {
                if (i >= s.length) fail()
                val c = s[i++]
                when (c) {
                    '"' -> return sb.toString()
                    '\\' -> { if (i >= s.length) fail(); when (val e = s[i++]) {
                        'n' -> sb.append('\n'); 'r' -> sb.append('\r'); 't' -> sb.append('\t'); 'b' -> sb.append('\b'); 'f' -> sb.append('\u000c')
                        '"', '\\', '/' -> sb.append(e)
                        'u' -> { if (i + 4 > s.length) fail(); sb.append(s.substring(i, i + 4).toInt(16).toChar()); i += 4 }
                        else -> fail() } }
                    else -> sb.append(c)
                }
            }
        }
        private fun arr(): List<Any?> {
            i++; val out = mutableListOf<Any?>(); ws()
            if (i < s.length && s[i] == ']') { i++; return out }
            while (true) { out += value(); ws(); if (i >= s.length) fail(); if (s[i] == ',') { i++; continue }; if (s[i] == ']') { i++; return out }; fail() }
        }
        private fun obj(): Map<String, Any?> {
            i++; val out = linkedMapOf<String, Any?>(); ws()
            if (i < s.length && s[i] == '}') { i++; return out }
            while (true) {
                ws(); if (i >= s.length || s[i] != '"') fail()
                val k = str(); ws(); if (i >= s.length || s[i] != ':') fail(); i++
                out[k] = value(); ws(); if (i >= s.length) fail()
                if (s[i] == ',') { i++; continue }; if (s[i] == '}') { i++; return out }; fail()
            }
        }
    }
}
