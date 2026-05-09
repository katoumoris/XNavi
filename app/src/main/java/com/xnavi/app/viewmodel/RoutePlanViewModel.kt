package com.xnavi.app.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.amap.api.maps.model.LatLng
import com.amap.api.services.core.AMapException
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.route.*

enum class TravelMode { DRIVING, TRANSIT, RIDING, WALKING }

data class RoutePoint(
    val name: String,
    val latLng: LatLng
)

data class DriveRouteInfo(
    val distance: String,
    val duration: String,
    val trafficLights: Int,
    val tolls: String
)

data class TransitRouteInfo(
    val title: String,
    val distance: String,
    val duration: String,
    val walkingDistance: String,
    val steps: List<String>
)

data class SimpleRouteInfo(
    val distance: String,
    val duration: String
)

class RoutePlanViewModel(application: Application) : AndroidViewModel(application) {

    var origin by mutableStateOf<RoutePoint?>(null)
        private set
    var destination by mutableStateOf<RoutePoint?>(null)
        private set
    val waypoints = mutableStateListOf<RoutePoint>()

    var travelMode by mutableStateOf(TravelMode.DRIVING)

    var driveRouteInfo by mutableStateOf<DriveRouteInfo?>(null)
    var driveRouteResult by mutableStateOf<DriveRouteResult?>(null)

    val transitRoutes = mutableStateListOf<TransitRouteInfo>()
    var busRouteResult by mutableStateOf<BusRouteResult?>(null)

    var simpleRouteInfo by mutableStateOf<SimpleRouteInfo?>(null)
    var rideRouteResult by mutableStateOf<RideRouteResult?>(null)
    var walkRouteResult by mutableStateOf<WalkRouteResult?>(null)

    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    fun setOrigin(name: String, latLng: LatLng) {
        origin = RoutePoint(name, latLng)
    }

    fun setDestination(name: String, latLng: LatLng) {
        destination = RoutePoint(name, latLng)
    }

    fun addWaypoint(name: String, latLng: LatLng) {
        if (waypoints.size < 3) {
            waypoints.add(RoutePoint(name, latLng))
        }
    }

    fun removeWaypoint(index: Int) {
        if (index in waypoints.indices) {
            waypoints.removeAt(index)
        }
    }

    fun changeTravelMode(mode: TravelMode) {
        travelMode = mode
        searchRoute()
    }

    fun searchRoute() {
        val from = origin ?: return
        val to = destination ?: return

        isLoading = true
        errorMessage = null

        val routeSearch = RouteSearch(getApplication())
        routeSearch.setRouteSearchListener(object : RouteSearch.OnRouteSearchListener {
            override fun onDriveRouteSearched(result: DriveRouteResult?, errorCode: Int) {
                isLoading = false
                if (errorCode == AMapException.CODE_AMAP_SUCCESS && result != null) {
                    driveRouteResult = result
                    val path = result.paths.firstOrNull()
                    if (path != null) {
                        driveRouteInfo = DriveRouteInfo(
                            distance = formatDistance(path.distance),
                            duration = formatDuration(path.duration),
                            trafficLights = path.totalTrafficlights,
                            tolls = formatDistance(path.tolls)
                        )
                    }
                } else {
                    errorMessage = "驾车路线规划失败"
                }
            }

            override fun onBusRouteSearched(result: BusRouteResult?, errorCode: Int) {
                isLoading = false
                if (errorCode == AMapException.CODE_AMAP_SUCCESS && result != null) {
                    busRouteResult = result
                    transitRoutes.clear()
                    result.paths.forEach { path ->
                        val steps = path.steps.map { step ->
                            when {
                                step.walk != null -> {
                                    "步行 ${formatDistance(step.walk!!.distance)}"
                                }
                                step.busLines != null && step.busLines!!.isNotEmpty() -> {
                                    val busLine = step.busLines!!.first()
                                    "乘坐 ${busLine.busLineName}"
                                }
                                step.railway != null -> {
                                    val rail = step.railway!!
                                    "乘坐 ${rail.name}"
                                }
                                else -> ""
                            }
                        }
                        transitRoutes.add(
                            TransitRouteInfo(
                                title = "公交方案, ${path.duration / 60}分钟",
                                distance = formatDistance(path.distance),
                                duration = formatDuration(path.duration),
                                walkingDistance = formatDistance(path.walkDistance),
                                steps = steps
                            )
                        )
                    }
                } else {
                    errorMessage = "公交路线规划失败"
                }
            }

            override fun onRideRouteSearched(result: RideRouteResult?, errorCode: Int) {
                isLoading = false
                if (errorCode == AMapException.CODE_AMAP_SUCCESS && result != null) {
                    rideRouteResult = result
                    val path = result.paths.firstOrNull()
                    if (path != null) {
                        simpleRouteInfo = SimpleRouteInfo(
                            distance = formatDistance(path.distance),
                            duration = formatDuration(path.duration)
                        )
                    }
                } else {
                    errorMessage = "骑行路线规划失败"
                }
            }

            override fun onWalkRouteSearched(result: WalkRouteResult?, errorCode: Int) {
                isLoading = false
                if (errorCode == AMapException.CODE_AMAP_SUCCESS && result != null) {
                    walkRouteResult = result
                    val path = result.paths.firstOrNull()
                    if (path != null) {
                        simpleRouteInfo = SimpleRouteInfo(
                            distance = formatDistance(path.distance),
                            duration = formatDuration(path.duration)
                        )
                    }
                } else {
                    errorMessage = "步行路线规划失败"
                }
            }
        })

        val fromPoint = LatLonPoint(from.latLng.latitude, from.latLng.longitude)
        val toPoint = LatLonPoint(to.latLng.latitude, to.latLng.longitude)
        val fromQuery = RouteSearch.FromAndTo(fromPoint, toPoint)

        when (travelMode) {
            TravelMode.DRIVING -> {
                val query = RouteSearch.DriveRouteQuery(fromQuery, RouteSearch.DrivingDefault, null, null, "")
                routeSearch.calculateDriveRouteAsyn(query)
            }
            TravelMode.TRANSIT -> {
                val query = RouteSearch.BusRouteQuery(fromQuery, RouteSearch.BusDefault, null, 0)
                routeSearch.calculateBusRouteAsyn(query)
            }
            TravelMode.RIDING -> {
                val query = RouteSearch.RideRouteQuery(fromQuery, RouteSearch.RidingDefault)
                routeSearch.calculateRideRouteAsyn(query)
            }
            TravelMode.WALKING -> {
                val query = RouteSearch.WalkRouteQuery(fromQuery, RouteSearch.WalkDefault)
                routeSearch.calculateWalkRouteAsyn(query)
            }
        }
    }

    private fun formatDistance(meters: Float): String {
        return if (meters < 1000) "${meters.toInt()}米" else "%.1f公里".format(meters / 1000f)
    }

    private fun formatDuration(seconds: Long): String {
        return if (seconds < 60) "${seconds}秒"
        else if (seconds < 3600) "${seconds / 60}分钟"
        else "${seconds / 3600}小时${(seconds % 3600) / 60}分钟"
    }

}
