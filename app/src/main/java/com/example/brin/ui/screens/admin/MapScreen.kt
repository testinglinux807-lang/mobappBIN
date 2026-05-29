package com.example.brin.ui.screens.admin

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.brin.data.BinData
import com.example.brin.data.BinStatus
import com.example.brin.data.MockData
import com.example.brin.data.api.OptimalRouteData
import com.example.brin.data.repository.BinRepository
import com.example.brin.ui.screens.shared.BinStatusBadge
import com.example.brin.ui.screens.shared.statusColor
import com.example.brin.ui.screens.shared.statusLabel
import com.example.brin.ui.theme.*
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

private enum class MapFilter { ALL, CRITICAL, NEED_PICKUP, NORMAL }

@Composable
fun MapScreen(onBinClick: (String) -> Unit) {
    var activeFilter   by remember { mutableStateOf(MapFilter.ALL) }
    var selectedBin    by remember { mutableStateOf<BinData?>(null) }
    var searchQuery    by remember { mutableStateOf("") }
    var searchFocused  by remember { mutableStateOf(false) }
    var centerOn       by remember { mutableStateOf<GeoPoint?>(null) }
    var userLocation   by remember { mutableStateOf<GeoPoint?>(null) }
    var locSnackbar    by remember { mutableStateOf("") }
    var showRouteDialog by remember { mutableStateOf(false) }
    var routeLoading   by remember { mutableStateOf(false) }
    var routeData      by remember { mutableStateOf<OptimalRouteData?>(null) }
    var routeError     by remember { mutableStateOf<String?>(null) }
    val context        = LocalContext.current
    val scope          = rememberCoroutineScope()

    fun fetchLocation() {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val loc = try {
            lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
        } catch (e: SecurityException) { null }
        if (loc != null) { userLocation = GeoPoint(loc.latitude, loc.longitude); centerOn = userLocation; locSnackbar = "" }
        else locSnackbar = "Lokasi tidak ditemukan. Pastikan GPS aktif."
    }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        if (perms.values.any { it }) fetchLocation() else locSnackbar = "Izin lokasi ditolak."
    }

    fun onMyLocationClick() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (granted) fetchLocation()
        else permLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    val filteredBins = remember(activeFilter) {
        when (activeFilter) {
            MapFilter.ALL         -> MockData.bins
            MapFilter.CRITICAL    -> MockData.bins.filter { it.status == BinStatus.CRITICAL }
            MapFilter.NEED_PICKUP -> MockData.bins.filter { it.status == BinStatus.NEED_PICKUP }
            MapFilter.NORMAL      -> MockData.bins.filter { it.status == BinStatus.NORMAL }
        }
    }
    val searchResults = remember(searchQuery) {
        if (searchQuery.length < 2) emptyList()
        else MockData.bins.filter { it.id.contains(searchQuery, ignoreCase = true) || it.location.contains(searchQuery, ignoreCase = true) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        OsmMapView(bins = filteredBins, centerOn = centerOn, userLocation = userLocation, modifier = Modifier.fillMaxSize(), onMarkerClick = { selectedBin = it })

        Column(modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 12.dp, start = 12.dp, end = 12.dp)) {
            Surface(shape = RoundedCornerShape(14.dp), color = CardBg, shadowElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = if (searchFocused) GreenPrimary else TextHint, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(
                        value = searchQuery, onValueChange = { searchQuery = it; searchFocused = true },
                        singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = TextPrimary),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner -> if (searchQuery.isEmpty()) Text("Cari bin atau lokasi...", color = TextHint, fontSize = 14.sp); inner() }
                    )
                    if (searchQuery.isNotEmpty()) {
                        Icon(Icons.Default.Close, contentDescription = "Hapus", tint = TextHint, modifier = Modifier.size(18.dp).clickable { searchQuery = ""; searchFocused = false })
                    }
                }
            }

            if (searchResults.isNotEmpty() && searchFocused) {
                Spacer(Modifier.height(4.dp))
                Surface(shape = RoundedCornerShape(14.dp), color = CardBg, shadowElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
                    Column {
                        searchResults.take(5).forEachIndexed { index, bin ->
                            val statusCol = statusColor(bin.status)
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    selectedBin = bin; centerOn = GeoPoint(bin.lat, bin.lng); searchQuery = ""; searchFocused = false
                                }.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = statusCol, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(bin.id, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                    Text(bin.location, fontSize = 12.sp, color = TextSecondary)
                                }
                                BinStatusBadge(bin.status)
                            }
                            if (index < searchResults.take(5).lastIndex) HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 14.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally).clip(RoundedCornerShape(24.dp)).background(CardBg).shadow(4.dp, RoundedCornerShape(24.dp)).padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MapFilterChip(activeFilter, MapFilter.ALL,         "Semua",        GreenPrimary)   { activeFilter = MapFilter.ALL }
                MapFilterChip(activeFilter, MapFilter.CRITICAL,    "Kritis",       StatusCritical) { activeFilter = MapFilter.CRITICAL }
                MapFilterChip(activeFilter, MapFilter.NEED_PICKUP, "Perlu Pickup", StatusWarning)  { activeFilter = MapFilter.NEED_PICKUP }
                MapFilterChip(activeFilter, MapFilter.NORMAL,      "Normal",       StatusNormal)   { activeFilter = MapFilter.NORMAL }
            }
        }

        Box(
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp, top = 80.dp).size(40.dp).clip(CircleShape).shadow(4.dp, CircleShape).background(CardBg).clickable { onMyLocationClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = null, tint = if (userLocation != null) Color(0xFF1976D2) else TextSecondary, modifier = Modifier.size(20.dp))
        }

        if (locSnackbar.isNotEmpty()) {
            Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFF323232), modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp, start = 16.dp, end = 16.dp)) {
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(locSnackbar, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    Text("OK", color = Color(0xFF80CBC4), fontSize = 13.sp, modifier = Modifier.clickable { locSnackbar = "" })
                }
            }
        }

        AnimatedVisibility(visible = selectedBin != null, enter = slideInVertically { it }, exit = slideOutVertically { it }, modifier = Modifier.align(Alignment.BottomCenter)) {
            selectedBin?.let { bin -> BinPopupCard(bin = bin, onClose = { selectedBin = null }, onDetail = { onBinClick(bin.id) }) }
        }

        // Generate Rute QR FAB
        FloatingActionButton(
            onClick = {
                routeData  = null
                routeError = null
                showRouteDialog = true
                routeLoading    = true
                val lat = userLocation?.latitude  ?: -6.9175
                val lng = userLocation?.longitude ?: 107.6191
                scope.launch {
                    BinRepository.getOptimalRoute(lat, lng)
                        .onSuccess { routeData = it }
                        .onFailure { routeError = it.message ?: "Gagal memuat rute" }
                    routeLoading = false
                }
            },
            containerColor = GreenPrimary,
            contentColor   = Color.White,
            modifier       = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 96.dp)
        ) {
            Icon(Icons.Default.AltRoute, contentDescription = "Generate Rute QR")
        }

        if (showRouteDialog) {
            RouteQrDialog(
                isLoading = routeLoading,
                routeData = routeData,
                error     = routeError,
                context   = context,
                onDismiss = { showRouteDialog = false }
            )
        }
    }
}

@Composable
private fun RouteQrDialog(
    isLoading: Boolean,
    routeData: OptimalRouteData?,
    error: String?,
    context: Context,
    onDismiss: () -> Unit
) {
    val qrBitmap = remember(routeData?.qrCodeBase64) {
        routeData?.qrCodeBase64?.let { decodeBase64Bitmap(it) }
    }
    val isEmpty = routeData != null && routeData.route.isEmpty()

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = CardBg) {
            Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Rute Optimal", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextHint) }
                }

                Spacer(Modifier.height(12.dp))

                when {
                    isLoading -> Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = GreenPrimary)
                    }
                    error != null -> Text(error, color = StatusCritical, fontSize = 13.sp)
                    isEmpty -> {
                        Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusNormal, modifier = Modifier.size(48.dp))
                                Spacer(Modifier.height(10.dp))
                                Text(routeData?.message ?: "Tidak ada bin yang perlu dikosongkan.", color = TextSecondary, fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        }
                    }
                    else -> {
                        // QR Code
                        if (qrBitmap != null) {
                            Surface(shape = RoundedCornerShape(12.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
                                Image(
                                    bitmap = qrBitmap.asImageBitmap(),
                                    contentDescription = "QR Rute",
                                    modifier = Modifier.fillMaxWidth().height(220.dp).padding(12.dp)
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text("Tunjukkan QR ini ke petugas untuk membuka rute di Google Maps.", fontSize = 11.sp, color = TextHint, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(14.dp))
                        }

                        // Route list
                        Text("${routeData!!.route.size} bin dalam rute:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Spacer(Modifier.height(6.dp))
                        routeData.route.forEachIndexed { i, stop ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(22.dp).background(GreenPrimary, CircleShape), contentAlignment = Alignment.Center) {
                                    Text("${i + 1}", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(stop.nodeId, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                    Text(stop.location, fontSize = 11.sp, color = TextSecondary)
                                }
                                if (i > 0) Text("${String.format("%.1f", stop.distanceFromPreviousKm)} km", fontSize = 11.sp, color = TextHint)
                            }
                        }

                        // Google Maps button
                        routeData.googleMapsUrl?.let { url ->
                            Spacer(Modifier.height(14.dp))
                            Button(
                                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
                                colors  = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                                shape   = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Buka di Google Maps", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun decodeBase64Bitmap(dataUri: String): Bitmap? = runCatching {
    val base64 = if (dataUri.contains(",")) dataUri.substringAfter(",") else dataUri
    val bytes  = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}.getOrNull()

@Composable
private fun MapFilterChip(activeFilter: MapFilter, filter: MapFilter, label: String, color: Color, onClick: () -> Unit) {
    val isActive = activeFilter == filter
    Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(if (isActive) color else Color.Transparent).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
        Text(text = label, fontSize = 12.sp, fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal, color = if (isActive) Color.White else TextSecondary)
    }
}

@Composable
private fun BinPopupCard(bin: BinData, onClose: () -> Unit, onDetail: () -> Unit) {
    val statusCol = statusColor(bin.status)
    val statusLbl = statusLabel(bin.status)
    Surface(shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp), color = CardBg, modifier = Modifier.fillMaxWidth().shadow(16.dp, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))) {
        Column(modifier = Modifier.padding(20.dp).padding(bottom = 80.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(bin.id, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("· ${bin.location}", fontSize = 13.sp, color = TextSecondary)
                }
                IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextHint) }
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Kapasitas", fontSize = 14.sp, color = TextSecondary)
                Text("${bin.capacity}%", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = statusCol)
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(progress = { bin.capacity / 100f }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = statusCol, trackColor = DividerColor)
            Spacer(Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(6.dp), color = statusCol.copy(alpha = 0.12f)) {
                    Text(statusLbl, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = statusCol, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDetail) {
                        Text("Lihat Detail", color = GreenPrimary, fontSize = 13.sp)
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(4.dp))
                    Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(GreenPrimary).clickable { }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Navigation, contentDescription = "Navigasi", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun OsmMapView(bins: List<BinData>, centerOn: GeoPoint?, userLocation: GeoPoint?, modifier: Modifier = Modifier, onMarkerClick: (BinData) -> Unit) {
    val context   = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    Configuration.getInstance().userAgentValue = context.packageName

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(13.0)
            controller.setCenter(GeoPoint(-6.9175, 107.6191))
        }
    }

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) { Lifecycle.Event.ON_RESUME -> mapView.onResume(); Lifecycle.Event.ON_PAUSE -> mapView.onPause(); else -> {} }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); mapView.onDetach() }
    }

    LaunchedEffect(centerOn) { centerOn?.let { mapView.controller.animateTo(it, 16.0, 800L) } }

    LaunchedEffect(bins) {
        mapView.overlays.clear()
        bins.forEach { bin ->
            val argb = when (bin.status) { BinStatus.CRITICAL -> StatusCritical.toArgb(); BinStatus.NEED_PICKUP -> StatusWarning.toArgb(); BinStatus.NORMAL -> StatusNormal.toArgb() }
            val marker = Marker(mapView).apply {
                position = GeoPoint(bin.lat, bin.lng); title = bin.id; snippet = bin.location
                icon = createPinDrawable(argb, context.resources); setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                setOnMarkerClickListener { _, _ -> onMarkerClick(bin); true }
            }
            mapView.overlays.add(marker)
        }
        mapView.invalidate()
    }

    LaunchedEffect(userLocation) {
        userLocation ?: return@LaunchedEffect
        mapView.overlays.removeAll { it is Marker && (it as Marker).id == "user_loc" }
        val dot = Marker(mapView).apply {
            id = "user_loc"; position = userLocation; title = "Lokasi Anda"
            icon = createUserDotDrawable(context.resources); setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        }
        mapView.overlays.add(dot)
        mapView.invalidate()
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

private fun createUserDotDrawable(resources: Resources): BitmapDrawable {
    val dp = resources.displayMetrics.density; val size = (22 * dp).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888); val canvas = android.graphics.Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG); val cx = size / 2f
    paint.color = android.graphics.Color.WHITE; canvas.drawCircle(cx, cx, cx, paint)
    paint.color = android.graphics.Color.parseColor("#1976D2"); canvas.drawCircle(cx, cx, cx * 0.72f, paint)
    paint.color = android.graphics.Color.WHITE; canvas.drawCircle(cx, cx, cx * 0.28f, paint)
    return BitmapDrawable(resources, bitmap)
}

private fun createPinDrawable(color: Int, resources: Resources): BitmapDrawable {
    val density = resources.displayMetrics.density; val w = (28 * density).toInt(); val h = (40 * density).toInt(); val r = w / 2f
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); val canvas = android.graphics.Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
    val path = Path().apply {
        addCircle(r, r, r, Path.Direction.CW)
        moveTo(r - r * 0.45f, r + r * 0.65f); lineTo(r + r * 0.45f, r + r * 0.65f); lineTo(r, h.toFloat()); close()
    }
    canvas.drawPath(path, paint)
    paint.color = android.graphics.Color.WHITE; canvas.drawCircle(r, r, r * 0.38f, paint)
    return BitmapDrawable(resources, bitmap)
}
