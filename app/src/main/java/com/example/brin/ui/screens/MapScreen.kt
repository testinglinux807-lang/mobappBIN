package com.example.brin.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.brin.ui.theme.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

private enum class MapFilter { ALL, CRITICAL, NEED_PICKUP, NORMAL }

@Composable
fun MapScreen(onBinClick: (String) -> Unit) {
    var activeFilter  by remember { mutableStateOf(MapFilter.ALL) }
    var selectedBin   by remember { mutableStateOf<BinData?>(null) }
    var searchQuery   by remember { mutableStateOf("") }
    var searchFocused by remember { mutableStateOf(false) }
    var centerOn      by remember { mutableStateOf<GeoPoint?>(null) }
    var userLocation  by remember { mutableStateOf<GeoPoint?>(null) }
    var locSnackbar   by remember { mutableStateOf("") }
    val context       = LocalContext.current

    fun fetchLocation() {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val loc = try {
            lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
        } catch (e: SecurityException) { null }

        if (loc != null) {
            val gp = GeoPoint(loc.latitude, loc.longitude)
            userLocation = gp
            centerOn     = gp
            locSnackbar  = ""
        } else {
            locSnackbar = "Lokasi tidak ditemukan. Pastikan GPS aktif."
        }
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms.values.any { it }) fetchLocation()
        else locSnackbar = "Izin lokasi ditolak."
    }

    fun onMyLocationClick() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        if (granted) fetchLocation()
        else permLauncher.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))
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
        else MockData.bins.filter {
            it.id.contains(searchQuery, ignoreCase = true) ||
            it.location.contains(searchQuery, ignoreCase = true)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        OsmMapView(
            bins          = filteredBins,
            centerOn      = centerOn,
            userLocation  = userLocation,
            modifier      = Modifier.fillMaxSize(),
            onMarkerClick = { selectedBin = it }
        )

        // Top controls: search + filter
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 12.dp, start = 12.dp, end = 12.dp)
        ) {
            // Search bar
            Surface(
                shape  = RoundedCornerShape(14.dp),
                color  = CardBg,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier          = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, contentDescription = null,
                        tint = if (searchFocused) GreenPrimary else TextHint,
                        modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(
                        value         = searchQuery,
                        onValueChange = { searchQuery = it; searchFocused = true },
                        singleLine    = true,
                        textStyle     = androidx.compose.ui.text.TextStyle(
                            fontSize = 14.sp,
                            color    = TextPrimary.toAndroidColor()
                        ),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            if (searchQuery.isEmpty()) {
                                Text("Cari bin atau lokasi...", color = TextHint, fontSize = 14.sp)
                            }
                            inner()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Hapus",
                            tint = TextHint,
                            modifier = Modifier.size(18.dp).clickable {
                                searchQuery = ""; searchFocused = false
                            }
                        )
                    }
                }
            }

            // Search results dropdown
            if (searchResults.isNotEmpty() && searchFocused) {
                Spacer(Modifier.height(4.dp))
                Surface(
                    shape  = RoundedCornerShape(14.dp),
                    color  = CardBg,
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        searchResults.take(5).forEachIndexed { index, bin ->
                            val statusCol = statusColor(bin.status)
                            Row(
                                modifier          = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedBin   = bin
                                        centerOn      = GeoPoint(bin.lat, bin.lng)
                                        searchQuery   = ""
                                        searchFocused = false
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = null,
                                    tint = statusCol, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(bin.id, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                    Text(bin.location, fontSize = 12.sp, color = TextSecondary)
                                }
                                BinStatusBadge(bin.status)
                            }
                            if (index < searchResults.take(5).lastIndex)
                                HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 14.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Filter chips
            Row(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(24.dp))
                    .background(CardBg)
                    .shadow(4.dp, RoundedCornerShape(24.dp))
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MapFilterChip(activeFilter, MapFilter.ALL,         "Semua",        GreenPrimary)   { activeFilter = MapFilter.ALL }
                MapFilterChip(activeFilter, MapFilter.CRITICAL,    "Kritis",       StatusCritical) { activeFilter = MapFilter.CRITICAL }
                MapFilterChip(activeFilter, MapFilter.NEED_PICKUP, "Perlu Pickup", StatusWarning)  { activeFilter = MapFilter.NEED_PICKUP }
                MapFilterChip(activeFilter, MapFilter.NORMAL,      "Normal",       StatusNormal)   { activeFilter = MapFilter.NORMAL }
            }
        }

        // My location button
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp, top = 80.dp)
                .size(40.dp)
                .clip(CircleShape)
                .shadow(4.dp, CircleShape)
                .background(CardBg)
                .clickable { onMyLocationClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.MyLocation,
                contentDescription = null,
                tint     = if (userLocation != null) Color(0xFF1976D2) else TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }

        // Snackbar error lokasi
        if (locSnackbar.isNotEmpty()) {
            Surface(
                shape    = RoundedCornerShape(10.dp),
                color    = Color(0xFF323232),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp, start = 16.dp, end = 16.dp)
            ) {
                Row(
                    modifier          = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.LocationOff, contentDescription = null,
                        tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(locSnackbar, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    Text("OK", color = Color(0xFF80CBC4), fontSize = 13.sp,
                        modifier = Modifier.clickable { locSnackbar = "" })
                }
            }
        }

        // Bin detail popup
        AnimatedVisibility(
            visible  = selectedBin != null,
            enter    = slideInVertically { it },
            exit     = slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            selectedBin?.let { bin ->
                BinPopupCard(
                    bin      = bin,
                    onClose  = { selectedBin = null },
                    onDetail = { onBinClick(bin.id) }
                )
            }
        }
    }
}

// Helper — Color compose ke Android color int untuk TextStyle
private fun androidx.compose.ui.graphics.Color.toAndroidColor(): androidx.compose.ui.graphics.Color = this

@Composable
private fun MapFilterChip(
    activeFilter: MapFilter,
    filter: MapFilter,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    val isActive = activeFilter == filter
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isActive) color else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = label,
            fontSize   = 12.sp,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
            color      = if (isActive) Color.White else TextSecondary
        )
    }
}

@Composable
private fun BinPopupCard(bin: BinData, onClose: () -> Unit, onDetail: () -> Unit) {
    val statusColor = statusColor(bin.status)
    val statusLabel = statusLabel(bin.status)

    Surface(
        shape    = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color    = CardBg,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
    ) {
        Column(modifier = Modifier.padding(20.dp).padding(bottom = 80.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text(bin.id, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("· ${bin.location}", fontSize = 13.sp, color = TextSecondary)
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextHint)
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("Kapasitas", fontSize = 14.sp, color = TextSecondary)
                Text("${bin.capacity}%", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = statusColor)
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress   = { bin.capacity / 100f },
                modifier   = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color      = statusColor,
                trackColor = DividerColor
            )

            Spacer(Modifier.height(14.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Surface(shape = RoundedCornerShape(6.dp), color = statusColor.copy(alpha = 0.12f)) {
                    Text(
                        statusLabel,
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = statusColor,
                        modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDetail) {
                        Text("Lihat Detail", color = GreenPrimary, fontSize = 13.sp)
                        Icon(Icons.Default.ChevronRight, contentDescription = null,
                            tint = GreenPrimary, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(GreenPrimary)
                            .clickable { },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Navigation, contentDescription = "Navigasi",
                            tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun OsmMapView(
    bins: List<BinData>,
    centerOn: GeoPoint?,
    userLocation: GeoPoint?,
    modifier: Modifier = Modifier,
    onMarkerClick: (BinData) -> Unit
) {
    val context   = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    Configuration.getInstance().userAgentValue = context.packageName

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(13.0)
            controller.setCenter(GeoPoint(-6.9175, 107.6191))
        }
    }

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE  -> mapView.onPause()
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    LaunchedEffect(centerOn) {
        centerOn?.let { mapView.controller.animateTo(it, 16.0, 800L) }
    }

    LaunchedEffect(bins) {
        mapView.overlays.clear()
        bins.forEach { bin ->
            val argb = when (bin.status) {
                BinStatus.CRITICAL    -> StatusCritical.toArgb()
                BinStatus.NEED_PICKUP -> StatusWarning.toArgb()
                BinStatus.NORMAL      -> StatusNormal.toArgb()
            }
            val marker = Marker(mapView).apply {
                position = GeoPoint(bin.lat, bin.lng)
                title    = bin.id
                snippet  = bin.location
                icon     = createPinDrawable(argb, context.resources)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
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
            id       = "user_loc"
            position = userLocation
            title    = "Lokasi Anda"
            icon     = createUserDotDrawable(context.resources)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        }
        mapView.overlays.add(dot)
        mapView.invalidate()
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

private fun createUserDotDrawable(resources: Resources): BitmapDrawable {
    val dp     = resources.displayMetrics.density
    val size   = (22 * dp).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint  = Paint(Paint.ANTI_ALIAS_FLAG)
    val cx     = size / 2f

    // White ring
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(cx, cx, cx, paint)
    // Blue fill
    paint.color = android.graphics.Color.parseColor("#1976D2")
    canvas.drawCircle(cx, cx, cx * 0.72f, paint)
    // White inner dot
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(cx, cx, cx * 0.28f, paint)

    return BitmapDrawable(resources, bitmap)
}

private fun createPinDrawable(color: Int, resources: Resources): BitmapDrawable {
    val density = resources.displayMetrics.density
    val w = (28 * density).toInt()
    val h = (40 * density).toInt()
    val r = w / 2f
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }

    // Pin body: circle top + teardrop tail
    val path = Path().apply {
        addCircle(r, r, r, Path.Direction.CW)
        moveTo(r - r * 0.45f, r + r * 0.65f)
        lineTo(r + r * 0.45f, r + r * 0.65f)
        lineTo(r, h.toFloat())
        close()
    }
    canvas.drawPath(path, paint)

    // White inner dot
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(r, r, r * 0.38f, paint)

    return BitmapDrawable(resources, bitmap)
}
