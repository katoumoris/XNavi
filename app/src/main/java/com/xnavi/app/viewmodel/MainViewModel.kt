package com.xnavi.app.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener
import com.amap.api.maps.model.LatLng

class MainViewModel(application: Application) : AndroidViewModel(application) {
    var currentLocation by mutableStateOf<LatLng?>(null)
        private set
    var isLocated by mutableStateOf(false)
        private set
    var selectedPoi by mutableStateOf<PoiInfo?>(null)
        private set
    var showPoiDetail by mutableStateOf(false)
        private set

    private var locationClient: AMapLocationClient? = null

    init {
        initLocation(application)
    }

    private fun initLocation(context: Application) {
        locationClient = AMapLocationClient(context)
        val option = AMapLocationClientOption().apply {
            locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
            isOnceLocation = true
            isOnceLocationLatest = true
            interval = 2000
        }
        locationClient?.setLocationOption(option)
        locationClient?.setLocationListener(object : AMapLocationListener {
            override fun onLocationChanged(location: AMapLocation?) {
                location?.let {
                    if (it.errorCode == 0) {
                        currentLocation = LatLng(it.latitude, it.longitude)
                        isLocated = true
                    }
                }
            }
        })
    }

    fun startLocation() {
        locationClient?.startLocation()
    }

    fun selectPoi(poi: PoiInfo) {
        selectedPoi = poi
        showPoiDetail = true
    }

    fun dismissPoiDetail() {
        showPoiDetail = false
    }

    fun moveToCurrentLocation() {
        startLocation()
    }

    override fun onCleared() {
        super.onCleared()
        locationClient?.onDestroy()
    }
}

data class PoiInfo(
    val name: String,
    val address: String,
    val latLng: LatLng,
    val distance: String = ""
)
