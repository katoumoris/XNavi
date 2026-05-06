package com.xnavi.app.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.amap.api.maps.model.LatLng
import com.amap.api.services.core.AMapException
import com.amap.api.services.help.Inputtips
import com.amap.api.services.help.InputtipsQuery
import com.amap.api.services.help.Tip
import com.xnavi.app.data.SearchHistoryItem
import com.xnavi.app.data.SearchHistoryStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    var query by mutableStateOf("")
        private set

    val suggestions = mutableStateListOf<SuggestionItem>()
    val searchResults = mutableStateListOf<PoiInfo>()
    val historyItems = mutableStateListOf<SearchHistoryItem>()

    var isSearching by mutableStateOf(false)
        private set
    var isSearchingSuggestions by mutableStateOf(false)
        private set
    var selectedPoi by mutableStateOf<PoiInfo?>(null)
        private set

    private val historyStore = SearchHistoryStore(application)
    private var suggestionJob: Job? = null

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            historyStore.history.collect { items ->
                historyItems.clear()
                historyItems.addAll(items)
            }
        }
    }

    fun onQueryChanged(newQuery: String) {
        query = newQuery
        suggestions.clear()
        if (newQuery.isBlank()) {
            suggestionJob?.cancel()
            return
        }
        suggestionJob?.cancel()
        suggestionJob = viewModelScope.launch {
            delay(300)
            fetchSuggestions(newQuery)
        }
    }

    private suspend fun fetchSuggestions(keyword: String) {
        isSearchingSuggestions = true
        try {
            val query = InputtipsQuery(keyword, null)
            query.cityLimit = false
            val inputtips = Inputtips(getApplication(), query)
            inputtips.setInputtipsListener(object : Inputtips.InputtipsListener {
                override fun onGetInputtips(tips: MutableList<Tip>?, errorCode: Int) {
                    isSearchingSuggestions = false
                    if (errorCode == AMapException.CODE_AMAP_SUCCESS && tips != null) {
                        suggestions.clear()
                        tips.forEach { tip ->
                            if (tip.point != null) {
                                suggestions.add(
                                    SuggestionItem(
                                        name = tip.name,
                                        address = tip.district ?: "",
                                        latLng = LatLng(tip.point.latitude, tip.point.longitude)
                                    )
                                )
                            }
                        }
                    }
                }
            })
            inputtips.requestInputtipsAsyn()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addToHistory(item: SearchHistoryItem) {
        viewModelScope.launch {
            historyStore.addHistory(item)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            historyStore.clearHistory()
        }
    }

    fun selectSuggestion(suggestion: SuggestionItem) {
        selectedPoi = PoiInfo(
            name = suggestion.name,
            address = suggestion.address,
            latLng = suggestion.latLng,
            distance = ""
        )
        addToHistory(SearchHistoryItem(
            name = suggestion.name,
            address = suggestion.address,
            lat = suggestion.latLng.latitude,
            lng = suggestion.latLng.longitude
        ))
    }

    fun selectHistory(item: SearchHistoryItem) {
        selectedPoi = PoiInfo(
            name = item.name,
            address = item.address,
            latLng = LatLng(item.lat, item.lng),
            distance = ""
        )
    }

    fun clearSelectedPoi() {
        selectedPoi = null
    }
}

data class SuggestionItem(
    val name: String,
    val address: String,
    val latLng: LatLng
)
