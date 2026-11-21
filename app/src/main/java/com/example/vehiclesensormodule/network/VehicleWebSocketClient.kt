package com.example.vehiclesensormodule.network

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject

/**
 * How long to wait before reconnect attempt number [attempt], counting from
 * 0: 1s, 2s, 4s, doubling up to [maxMs].
 */
internal fun reconnectDelayMs(attempt: Int, maxMs: Long): Long = minOf(1000L shl attempt.coerceIn(0, 5), maxMs)

class VehicleWebSocketClient(context: Context) {

    companion object {
        private const val TAG = "VehicleWebSocket"

        // The server restarts on every deploy and is back within a minute,
        // so a short longest wait means sharing resumes soon after
        private const val MAX_RECONNECT_DELAY_MS = 10_000L

        // Sent by the backend. Reconnecting would not help with these.
        private const val CLOSE_INVALID_DEVICE = 4001
        private const val CLOSE_REPLACED = 4002

        // The login isn't valid for this phone, so it must log in again
        private const val CLOSE_NOT_AUTHORIZED = 4004
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var webSocket: WebSocket? = null

    // True from connect() until disconnect()
    @Volatile
    private var shouldStayConnected = false

    private var reconnectAttempt = 0

    private val reconnectHandler =
        Handler(Looper.getMainLooper())

    private val reconnectRunnable = Runnable {
        connect()
    }

    private var deviceId: String = ""

    @Volatile
    var isConnected: Boolean = false
        private set

    // Backend connection status
    var onConnectionChanged: ((Boolean) -> Unit)? = null

    // Proves to the backend that this phone may act as this device
    var authToken: String? = null

    // The backend refused this login
    var onAuthorizationRejected: (() -> Unit)? = null

    // Vehicle assigned by backend
    var onVehicleAssigned: ((String) -> Unit)? = null

    // The ghat road the car is on, for Mountain Mode, or null when off one
    var onMountainChanged: ((String?) -> Unit)? = null

    init {

        deviceId = DeviceIdentity.get(context)
    }

    // --------------------------------------------------
    // CONNECT
    // --------------------------------------------------

    fun connect() {
        shouldStayConnected = true

        reconnectHandler.removeCallbacks(
            reconnectRunnable
        )

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
            .apply {
                authToken?.let {
                    header("Authorization", "Bearer $it")
                }
            }
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

                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.i(
                        TAG,
                        "WebSocket CONNECTED — socket=$webSocket"
                    )

                    // This callback belongs to the current socket
                    this@VehicleWebSocketClient.webSocket =
                        webSocket

                    isConnected = true

                    reconnectHandler.post {
                        reconnectAttempt = 0
                    }

                    onConnectionChanged?.invoke(true)

                    registerDevice(webSocket)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    handleServerMessage(text)
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d(
                        TAG,
                        "WebSocket CLOSING — " +
                            "socket=$webSocket " +
                            "code=$code " +
                            "reason=$reason"
                    )

                    // onClosed only fires once this side
                    // has also closed the connection.
                    webSocket.close(
                        1000,
                        null
                    )
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Log.i(
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

                        if (code == CLOSE_NOT_AUTHORIZED) {
                            reconnectHandler.post {
                                onAuthorizationRejected?.invoke()
                            }
                        } else if (
                            code != CLOSE_INVALID_DEVICE &&
                            code != CLOSE_REPLACED
                        ) {
                            scheduleReconnect()
                        }
                    } else {
                        Log.d(
                            TAG,
                            "Old WebSocket closed — " +
                                "current connection preserved"
                        )
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
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

                        scheduleReconnect()
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
    // RECONNECT
    //
    // Waits 1s, 2s, 4s ... up to 10s between attempts.
    // --------------------------------------------------

    private fun scheduleReconnect() {
        reconnectHandler.post {
            if (!shouldStayConnected) {
                return@post
            }

            val delayMs = reconnectDelayMs(reconnectAttempt, MAX_RECONNECT_DELAY_MS)

            reconnectAttempt++

            Log.i(
                TAG,
                "Reconnecting in ${delayMs}ms " +
                    "(attempt $reconnectAttempt)"
            )

            reconnectHandler.removeCallbacks(
                reconnectRunnable
            )

            reconnectHandler.postDelayed(
                reconnectRunnable,
                delayMs
            )
        }
    }

    // --------------------------------------------------
    // NETWORK RETURNED OR CHANGED
    //
    // Skips the wait between attempts. After a change, such as
    // Wi-Fi to mobile data, the old connection can't be trusted:
    // it could otherwise take up to a minute to notice it's dead.
    // --------------------------------------------------

    fun onNetworkAvailable(networkChanged: Boolean) {
        reconnectHandler.post {
            if (!shouldStayConnected) {
                return@post
            }

            reconnectAttempt = 0

            val socket = webSocket

            if (socket == null) {
                connect()
            } else if (networkChanged) {
                // Fails the socket, which schedules a quick reconnect
                socket.cancel()
            }
        }
    }

    // --------------------------------------------------
    // REGISTER DEVICE
    // --------------------------------------------------

    private fun registerDevice(webSocket: WebSocket) {
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

    private fun handleServerMessage(text: String) {
        try {
            val json = JSONObject(text)

            when (json.optString("type")) {
                // --------------------------------------
                // Vehicle registration
                // --------------------------------------

                "registered" -> {
                    val assignedVehicleId =
                        json.optString("vehicleId")

                    Log.d(
                        TAG,
                        "REGISTERED"
                    )

                    Log.d(
                        TAG,
                        "Vehicle ID: $assignedVehicleId"
                    )

                    onVehicleAssigned?.invoke(
                        assignedVehicleId
                    )
                }

                // --------------------------------------
                // On or off a ghat road
                // --------------------------------------

                "mountain" -> {
                    onMountainChanged?.invoke(
                        if (json.isNull("roadName")) null else json.optString("roadName")
                    )
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
        gyroZ: Float,
        speed: Float?,
        bearing: Float?,
        accuracy: Float?
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

                speed?.let {
                    put("speed", it.toDouble())
                }

                bearing?.let {
                    put("bearing", it.toDouble())
                }

                accuracy?.let {
                    put("accuracy", it.toDouble())
                }
            }

        // --------------------------------------------------
        // Send
        // --------------------------------------------------

        val success =
            socket.send(
                telemetry.toString()
            )

        if (!success) {
            Log.w(
                TAG,
                "Failed to send telemetry"
            )
        }
    }

    // --------------------------------------------------
    // SEND HEADING
    //
    // Only the compass, so a turn shows on the screen
    // without waiting for the next GPS update.
    // --------------------------------------------------

    fun sendHeading(heading: Float): Boolean {
        val socket = webSocket

        if (!isConnected || socket == null) {
            return false
        }

        val message = JSONObject().apply {
            put("type", "heading")
            put("heading", heading.toDouble())
        }

        return socket.send(message.toString())
    }

    // --------------------------------------------------
    // DISCONNECT
    // --------------------------------------------------

    fun disconnect() {
        Log.d(
            TAG,
            "Disconnecting WebSocket — socket=$webSocket"
        )

        shouldStayConnected = false

        reconnectHandler.removeCallbacks(
            reconnectRunnable
        )

        webSocket?.close(
            1000,
            "App closed"
        )

        webSocket = null

        isConnected = false

        onConnectionChanged?.invoke(false)
    }
}
