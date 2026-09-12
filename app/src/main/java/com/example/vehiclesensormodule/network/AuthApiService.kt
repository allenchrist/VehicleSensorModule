package com.example.vehiclesensormodule.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

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
    val password: String
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
    val email: String
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

    @POST("api/auth/device")
    suspend fun associateDevice(
        @Header("Authorization") authorization: String,
        @Body request: DeviceAssociationRequest
    ): Response<DeviceAssociationResponse>
}