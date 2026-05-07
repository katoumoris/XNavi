package com.xnavi.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.PolylineOptions
import com.xnavi.app.ui.components.AmapView
import com.xnavi.app.viewmodel.MainViewModel
import com.xnavi.app.viewmodel.NavigationViewModel
import com.xnavi.app.viewmodel.RoutePlanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationScreen(
    navController: NavController,
    navViewModel: NavigationViewModel = viewModel(),
    routeViewModel: RoutePlanViewModel = viewModel(),
    mainViewModel: MainViewModel = viewModel()
) {
    var aMap by remember { mutableStateOf<AMap?>(null) }
    var showEndDialog by remember { mutableStateOf(false) }

    LaunchedEffect(routeViewModel.driveRouteResult) {
        routeViewModel.driveRouteResult?.paths?.firstOrNull()?.let { path ->
            val startLatLng = routeViewModel.origin?.latLng
            val endLatLng = routeViewModel.destination?.latLng
            if (startLatLng != null && endLatLng != null) {
                navViewModel.initialize(path, startLatLng, endLatLng)
                aMap?.let { map ->
                    val points = navViewModel.routePoints
                    if (points.isNotEmpty()) {
                        map.addPolyline(
                            PolylineOptions().addAll(points)
                                .color(0xFF1678FF.toInt()).width(18f)
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(navViewModel.currentPosition) {
        navViewModel.currentPosition?.let { pos ->
            aMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 18f))
        }
    }

    if (showEndDialog) {
        AlertDialog(
            onDismissRequest = { showEndDialog = false },
            title = { Text("导航结束") },
            text = { Text("已到达目的地") },
            confirmButton = {
                Button(onClick = {
                    showEndDialog = false
                    navController.popBackStack()
                }) {
                    Text("确定")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AmapView(
            modifier = Modifier.fillMaxSize(),
            onMapReady = { map ->
                aMap = map
                map.mapType = AMap.MAP_TYPE_NORMAL
                map.uiSettings.isZoomControlsEnabled = false
                map.uiSettings.isRotateGesturesEnabled = true
            }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                tonalElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    if (navViewModel.navState.currentRoadName.isNotEmpty()) {
                        Text(
                            text = navViewModel.navState.currentRoadName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                    }

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = navViewModel.navState.remainingDistance,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "剩余距离",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = navViewModel.navState.remainingDuration,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "预计时间",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        if (navViewModel.navState.nextTurnDescription.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                color = Color(0xFF1A1A2E),
                tonalElevation = 8.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Navigation,
                            contentDescription = null,
                            tint = Color(0xFF4FC3F7),
                            modifier = Modifier.size(28.dp)
                        )
                        if (navViewModel.navState.nextTurnDistance.isNotEmpty()) {
                            Text(
                                text = navViewModel.navState.nextTurnDistance,
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = navViewModel.navState.nextTurnDescription,
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (!navViewModel.navState.isNavigating) {
                            Button(
                                onClick = { navViewModel.startNavigation() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1678FF)
                                )
                            ) {
                                Icon(Icons.Default.Navigation, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("开始模拟导航")
                            }
                        } else {
                            Button(
                                onClick = { navViewModel.stopNavigation() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(Icons.Default.Cancel, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("停止导航")
                            }
                        }

                        OutlinedButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("退出")
                        }
                    }
                }
            }
        }
    }
}
