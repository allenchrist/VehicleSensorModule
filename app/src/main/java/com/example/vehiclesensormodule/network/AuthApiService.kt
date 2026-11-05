package com.example.vehiclesensormodule.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT

// --------------------------------------------------
// LOGIN
// --------------------------------------------------

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val message: String,
    val token: String,
    val user: User
)

// --------------------------------------------------
// SIGNUP / REGISTER
// --------------------------------------------------

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val vehicleType: String
)

data class RegisterResponse(
    val message: String,
    val user: User
)

// --------------------------------------------------
// DEVICE ASSOCIATION
// --------------------------------------------------

data class DeviceAssociationRequest(
    val deviceId: String
)

data class DeviceAssociationResponse(
    val message: String,
    val user: User,
    val deviceId: String
)

// --------------------------------------------------
// USER
// --------------------------------------------------

data class User(
    val username: String,
    val email: String,
    val vehicleType: String?
)

data class ProfileResponse(
    val user: User
)

// --------------------------------------------------
// VEHICLE TYPE
// --------------------------------------------------

data class VehicleTypeRequest(
    val vehicleType: String
)

data class VehicleTypeResponse(
    val vehicleType: String
)

// --------------------------------------------------
// AUTH API
// --------------------------------------------------

interface AuthApiService {

    @POST("api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @POST("api/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<RegisterResponse>

    @GET("api/auth/profile")
    suspend fun getProfile(
        @Header("Authorization") authorization: String
    ): Response<ProfileResponse>

    @PUT("api/auth/vehicle-type")
    suspend fun updateVehicleType(
        @Header("Authorization") authorization: String,
        @Body request: VehicleTypeRequest
    ): Response<VehicleTypeResponse>

    @POST("api/auth/device")
    suspend fun associateDevice(
        @Header("Authorization") authorization: String,
        @Body request: DeviceAssociationRequest
    ): Response<DeviceAssociationResponse>

    @POST("api/auth/device/unlink")
    suspend fun unlinkDevice(
        @Header("Authorization") authorization: String,
        @Body request: DeviceAssociationRequest
    ): Response<Unit>
}