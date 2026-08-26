package com.example.vehiclesensormodule.network

import android.content.Context
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

class VehicleWebSocketClient(context: Context) {

    companion object {
        private const val TAG = "VehicleWebSocket"

        private const val PREFS_NAME =
            "vehicle_sensor_preferences"

        private const val DEVICE_ID_KEY =
            "device_id"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null

    private var deviceId: String = ""
    private var vehicleId: String? = null
    private var pairingCode: String? = null

    var isConnected: Boolean = false
        private set

    // Backend connection status
    var onConnectionChanged: ((Boolean) -> Unit)? = null

    // Vehicle assigned by backend
    var onVehicleAssigned: ((String) -> Unit)? = null

    // Pairing code received from backend
    var onPairingCodeReceived: ((String) -> Unit)? = null

    // Fleet update callback
    var onFleetUpdate: ((List<VehicleData>) -> Unit)? = null

    init {

        // --------------------------------------------------
        // Load persistent device ID
        // --------------------------------------------------

        val preferences = context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        deviceId = preferences.getString(
            DEVICE_ID_KEY,
            null
        ) ?: UUID.randomUUID().toString().also { newId ->

            preferences.edit()
                .putString(
                    DEVICE_ID_KEY,
                    newId
                )
                .apply()
        }

        Log.d(
            TAG,
            "Persistent Device ID: $deviceId"
        )
    }

    // --------------------------------------------------
    // CONNECT
    // --------------------------------------------------

    fun connect() {

        if (isConnected) {
            Log.d(
                TAG,
                "Already connected — ignoring connect()"
            )
            return
        }

        // --------------------------------------------------
        // Prevent another connection while one is already
        // being established.
        // --------------------------------------------------

        if (webSocket != null) {
            Log.d(
                TAG,
                "WebSocket already exists — waiting for current connection"
            )
            return
        }

        val request = Request.Builder()
            .url(ServerConfig.VEHICLE_WS_URL)
            .build()

        Log.d(
            TAG,
            "CONNECT() called"
        )

        Log.d(
            TAG,
            "Connecting to: ${ServerConfig.VEHICLE_WS_URL}"
        )

        val newSocket = client.newWebSocket(
            request,
            object : WebSocketListener() {

                override fun onOpen(
                    webSocket: WebSocket,
                    response: Response
                ) {

                    Log.d(
                        TAG,
                        "WebSocket CONNECTED — socket=$webSocket"
                    )

                    // This callback belongs to the current socket
                    this@VehicleWebSocketClient.webSocket =
                        webSocket

                    isConnected = true

                    onConnectionChanged?.invoke(true)

                    registerDevice(webSocket)
                }

                override fun onMessage(
                    webSocket: WebSocket,
                    text: String
                ) {

                    Log.d(
                        TAG,
                        "SERVER: $text"
                    )

                    handleServerMessage(text)
                }

                override fun onClosing(
                    webSocket: WebSocket,
                    code: Int,
                    reason: String
                ) {

                    Log.d(
                        TAG,
                        "WebSocket CLOSING — " +
                                "socket=$webSocket " +
                                "code=$code " +
                                "reason=$reason"
                    )
                }

                override fun onClosed(
                    webSocket: WebSocket,
                    code: Int,
                    reason: String
                ) {

                    Log.d(
                        TAG,
                        "WebSocket CLOSED — " +
                                "socket=$webSocket " +
                                "code=$code " +
                                "reason=$reason"
                    )

                    // Only clear state if this is still
                    // the active socket.
                    if (
                        this@VehicleWebSocketClient.webSocket ===
                        webSocket
                    ) {

                        this@VehicleWebSocketClient.webSocket =
                            null

                        isConnected = false

                        onConnectionChanged?.invoke(false)

                        Log.d(
                            TAG,
                            "Active WebSocket cleared"
                        )

                    } else {

                        Log.d(
                            TAG,
                            "Old WebSocket closed — " +
                                    "current connection preserved"
                        )
                    }
                }

                override fun onFailure(
                    webSocket: WebSocket,
                    t: Throwable,
                    response: Response?
                ) {

                    Log.e(
                        TAG,
                        "WebSocket FAILURE — " +
                                "socket=$webSocket " +
                                "message=${t.message}",
                        t
                    )

                    Log.e(
                        TAG,
                        "HTTP response: ${response?.code}"
                    )

                    // Only clear state if this is still
                    // the active socket.
                    if (
                        this@VehicleWebSocketClient.webSocket ===
                        webSocket
                    ) {

                        this@VehicleWebSocketClient.webSocket =
                            null

                        isConnected = false

                        onConnectionChanged?.invoke(false)

                        Log.d(
                            TAG,
                            "Active WebSocket cleared after failure"
                        )

                    } else {

                        Log.d(
                            TAG,
                            "Old WebSocket failed — " +
                                    "current connection preserved"
                        )
                    }
                }
            }
        )

        // Store the socket immediately so another
        // connect() call cannot create another socket
        // while this one is connecting.
        webSocket = newSocket

        Log.d(
            TAG,
            "WebSocket instance created — socket=$newSocket"
        )
    }

    // --------------------------------------------------
    // REGISTER DEVICE
    // --------------------------------------------------

    private fun registerDevice(
        webSocket: WebSocket
    ) {

        val registration = JSONObject().apply {

            put(
                "type",
                "register"
            )

            put(
                "deviceId",
                deviceId
            )
        }

        Log.d(
            TAG,
            "Registering device: $deviceId"
        )

        val success = webSocket.send(
            registration.toString()
        )

        if (!success) {

            Log.w(
                TAG,
                "Failed to send registration"
            )
        }
    }

    // --------------------------------------------------
    // HANDLE SERVER MESSAGE
    // --------------------------------------------------

    private fun handleServerMessage(
        text: String
    ) {

        try {

            val json = JSONObject(text)

            when (json.optString("type")) {

                // --------------------------------------
                // Vehicle registration
                // --------------------------------------

                "registered" -> {

                    val assignedDeviceId =
                        json.optString("deviceId")

                    val assignedVehicleId =
                        json.optString("vehicleId")

                    val assignedPairingCode =
                        json.optString("pairingCode")

                    vehicleId =
                        assignedVehicleId

                    pairingCode =
                        assignedPairingCode

                    Log.d(
                        TAG,
                        "REGISTERED"
                    )

                    Log.d(
                        TAG,
                        "Device ID: $assignedDeviceId"
                    )

                    Log.d(
                        TAG,
                        "Vehicle ID: $assignedVehicleId"
                    )

                    Log.d(
                        TAG,
                        "Pairing Code: $assignedPairingCode"
                    )

                    onVehicleAssigned?.invoke(
                        assignedVehicleId
                    )

                    if (
                        assignedPairingCode.isNotEmpty()
                    ) {

                        onPairingCodeReceived?.invoke(
                            assignedPairingCode
                        )
                    }
                }

                // --------------------------------------
                // Fleet update
                // --------------------------------------

                "fleet_update" -> {

                    handleFleetUpdate(json)
                }

                else -> {

                    Log.d(
                        TAG,
                        "Unknown server message: $text"
                    )
                }
            }

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Failed to parse server message",
                e
            )
        }
    }

    // --------------------------------------------------
    // HANDLE FLEET UPDATE
    // --------------------------------------------------

    private fun handleFleetUpdate(
        json: JSONObject
    ) {

        try {

            val vehiclesArray =
                json.optJSONArray("vehicles")

            if (vehiclesArray == null) {

                Log.w(
                    TAG,
                    "fleet_update contains no vehicles array"
                )

                return
            }

            val vehicles =
                mutableListOf<VehicleData>()

            for (
            i in 0 until vehiclesArray.length()
            ) {

                val vehicle =
                    vehiclesArray.getJSONObject(i)

                val vehicleData =
                    VehicleData(

                        vehicleId =
                            vehicle.optString(
                                "vehicleId"
                            ),

                        lat =
                            if (vehicle.isNull("lat"))
                                null
                            else
                                vehicle.optDouble("lat"),

                        lon =
                            if (vehicle.isNull("lon"))
                                null
                            else
                                vehicle.optDouble("lon"),

                        heading =
                            if (vehicle.isNull("heading"))
                                null
                            else
                                vehicle.optDouble(
                                    "heading"
                                ),

                        accelX =
                            if (vehicle.isNull("accelX"))
                                null
                            else
                                vehicle.optDouble(
                                    "accelX"
                                ),

                        accelY =
                            if (vehicle.isNull("accelY"))
                                null
                            else
                                vehicle.optDouble(
                                    "accelY"
                                ),

                        accelZ =
                            if (vehicle.isNull("accelZ"))
                                null
                            else
                                vehicle.optDouble(
                                    "accelZ"
                                ),

                        gyroX =
                            if (vehicle.isNull("gyroX"))
                                null
                            else
                                vehicle.optDouble(
                                    "gyroX"
                                ),

                        gyroY =
                            if (vehicle.isNull("gyroY"))
                                null
                            else
                                vehicle.optDouble(
                                    "gyroY"
                                ),

                        gyroZ =
                            if (vehicle.isNull("gyroZ"))
                                null
                            else
                                vehicle.optDouble(
                                    "gyroZ"
                                ),

                        connected =
                            vehicle.optBoolean(
                                "connected",
                                false
                            ),

                        lastReceivedAt =
                            if (
                                vehicle.isNull(
                                    "lastReceivedAt"
                                )
                            )
                                null
                            else
                                vehicle.optLong(
                                    "lastReceivedAt"
                                )
                    )

                vehicles.add(vehicleData)
            }

            Log.d(
                TAG,
                "Fleet update received: " +
                        "${vehicles.size} vehicles"
            )

            for (vehicle in vehicles) {

                Log.d(
                    TAG,
                    "Vehicle ${vehicle.vehicleId}: " +
                            "lat=${vehicle.lat}, " +
                            "lon=${vehicle.lon}, " +
                            "heading=${vehicle.heading}, " +
                            "connected=${vehicle.connected}"
                )
            }

            onFleetUpdate?.invoke(
                vehicles
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Failed to parse fleet update",
                e
            )
        }
    }

    // --------------------------------------------------
    // SEND TELEMETRY
    // --------------------------------------------------

    fun sendTelemetry(
        latitude: Double,
        longitude: Double,
        heading: Float,
        accelX: Float,
        accelY: Float,
        accelZ: Float,
        gyroX: Float,
        gyroY: Float,
        gyroZ: Float
    ) {

        val socket =
            webSocket

        // --------------------------------------------------
        // Connection check
        // --------------------------------------------------

        if (
            !isConnected ||
            socket == null
        ) {

            Log.d(
                TAG,
                "TELEMETRY NOT SENT — " +
                        "connected=$isConnected " +
                        "socket=${socket != null}"
            )

            return
        }

        // --------------------------------------------------
        // Build telemetry packet
        // --------------------------------------------------

        val telemetry =
            JSONObject().apply {

                put(
                    "lat",
                    latitude
                )

                put(
                    "lon",
                    longitude
                )

                put(
                    "heading",
                    heading.toDouble()
                )

                put(
                    "accelX",
                    accelX.toDouble()
                )

                put(
                    "accelY",
                    accelY.toDouble()
                )

                put(
                    "accelZ",
                    accelZ.toDouble()
                )

                put(
                    "gyroX",
                    gyroX.toDouble()
                )

                put(
                    "gyroY",
                    gyroY.toDouble()
                )

                put(
                    "gyroZ",
                    gyroZ.toDouble()
                )
            }

        // --------------------------------------------------
        // DEBUG
        // --------------------------------------------------

        Log.d(
            TAG,
            "SENDING TELEMETRY — " +
                    "socket=$socket " +
                    "vehicle=$vehicleId"
        )

        Log.d(
            TAG,
            "TELEMETRY DATA: $telemetry"
        )

        // --------------------------------------------------
        // Send
        // --------------------------------------------------

        val success =
            socket.send(
                telemetry.toString()
            )

        Log.d(
            TAG,
            "Telemetry send result: $success"
        )

        if (!success) {

            Log.w(
                TAG,
                "Failed to send telemetry"
            )
        }
    }

    // --------------------------------------------------
    // DISCONNECT
    // --------------------------------------------------

    fun disconnect() {

        Log.d(
            TAG,
            "Disconnecting WebSocket — socket=$webSocket"
        )

        webSocket?.close(
            1000,
            "App closed"
        )

        webSocket = null

        isConnected = false

        onConnectionChanged?.invoke(false)
    }

    // --------------------------------------------------
    // GETTERS
    // --------------------------------------------------

    fun getDeviceId(): String {
        return deviceId
    }

    fun getVehicleId(): String? {
        return vehicleId
    }

    fun getPairingCode(): String? {
        return pairingCode
    }
}