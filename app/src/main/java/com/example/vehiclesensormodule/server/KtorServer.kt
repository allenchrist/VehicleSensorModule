package com.example.vehiclesensormodule.server

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration

class KtorServer {

    private var server: ApplicationEngine? = null

    private val clients = mutableSetOf<DefaultWebSocketServerSession>()

    fun start() {

        server = embeddedServer(
            CIO,
            port = 8080
        ) {

            install(WebSockets) {
                pingPeriod = Duration.parse("PT15S")
                timeout = Duration.parse("PT15S")
                maxFrameSize = Long.MAX_VALUE
                masking = false
            }

            routing {

                get("/") {

                    call.respondText(
                        """
<!DOCTYPE html>
<html>

<head>

<title>Vehicle Sensor Server</title>

<style>

body{
    font-family:Arial;
    margin:40px;
    background:#f5f5f5;
}

.card{
    background:white;
    padding:20px;
    border-radius:10px;
    box-shadow:0 2px 8px rgba(0,0,0,.2);
}

table{
    border-collapse:collapse;
    width:100%;
}

td{
    padding:10px;
    border-bottom:1px solid #ddd;
}

h1{
    color:#1976D2;
}

</style>

</head>

<body>

<div class="card">

<h1>Vehicle Sensor Server</h1>

<table>

<tr>
<td><b>Status</b></td>
<td id="status">Connecting...</td>
</tr>

<tr>
<td><b>Latitude</b></td>
<td id="lat">Waiting...</td>
</tr>

<tr>
<td><b>Longitude</b></td>
<td id="lon">Waiting...</td>
</tr>

<tr>
<td><b>Heading</b></td>
<td id="heading">Waiting...</td>
</tr>

<tr>
<td><b>Accelerometer</b></td>
<td id="accel">Waiting...</td>
</tr>

<tr>
<td><b>Gyroscope</b></td>
<td id="gyro">Waiting...</td>
</tr>

</table>

</div>

<script>

const socket = new WebSocket(
    "ws://" + window.location.host + "/ws"
);

socket.onopen = function(){

    console.log("CONNECTED");

    document.getElementById("status").innerHTML =
        "Connected";

};

socket.onmessage = function(event){

    console.log("Received:", event.data);

    const data = JSON.parse(event.data);

    document.getElementById("lat").innerHTML =
        data.lat.toFixed(6);

    document.getElementById("lon").innerHTML =
        data.lon.toFixed(6);

    document.getElementById("heading").innerHTML =
        data.heading.toFixed(1) + "°";

    document.getElementById("accel").innerHTML =
        "X : " + data.accelX.toFixed(2) +
        " | Y : " + data.accelY.toFixed(2) +
        " | Z : " + data.accelZ.toFixed(2);

    document.getElementById("gyro").innerHTML =
        "X : " + data.gyroX.toFixed(2) +
        " | Y : " + data.gyroY.toFixed(2) +
        " | Z : " + data.gyroZ.toFixed(2);

};

socket.onclose = function(){

    console.log("DISCONNECTED");

    document.getElementById("status").innerHTML =
        "Disconnected";

};

socket.onerror = function(error){

    console.log(error);

};

</script>

</body>

</html>
                        """.trimIndent(),
                        ContentType.Text.Html
                    )

                }

                webSocket("/ws") {

                    clients.add(this)

                    println("Browser Connected!")

                    try {

                        while (true) {
                            delay(1000)
                        }

                    } finally {

                        clients.remove(this)

                        println("Browser Disconnected")

                    }

                }

            }

        }

        server?.start(wait = false)

    }

    fun broadcast(message: String) {

        GlobalScope.launch {

            clients.forEach { client ->

                try {

                    client.send(Frame.Text(message))

                } catch (e: Exception) {

                    e.printStackTrace()

                }

            }

        }

    }

}