package com.xnavi.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "search_history")

class SearchHistoryStore(private val context: Context) {

    companion object {
        private val HISTORY_KEY = stringPreferencesKey("search_history")
        private const val MAX_HISTORY = 20
    }

    val history: Flow<List<SearchHistoryItem>> = context.dataStore.data.map { prefs ->
        val json = prefs[HISTORY_KEY] ?: "[]"
        parseHistory(json)
    }

    suspend fun addHistory(item: SearchHistoryItem) {
        context.dataStore.edit { prefs ->
            val current = parseHistory(prefs[HISTORY_KEY] ?: "[]").toMutableList()
            current.removeAll { it.name == item.name && it.address == item.address }
            current.add(0, item)
            if (current.size > MAX_HISTORY) {
                current.removeAt(current.lastIndex)
            }
            prefs[HISTORY_KEY] = serializeHistory(current)
        }
    }

    suspend fun clearHistory() {
        context.dataStore.edit { prefs ->
            prefs[HISTORY_KEY] = "[]"
        }
    }

    private fun parseHistory(json: String): List<SearchHistoryItem> {
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                SearchHistoryItem(
                    name = obj.getString("name"),
                    address = obj.getString("address"),
                    lat = obj.getDouble("lat"),
                    lng = obj.getDouble("lng")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun serializeHistory(items: List<SearchHistoryItem>): String {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(JSONObject().apply {
                put("name", item.name)
                put("address", item.address)
                put("lat", item.lat)
                put("lng", item.lng)
            })
        }
        return arr.toString()
    }
}

data class SearchHistoryItem(
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double
)
