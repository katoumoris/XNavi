package com.xnavi.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amap.api.maps.model.LatLng
import com.amap.api.services.route.DrivePath
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class NavState(
    val remainingDistance: String = "",
    val remainingDuration: String = "",
    val currentRoadName: String = "",
    val nextTurnDescription: String = "",
    val nextTurnDistance: String = "",
    val isNavigating: Boolean = false,
    val totalDistance: String = "",
    val estimatedArrival: String = ""
)

class NavigationViewModel : ViewModel() {

    var navState by mutableStateOf(NavState())
        private set

    var currentPosition by mutableStateOf<LatLng?>(null)
        private set

    val routePoints = mutableListOf<LatLng>()

    private var currentStepIndex by mutableIntStateOf(0)
    private var currentPointIndex by mutableIntStateOf(0)
    private var navJob: Job? = null

    fun initialize(drivePath: DrivePath) {
        routePoints.clear()
        drivePath.steps.forEach { step ->
            step.polyline?.let { polyline ->
                polyline.split(";").forEach { coord ->
                    val parts = coord.split(",")
                    if (parts.size == 2) {
                        routePoints.add(LatLng(parts[1].toDouble(), parts[0].toDouble()))
                    }
                }
            }
        }

        val totalDistance = if (drivePath.distance < 1000) "${drivePath.distance.toInt()}米"
        else "%.1f公里".format(drivePath.distance / 1000f)

        val totalSeconds = drivePath.duration
        val totalDuration = when {
            totalSeconds < 60 -> "${totalSeconds}秒"
            totalSeconds < 3600 -> "${totalSeconds / 60}分钟"
            else -> "${totalSeconds / 3600}小时${(totalSeconds % 3600) / 60}分钟"
        }

        val firstStep = drivePath.steps.firstOrNull()

        navState = NavState(
            remainingDistance = totalDistance,
            remainingDuration = totalDuration,
            currentRoadName = firstStep?.road ?: "",
            nextTurnDescription = firstStep?.instruction ?: "",
            nextTurnDistance = formatStepDistance(firstStep?.distance ?: 0f),
            isNavigating = false,
            totalDistance = totalDistance,
            estimatedArrival = "预计到达..."
        )

        currentStepIndex = 0
        currentPointIndex = 0

        if (routePoints.isNotEmpty()) {
            currentPosition = routePoints.first()
        }
    }

    fun startNavigation() {
        navState = navState.copy(isNavigating = true)
        navJob?.cancel()
        navJob = viewModelScope.launch {
            while (currentPointIndex < routePoints.size - 1) {
                delay(500)
                currentPointIndex++
                currentPosition = routePoints[currentPointIndex]
            }
            navState = navState.copy(
                isNavigating = false,
                remainingDistance = "0米",
                remainingDuration = "已到达",
                nextTurnDescription = "到达目的地"
            )
        }
    }

    fun stopNavigation() {
        navJob?.cancel()
        navState = navState.copy(isNavigating = false)
    }

    private fun formatStepDistance(meters: Float): String {
        return if (meters < 1000) "${meters.toInt()}米" else "%.1f公里".format(meters / 1000f)
    }

    override fun onCleared() {
        super.onCleared()
        navJob?.cancel()
    }
}
