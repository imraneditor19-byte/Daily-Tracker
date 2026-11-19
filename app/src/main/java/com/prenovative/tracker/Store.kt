package com.prenovative.tracker

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Task(val id: Int, val title: String, val h: Int, val m: Int, var doneOn: String)

object Store {
    fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    private fun prefs(c: Context) = c.getSharedPreferences("tracker", 0)

    fun load(c: Context): MutableList<Task> {
        val a = JSONArray(prefs(c).getString("tasks", "[]"))
        return MutableList(a.length()) {
            val o = a.getJSONObject(it)
            Task(o.getInt("id"), o.getString("t"), o.getInt("h"), o.getInt("m"), o.optString("d"))
        }
    }

    fun save(c: Context, l: List<Task>) {
        val a = JSONArray()
        l.forEach { a.put(JSONObject().put("id", it.id).put("t", it.title).put("h", it.h).put("m", it.m).put("d", it.doneOn)) }
        prefs(c).edit().putString("tasks", a.toString()).apply()
    }
}
