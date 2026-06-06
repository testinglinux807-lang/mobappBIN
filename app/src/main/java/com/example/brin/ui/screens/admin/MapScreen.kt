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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.example.brin.data.api.OptimalRouteData
import com.example.brin.data.api.RouteStop
import com.example.brin.data.repository.BinRepository
import com.example.brin.data.websocket.WebSocketManager
import com.example.brin.data.websocket.WsEvent
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.ByteArrayOutputStream
import kotlin.math.*
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

@OptIn(ExperimentalMaterial3Api::class)
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
    var showRouteSelector by remember { mutableStateOf(false) }
    var selectedRouteBins by remember { mutableStateOf<Set<String>>(emptySet()) }
    var routeLoading   by remember { mutableStateOf(false) }
    var routeData      by remember { mutableStateOf<OptimalRouteData?>(null) }
    var routeError     by remember { mutableStateOf<String?>(null) }
    var allBins        by remember { mutableStateOf<List<BinData>>(emptyList()) }
    val sheetState     = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context        = LocalContext.current
    val scope          = rememberCoroutineScope()

    LaunchedEffect(Unit) { BinRepository.getBins().onSuccess { allBins = it } }

    // Live bin updates via WebSocket — patch markers without re-fetch
    LaunchedEffect(Unit) {
        WebSocketManager.events.collect { event ->
            when (event) {
                is WsEvent.BinUpdate -> allBins = allBins.map { bin ->
                    if (bin.id == event.binId) bin.copy(
                        capacity   = event.volume,
                        battery    = event.battery,
                        gas        = event.gas,
                        status     = when { event.volume >= 90 -> BinStatus.CRITICAL; event.volume >= 70 -> BinStatus.NEED_PICKUP; else -> BinStatus.NORMAL },
                        lastUpdate = "Baru saja"
                    ) else bin
                }
                is WsEvent.BinStatus -> allBins = allBins.map { bin ->
                    if (bin.nodeId == event.nodeId) bin.copy(online = event.status.equals("online", ignoreCase = true)) else bin
                }
                else -> Unit
            }
        }
    }

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

    val filteredBins = remember(activeFilter, allBins) {
        when (activeFilter) {
            MapFilter.ALL         -> allBins
            MapFilter.CRITICAL    -> allBins.filter { it.status == BinStatus.CRITICAL }
            MapFilter.NEED_PICKUP -> allBins.filter { it.status == BinStatus.NEED_PICKUP }
            MapFilter.NORMAL      -> allBins.filter { it.status == BinStatus.NORMAL }
        }
    }
    val searchResults = remember(searchQuery, allBins) {
        if (searchQuery.length < 2) emptyList()
        else allBins.filter { it.nodeId.contains(searchQuery, ignoreCase = true) || it.location.contains(searchQuery, ignoreCase = true) }
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
                                    Text(bin.nodeId, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
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

        if (selectedBin != null) {
            ModalBottomSheet(
                onDismissRequest = { selectedBin = null },
                sheetState       = sheetState,
                containerColor   = CardBg,
            ) {
                selectedBin?.let { bin -> BinPopupCard(bin = bin, onDetail = { onBinClick(bin.id) }) }
            }
        }

        // Generate Rute QR FAB
        FloatingActionButton(
            onClick = {
                // Pre-select bins that need attention (critical + need pickup)
                selectedRouteBins = allBins
                    .filter { it.status == BinStatus.CRITICAL || it.status == BinStatus.NEED_PICKUP }
                    .map { it.id }.toSet()
                showRouteSelector = true
            },
            containerColor = GreenPrimary,
            contentColor   = Color.White,
            modifier       = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 96.dp)
        ) {
            Icon(Icons.Default.AltRoute, contentDescription = "Generate Rute QR")
        }

        if (showRouteSelector) {
            RouteBinSelector(
                bins        = allBins,
                selectedIds = selectedRouteBins,
                onToggle    = { id ->
                    selectedRouteBins = if (selectedRouteBins.contains(id)) selectedRouteBins - id else selectedRouteBins + id
                },
                onToggleAll = { all ->
                    selectedRouteBins = if (all) allBins.map { it.id }.toSet() else emptySet()
                },
                onDismiss   = { showRouteSelector = false },
                onGenerate  = {
                    val chosen = allBins.filter { selectedRouteBins.contains(it.id) }
                    showRouteSelector = false
                    routeData  = null
                    routeError = null
                    routeLoading = true
                    showRouteDialog = true
                    scope.launch {
                        val start = userLocation ?: GeoPoint(-6.9175, 107.6191)
                        routeData = buildLocalRoute(start, chosen)
                        routeLoading = false
                    }
                }
            )
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
private fun RouteBinSelector(
    bins: List<BinData>,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit,
    onToggleAll: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onGenerate: () -> Unit
) {
    // Order: critical & need-pickup first, then the rest
    val ordered = remember(bins) {
        bins.sortedBy {
            when (it.status) {
                BinStatus.CRITICAL    -> 0
                BinStatus.NEED_PICKUP -> 1
                BinStatus.NORMAL      -> 2
            }
        }
    }
    val allSelected = selectedIds.size == bins.size && bins.isNotEmpty()

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = CardBg) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Pilih Bin untuk Rute", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextHint) }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onToggleAll(!allSelected) }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = allSelected, onCheckedChange = { onToggleAll(it) }, colors = CheckboxDefaults.colors(checkedColor = GreenPrimary))
                    Text("Pilih semua", fontSize = 13.sp, color = TextSecondary)
                }

                Spacer(Modifier.height(4.dp))

                Column(modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                    if (ordered.isEmpty()) {
                        Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                            Text("Belum ada bin.", color = TextHint, fontSize = 13.sp)
                        }
                    } else {
                        ordered.forEach { bin ->
                            val checked = selectedIds.contains(bin.id)
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onToggle(bin.id) }.padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = checked, onCheckedChange = { onToggle(bin.id) }, colors = CheckboxDefaults.colors(checkedColor = GreenPrimary))
                                Spacer(Modifier.width(4.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(bin.nodeId, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                    Text(bin.location, fontSize = 11.sp, color = TextSecondary)
                                }
                                BinStatusBadge(bin.status)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = onGenerate,
                    enabled = selectedIds.isNotEmpty(),
                    colors  = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    shape   = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AltRoute, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Generate Rute (${selectedIds.size})", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/** Builds an optimized route client-side via greedy nearest-neighbor from [start]. */
private fun buildLocalRoute(start: GeoPoint, bins: List<BinData>): OptimalRouteData {
    if (bins.isEmpty()) return OptimalRouteData(emptyList(), null, null, "Tidak ada bin dipilih.")

    val remaining = bins.toMutableList()
    val ordered   = mutableListOf<RouteStop>()
    var curLat = start.latitude
    var curLng = start.longitude
    while (remaining.isNotEmpty()) {
        val next = remaining.minByOrNull { haversineKm(curLat, curLng, it.lat, it.lng) }!!
        val dist = haversineKm(curLat, curLng, next.lat, next.lng)
        ordered.add(RouteStop(next.id, next.nodeId, next.location, next.lat, next.lng, dist))
        curLat = next.lat; curLng = next.lng
        remaining.remove(next)
    }

    val origin     = "${start.latitude},${start.longitude}"
    val destination = "${ordered.last().lat},${ordered.last().lng}"
    val waypoints  = ordered.dropLast(1).joinToString("|") { "${it.lat},${it.lng}" }
    val url = buildString {
        append("https://www.google.com/maps/dir/?api=1&origin=$origin&destination=$destination")
        if (waypoints.isNotEmpty()) append("&waypoints=$waypoints")
        append("&travelmode=driving")
    }
    return OptimalRouteData(ordered, url, genQrBase64(url), null)
}

private fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
    return r * 2 * atan2(sqrt(a), sqrt(1 - a))
}

private fun genQrBase64(text: String): String? = runCatching {
    val size = 600
    val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    for (x in 0 until size) for (y in 0 until size) {
        bmp.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
    }
    val baos = ByteArrayOutputStream()
    bmp.compress(Bitmap.CompressFormat.PNG, 100, baos)
    android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.NO_WRAP)
}.getOrNull()

@Composable
private fun MapFilterChip(activeFilter: MapFilter, filter: MapFilter, label: String, color: Color, onClick: () -> Unit) {
    val isActive = activeFilter == filter
    Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(if (isActive) color else Color.Transparent).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
        Text(text = label, fontSize = 12.sp, fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal, color = if (isActive) Color.White else TextSecondary)
    }
}

@Composable
private fun BinPopupCard(bin: BinData, onDetail: () -> Unit) {
    val statusCol = statusColor(bin.status)
    val statusLbl = statusLabel(bin.status)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 4.dp, bottom = 28.dp)
            .navigationBarsPadding()
    ) {
        Text(bin.nodeId, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(bin.location, fontSize = 13.sp, color = TextSecondary)
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Kapasitas", fontSize = 14.sp, color = TextSecondary)
            Text("${bin.capacity}%", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = statusCol)
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(progress = { bin.capacity / 100f }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = statusCol, trackColor = DividerColor)
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(6.dp), color = statusCol.copy(alpha = 0.12f)) {
                Text(statusLbl, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = statusCol, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
            }
            Button(
                onClick = onDetail,
                colors  = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                shape   = RoundedCornerShape(10.dp)
            ) {
                Text("Lihat Detail", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
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
