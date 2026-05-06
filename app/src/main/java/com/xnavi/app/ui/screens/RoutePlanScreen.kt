package com.xnavi.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.PolylineOptions
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.route.DrivePath
import com.xnavi.app.ui.components.AmapView
import com.xnavi.app.viewmodel.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutePlanScreen(
    navController: NavController,
    mainViewModel: MainViewModel = viewModel(),
    routeViewModel: RoutePlanViewModel = viewModel()
) {
    val context = LocalContext.current
    var aMap by remember { mutableStateOf<AMap?>(null) }
    var showRouteDetail by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        mainViewModel.currentLocation?.let { loc ->
            if (routeViewModel.origin == null) {
                routeViewModel.setOrigin("我的位置", loc)
            }
        }
        mainViewModel.selectedPoi?.let { poi ->
            if (routeViewModel.destination == null) {
                routeViewModel.setDestination(poi.name, poi.latLng)
            }
        }
    }

    LaunchedEffect(
        routeViewModel.driveRouteResult,
        routeViewModel.busRouteResult,
        routeViewModel.rideRouteResult,
        routeViewModel.walkRouteResult
    ) {
        aMap?.let { map ->
            map.clear()
            routeViewModel.origin?.let {
                map.addMarker(
                    MarkerOptions().position(it.latLng).title(it.name)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                )
            }
            routeViewModel.destination?.let {
                map.addMarker(
                    MarkerOptions().position(it.latLng).title(it.name)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                )
            }

            when (routeViewModel.travelMode) {
                TravelMode.DRIVING -> {
                    routeViewModel.driveRouteResult?.paths?.firstOrNull()?.let { path ->
                        val points = convertDrivePathToLatLngList(path)
                        map.addPolyline(
                            PolylineOptions().addAll(points)
                                .color(0xFF1678FF.toInt()).width(15f)
                        )
                        showRouteDetail = true
                        zoomToFit(map, points)
                    }
                }
                TravelMode.TRANSIT -> {
                    routeViewModel.busRouteResult?.paths?.firstOrNull()?.let { path ->
                        val points = mutableListOf<LatLng>()
                        path.steps.forEach { step ->
                            step.polyline?.let { pl ->
                                val coords = pl.split(";")
                                coords.forEach { coord ->
                                    val parts = coord.split(",")
                                    if (parts.size == 2) {
                                        points.add(LatLng(parts[1].toDouble(), parts[0].toDouble()))
                                    }
                                }
                            }
                        }
                        if (points.isNotEmpty()) {
                            map.addPolyline(
                                PolylineOptions().addAll(points)
                                    .color(0xFF00B061.toInt()).width(15f)
                            )
                            zoomToFit(map, points)
                        }
                        showRouteDetail = true
                    }
                }
                TravelMode.RIDING -> {
                    routeViewModel.rideRouteResult?.paths?.firstOrNull()?.let { path ->
                        val points = mutableListOf<LatLng>()
                        path.steps.forEach { step ->
                            step.polyline?.let { pl ->
                                pl.split(";").forEach { coord ->
                                    val parts = coord.split(",")
                                    if (parts.size == 2) {
                                        points.add(LatLng(parts[1].toDouble(), parts[0].toDouble()))
                                    }
                                }
                            }
                        }
                        if (points.isNotEmpty()) {
                            map.addPolyline(
                                PolylineOptions().addAll(points)
                                    .color(0xFFFF6B35.toInt()).width(15f)
                            )
                            zoomToFit(map, points)
                        }
                        showRouteDetail = true
                    }
                }
                TravelMode.WALKING -> {
                    routeViewModel.walkRouteResult?.paths?.firstOrNull()?.let { path ->
                        val points = mutableListOf<LatLng>()
                        path.steps.forEach { step ->
                            step.polyline?.let { pl ->
                                pl.split(";").forEach { coord ->
                                    val parts = coord.split(",")
                                    if (parts.size == 2) {
                                        points.add(LatLng(parts[1].toDouble(), parts[0].toDouble()))
                                    }
                                }
                            }
                        }
                        if (points.isNotEmpty()) {
                            map.addPolyline(
                                PolylineOptions().addAll(points)
                                    .color(0xFFFF6B35.toInt()).width(15f)
                            )
                            zoomToFit(map, points)
                        }
                        showRouteDetail = true
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("路线规划", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            RouteInputSection(
                origin = routeViewModel.origin?.name ?: "",
                destination = routeViewModel.destination?.name ?: "",
                waypoints = routeViewModel.waypoints,
                onAddWaypoint = {
                    android.widget.Toast.makeText(context, "途经点搜索功能开发中", android.widget.Toast.LENGTH_SHORT).show()
                },
                onRemoveWaypoint = { routeViewModel.removeWaypoint(it) }
            )

            TravelModeTabs(
                selectedMode = routeViewModel.travelMode,
                onModeSelected = { routeViewModel.setTravelMode(it) }
            )

            Box(modifier = Modifier.weight(1f)) {
                AmapView(
                    modifier = Modifier.fillMaxSize(),
                    onMapReady = { map ->
                        aMap = map
                        map.uiSettings.isZoomControlsEnabled = false
                    }
                )

                if (routeViewModel.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            if (showRouteDetail) {
                RouteDetailCard(routeViewModel = routeViewModel) {
                    navController.navigate("navigation")
                }
            }
        }
    }
}

@Composable
private fun RouteInputSection(
    origin: String,
    destination: String,
    waypoints: List<RoutePoint>,
    onAddWaypoint: () -> Unit,
    onRemoveWaypoint: (Int) -> Unit
) {
    Surface(tonalElevation = 2.dp) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = origin,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Text("起", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = destination,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Text("终", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { }) {
                    Icon(Icons.Default.SwapVert, "交换")
                }
            }

            waypoints.forEachIndexed { index, wp ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = wp.name,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.weight(1f),
                        leadingIcon = { Text("经", color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                    IconButton(onClick = { onRemoveWaypoint(index) }) {
                        Icon(Icons.Default.Close, "删除", modifier = Modifier.size(18.dp))
                    }
                }
            }

            if (waypoints.size < 3) {
                TextButton(onClick = onAddWaypoint) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("添加途经点")
                }
            }
        }
    }
}

@Composable
private fun TravelModeTabs(
    selectedMode: TravelMode,
    onModeSelected: (TravelMode) -> Unit
) {
    val modes = listOf(
        TravelMode.DRIVING to "驾车",
        TravelMode.TRANSIT to "公交",
        TravelMode.RIDING to "骑行",
        TravelMode.WALKING to "步行"
    )

    TabRow(selectedTabIndex = modes.indexOfFirst { it.first == selectedMode }) {
        modes.forEachIndexed { _, (mode, label) ->
            Tab(
                selected = selectedMode == mode,
                onClick = { onModeSelected(mode) },
                text = { Text(label) }
            )
        }
    }
}

@Composable
private fun RouteDetailCard(
    routeViewModel: RoutePlanViewModel,
    onNavigate: () -> Unit
) {
    Surface(
        tonalElevation = 4.dp,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            when (routeViewModel.travelMode) {
                TravelMode.DRIVING -> {
                    routeViewModel.driveRouteInfo?.let { info ->
                        Text("驾车路线", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            InfoChip("距离", info.distance)
                            InfoChip("时间", info.duration)
                            InfoChip("红绿灯", "${info.trafficLights}个")
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = onNavigate, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Navigation, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("开始导航")
                        }
                    }
                }
                TravelMode.TRANSIT -> {
                    Text("公交方案", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        itemsIndexed(routeViewModel.transitRoutes) { index, route ->
                            Surface(
                                tonalElevation = 1.dp,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("方案 ${index + 1}: ${route.title}", style = MaterialTheme.typography.bodyMedium)
                                    Spacer(Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        Text("${route.distance}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${route.duration}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("步行${route.walkingDistance}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
                TravelMode.RIDING, TravelMode.WALKING -> {
                    routeViewModel.simpleRouteInfo?.let { info ->
                        val modeName = if (routeViewModel.travelMode == TravelMode.RIDING) "骑行" else "步行"
                        Text("${modeName}路线", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            InfoChip("距离", info.distance)
                            InfoChip("时间", info.duration)
                        }
                    }
                }
            }

            routeViewModel.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun InfoChip(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

private fun convertDrivePathToLatLngList(path: DrivePath): List<LatLng> {
    val points = mutableListOf<LatLng>()
    path.steps.forEach { step ->
        step.polyline?.let { pl ->
            pl.split(";").forEach { coord ->
                val parts = coord.split(",")
                if (parts.size == 2) {
                    points.add(LatLng(parts[1].toDouble(), parts[0].toDouble()))
                }
            }
        }
    }
    return points
}

private fun zoomToFit(map: AMap, points: List<LatLng>) {
    if (points.isEmpty()) return
    val builder = com.amap.api.maps.model.LatLngBounds.Builder()
    points.forEach { builder.include(it) }
    map.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100))
}
