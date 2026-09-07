package com.tdminsight.app.data

import android.content.Context
import com.tdminsight.app.engine.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** A single saved calculation, enough to redisplay the results screen. */
data class HistoryEntry(
    val id: String,
    val timestamp: Long,
    val workflow: Workflow,
    val result: TdmResult,
)

private const val PREFS_NAME = "tdm_insight_prefs"
private const val KEY_HISTORY = "history_json"

/**
 * Simple local-only history store backed by SharedPreferences + JSON.
 * No backend/cloud dependency, consistent with the project scope.
 */
object HistoryStore {

    fun load(context: Context): List<HistoryEntry> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { idx -> entryFromJson(arr.getJSONObject(idx)) }
                .sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(context: Context, entry: HistoryEntry) {
        val current = load(context).toMutableList()
        current.add(0, entry)
        persist(context, current)
    }

    fun clear(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    fun findById(context: Context, id: String): HistoryEntry? =
        load(context).find { it.id == id }

    fun newId(): String = UUID.randomUUID().toString()

    private fun persist(context: Context, entries: List<HistoryEntry>) {
        val arr = JSONArray()
        entries.forEach { arr.put(entryToJson(it)) }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_HISTORY, arr.toString()).apply()
    }

    private fun paramMapToJson(map: Map<String, ParamValue>): JSONObject {
        val obj = JSONObject()
        map.forEach { (k, v) ->
            obj.put(k, JSONObject().apply {
                put("value", v.value)
                put("unit", v.unit)
                put("label", v.label)
            })
        }
        return obj
    }

    private fun paramMapFromJson(obj: JSONObject): LinkedHashMap<String, ParamValue> {
        val map = LinkedHashMap<String, ParamValue>()
        obj.keys().forEach { k ->
            val p = obj.getJSONObject(k)
            map[k] = ParamValue(p.getDouble("value"), p.getString("unit"), p.getString("label"))
        }
        return map
    }

    private fun entryToJson(e: HistoryEntry): JSONObject {
        val i = e.result.inputs
        return JSONObject().apply {
            put("id", e.id)
            put("timestamp", e.timestamp)
            put("workflow", e.workflow.id)
            put("inputs", JSONObject().apply {
                put("age", i.age); put("weight", i.weight); put("sex", i.sex); put("scr", i.scr)
                put("dose", i.dose); put("tau", i.tau); put("tInf", i.tInf)
                i.cminMeasured?.let { put("cminMeasured", it) }
                i.cpeakMeasured?.let { put("cpeakMeasured", it) }
                i.tPeak?.let { put("tPeak", it) }
            })
            put("intermediate", paramMapToJson(e.result.intermediate))
            put("pk", paramMapToJson(e.result.pk))
            put("recommendation", JSONObject().apply {
                put("targetDose", e.result.recommendation.targetDose)
                put("target", e.result.recommendation.target)
                put("note", e.result.recommendation.note)
            })
            put("explanation", JSONArray().apply {
                e.result.explanation.forEach { step ->
                    put(JSONObject().apply {
                        put("step", step.step); put("label", step.label); put("detail", step.detail)
                    })
                }
            })
        }
    }

    private fun entryFromJson(obj: JSONObject): HistoryEntry? {
        return try {
            val workflow = Workflow.fromId(obj.getString("workflow"))
            val inJson = obj.getJSONObject("inputs")
            val inputs = TdmInputs(
                age = inJson.getDouble("age"),
                weight = inJson.getDouble("weight"),
                sex = inJson.getString("sex"),
                scr = inJson.getDouble("scr"),
                dose = inJson.getDouble("dose"),
                tau = inJson.getDouble("tau"),
                tInf = inJson.getDouble("tInf"),
                cminMeasured = if (inJson.has("cminMeasured")) inJson.getDouble("cminMeasured") else null,
                cpeakMeasured = if (inJson.has("cpeakMeasured")) inJson.getDouble("cpeakMeasured") else null,
                tPeak = if (inJson.has("tPeak")) inJson.getDouble("tPeak") else null,
            )
            val recJson = obj.getJSONObject("recommendation")
            val recommendation = Recommendation(
                recJson.getInt("targetDose"), recJson.getString("target"), recJson.getString("note")
            )
            val explanationArr = obj.getJSONArray("explanation")
            val explanation = (0 until explanationArr.length()).map { idx ->
                val s = explanationArr.getJSONObject(idx)
                ExplanationStep(s.getString("step"), s.getString("label"), s.getString("detail"))
            }
            val result = TdmResult(
                workflow = workflow,
                inputs = inputs,
                intermediate = paramMapFromJson(obj.getJSONObject("intermediate")),
                pk = paramMapFromJson(obj.getJSONObject("pk")),
                recommendation = recommendation,
                explanation = explanation,
            )
            HistoryEntry(obj.getString("id"), obj.getLong("timestamp"), workflow, result)
        } catch (e: Exception) {
            null
        }
    }
}
