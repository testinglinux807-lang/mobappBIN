package com.example.brin.data.api

import retrofit2.http.*

interface ApiService {

    // Auth
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @GET("auth/me")
    suspend fun getMe(): MeResponse

    @PUT("auth/password")
    suspend fun changePassword(@Body body: ChangePasswordRequest): StandardResponse

    // Bins
    @GET("bins")
    suspend fun getBins(): BinsResponse

    @GET("bins/{id}")
    suspend fun getBinById(@Path("id") id: String): BinDetailResponse

    @GET("bins/{id}/history")
    suspend fun getBinHistory(
        @Path("id") id: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50
    ): BinHistoryResponse

    @GET("bins/route/optimal")
    suspend fun getOptimalRoute(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double
    ): OptimalRouteResponse

    @POST("bins")
    suspend fun createBin(@Body body: CreateBinRequest): BinSingleResponse

    @PUT("bins/{id}")
    suspend fun updateBin(@Path("id") id: String, @Body body: UpdateBinRequest): BinSingleResponse

    @PUT("bins/{id}/threshold")
    suspend fun setThreshold(@Path("id") id: String, @Body body: ThresholdRequest): StandardResponse

    @DELETE("bins/{id}")
    suspend fun deleteBin(@Path("id") id: String): StandardResponse

    // Users
    @GET("users")
    suspend fun getUsers(@Query("page") page: Int = 1, @Query("limit") limit: Int = 100): UsersResponse

    @GET("users/{id}")
    suspend fun getUserById(@Path("id") id: String): UserDetailResponse

    @POST("users")
    suspend fun createUser(@Body body: CreateUserRequest): UserDetailResponse

    @PUT("users/{id}")
    suspend fun updateUser(@Path("id") id: String, @Body body: UpdateUserRequest): UserDetailResponse

    @DELETE("users/{id}")
    suspend fun deleteUser(@Path("id") id: String): StandardResponse

    // Areas
    @GET("areas")
    suspend fun getAreas(): AreasResponse

    @GET("areas/{id}")
    suspend fun getAreaById(@Path("id") id: String): AreaDetailResponse

    @POST("areas")
    suspend fun createArea(@Body body: AreaRequest): AreaDetailResponse

    @PUT("areas/{id}")
    suspend fun updateArea(@Path("id") id: String, @Body body: AreaRequest): AreaDetailResponse

    @DELETE("areas/{id}")
    suspend fun deleteArea(@Path("id") id: String): StandardResponse

    // Pickups
    @POST("pickups/{binId}/complete")
    suspend fun completePickup(@Path("binId") binId: String, @Body body: CompletePickupRequest): PickupResponse

    @GET("pickups")
    suspend fun getPickups(@Query("page") page: Int = 1, @Query("limit") limit: Int = 50): PickupsResponse

    @GET("pickups/{id}")
    suspend fun getPickupById(@Path("id") id: String): PickupResponse

    @POST("pickups/{id}/confirm")
    suspend fun confirmPickup(@Path("id") id: String): PickupResponse

    // Schedules
    @GET("schedules")
    suspend fun getSchedules(
        @Query("date") date: String? = null,
        @Query("status") status: String? = null,
        @Query("petugasId") petugasId: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 100
    ): SchedulesResponse

    @POST("schedules")
    suspend fun createSchedule(@Body body: CreateScheduleRequest): ScheduleResponse

    @PUT("schedules/{id}")
    suspend fun updateSchedule(@Path("id") id: String, @Body body: UpdateScheduleRequest): ScheduleResponse

    @DELETE("schedules/{id}")
    suspend fun deleteSchedule(@Path("id") id: String): StandardResponse

    // Alerts
    @GET("alerts")
    suspend fun getAlerts(
        @Query("resolved") resolved: Boolean? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50
    ): AlertsResponse

    @PUT("alerts/{id}/resolve")
    suspend fun resolveAlert(@Path("id") id: String): AlertDetailResponse

    // Devices (remote control Raspberry Pi) — ADMIN only, backend dukung role tsb
    @GET("devices/state")
    suspend fun getDeviceStates(): DeviceStateResponse

    @GET("devices/{nodeId}/status")
    suspend fun getDeviceStatus(
        @Path("nodeId") nodeId: String,
        @Query("live") live: Int = 1
    ): DeviceStateSingleResponse

    @POST("devices/{nodeId}/command")
    suspend fun sendDeviceCommand(
        @Path("nodeId") nodeId: String,
        @Body body: DeviceCommandRequest
    ): DeviceCommandResponse

    @POST("devices/{nodeId}/camera/start")
    suspend fun deviceCameraStart(@Path("nodeId") nodeId: String): DeviceCommandResponse

    @POST("devices/{nodeId}/camera/stop")
    suspend fun deviceCameraStop(@Path("nodeId") nodeId: String): DeviceCommandResponse

    @POST("devices/{nodeId}/logs")
    suspend fun deviceToggleLogs(
        @Path("nodeId") nodeId: String,
        @Body body: DeviceLogsRequest
    ): DeviceCommandResponse
}
