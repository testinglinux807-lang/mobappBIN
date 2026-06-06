package com.example.brin.data.api

import com.google.gson.annotations.SerializedName

// ── Auth ──────────────────────────────────────────────────────────────────────

data class LoginRequest(val email: String, val password: String)

data class LoginResponse(
    val success: Boolean,
    val message: String,
    val data: LoginData?
)

data class LoginData(
    val token: String,
    val user: ApiUser
)

data class ApiUser(
    val id: String,
    val name: String,
    val email: String,
    val role: String,       // "ADMIN" | "PETUGAS"
    val areaId: String?,
    val area: ApiArea?
)

data class MeResponse(
    val success: Boolean,
    val message: String,
    val data: ApiUser?
)

data class ChangePasswordRequest(val oldPassword: String, val newPassword: String)

// ── Areas ─────────────────────────────────────────────────────────────────────

data class ApiArea(
    val id: String,
    val name: String,
    val createdAt: String? = null,
    val _count: AreaCount? = null
)
data class AreaCount(val bins: Int = 0, val users: Int = 0)

// ── Bins ──────────────────────────────────────────────────────────────────────

data class BinsResponse(
    val success: Boolean,
    val message: String,
    val data: List<ApiBin>?
)

data class BinDetailResponse(
    val success: Boolean,
    val message: String,
    val data: ApiBinDetail?
)

data class ApiBin(
    val id: String,
    val nodeId: String,
    val location: String,
    val lat: Double,
    val lng: Double,
    val areaId: String?,
    val area: ApiArea? = null,
    val createdAt: String? = null,
    val status: String? = null,   // "online" | "offline" — absent in create/update responses
    val lastSeen: String? = null,
    val latest: BinLatest? = null
)

data class ApiBinDetail(
    val id: String,
    val nodeId: String,
    val location: String,
    val lat: Double,
    val lng: Double,
    val areaId: String?,
    val area: ApiArea? = null,
    val status: String,
    val lastSeen: String?,
    val latest: BinLatest?,
    val threshold: BinThreshold?
)

data class BinLatest(
    val weight: Double,
    val volume: Int,
    val battery: Int,
    val gas: Double,
    val rssi: Int,
    val timestamp: String,
    val logId: String
)

data class BinThreshold(
    val weight: Double?,
    val volume: Int?,
    val gas: Double?,
    val battery: Int?
)

// ── Bin History ───────────────────────────────────────────────────────────────

data class BinHistoryResponse(
    val success: Boolean,
    val data: List<BinSensorLog>?,
    val pagination: Pagination?
)

data class BinSensorLog(
    val id: String,
    val binId: String,
    val weight: Double,
    val volume: Int,
    val battery: Int,
    val gas: Double,
    val rssi: Int,
    val createdAt: String
)

// ── Optimal Route ─────────────────────────────────────────────────────────────

data class OptimalRouteResponse(
    val success: Boolean,
    val message: String,
    val data: OptimalRouteData?
)

data class OptimalRouteData(
    val route: List<RouteStop>,
    val googleMapsUrl: String?,
    val qrCodeBase64: String?,
    val message: String?
)

data class RouteStop(
    val id: String,
    val nodeId: String,
    val location: String,
    val lat: Double,
    val lng: Double,
    val distanceFromPreviousKm: Double
)

// ── Alerts ────────────────────────────────────────────────────────────────────

data class AlertsResponse(
    val success: Boolean,
    val data: List<ApiAlert>?,
    val pagination: Pagination?
)

data class AlertDetailResponse(
    val success: Boolean,
    val data: ApiAlert?
)

data class ApiAlert(
    val id: String,
    val binId: String,
    val type: String,         // "FULL_WEIGHT" | "FULL_VOLUME" | "BATTERY_LOW" | "GAS_HIGH"
    val message: String,
    val resolved: Boolean,
    val createdAt: String,
    val resolvedAt: String?,
    val bin: AlertBinInfo?
)

data class AlertBinInfo(val nodeId: String, val location: String)

// ── Users ─────────────────────────────────────────────────────────────────────

data class UsersResponse(val success: Boolean, val data: List<ApiUser>?, val pagination: Pagination?)
data class UserDetailResponse(val success: Boolean, val message: String, val data: ApiUser?)
data class CreateUserRequest(val name: String, val email: String, val password: String, val role: String = "PETUGAS", val areaId: String? = null)
data class UpdateUserRequest(val name: String? = null, val email: String? = null, val password: String? = null, val role: String? = null, val areaId: String? = null)

// ── Areas ─────────────────────────────────────────────────────────────────────

data class AreasResponse(val success: Boolean, val data: List<ApiArea>?)
data class AreaDetailResponse(val success: Boolean, val message: String, val data: ApiArea?)
data class AreaRequest(val name: String)

// ── Bin CRUD Requests ─────────────────────────────────────────────────────────

data class CreateBinRequest(
    val nodeId: String,
    val location: String,
    val lat: Double,
    val lng: Double,
    val areaId: String? = null
)

data class UpdateBinRequest(
    val nodeId: String? = null,
    val location: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val areaId: String? = null
)

data class ThresholdRequest(
    val weightThreshold: Double? = null,
    val volumeThreshold: Int? = null,
    val gasThreshold: Double? = null,
    val batteryThreshold: Int? = null
)

data class BinSingleResponse(
    val success: Boolean,
    val message: String,
    val data: ApiBin?
)

// ── Pickups ───────────────────────────────────────────────────────────────────

data class CompletePickupRequest(val lat: Double? = null, val lng: Double? = null)

data class ApiPickup(
    val id: String,
    val binId: String,
    val petugasId: String,
    val areaId: String?,
    val alertId: String?,
    val status: String,          // "MENUNGGU_SENSOR" | "SELESAI"
    val completedAt: String,
    val completedLat: Double?,
    val completedLng: Double?,
    val sensorConfirmedAt: String?,
    val manualConfirmedAt: String? = null,
    val createdAt: String,
    val bin: PickupBinInfo?,
    val petugas: PickupPetugasInfo?
)

data class PickupBinInfo(val nodeId: String, val location: String, val lat: Double, val lng: Double)
data class PickupPetugasInfo(val id: String, val name: String, val email: String)

data class PickupResponse(val success: Boolean, val message: String, val data: ApiPickup?)
data class PickupsResponse(val success: Boolean, val data: List<ApiPickup>?, val pagination: Pagination?)

// ── Schedules ───────────────────────────────────────────────────────────────

data class ApiSchedule(
    val id: String,
    val petugasId: String,
    val areaId: String?,
    val date: String,            // ISO date
    val startTime: String,       // "08.00"
    val endTime: String,         // "10.00"
    val truck: String?,
    val binTarget: Int,
    val note: String?,
    val status: String,          // "PENDING" | "PROSES" | "SELESAI"
    val createdAt: String,
    val updatedAt: String,
    val petugas: SchedulePetugas?,
    val area: ScheduleArea?
)

data class SchedulePetugas(val id: String, val name: String, val email: String)
data class ScheduleArea(val id: String, val name: String)

data class CreateScheduleRequest(
    val petugasId: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val areaId: String? = null,
    val truck: String? = null,
    val binTarget: Int = 0,
    val note: String? = null
)

data class UpdateScheduleRequest(
    val status: String? = null,
    val petugasId: String? = null,
    val areaId: String? = null,
    val date: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val truck: String? = null,
    val binTarget: Int? = null,
    val note: String? = null
)

data class ScheduleResponse(val success: Boolean, val message: String, val data: ApiSchedule?)
data class SchedulesResponse(val success: Boolean, val data: List<ApiSchedule>?, val pagination: Pagination?)

// ── Standard / Pagination ─────────────────────────────────────────────────────

data class StandardResponse(val success: Boolean, val message: String)

data class Pagination(val total: Int, val page: Int, val limit: Int, val totalPages: Int)
