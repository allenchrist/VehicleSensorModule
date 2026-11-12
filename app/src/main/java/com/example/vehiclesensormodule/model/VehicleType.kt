package com.example.vehiclesensormodule.model

import androidx.annotation.DrawableRes
import com.example.vehiclesensormodule.R

/**
 * The ids match the backend's list in utils/vehicleTypes.js.
 */
enum class VehicleType(val id: String, val label: String, @DrawableRes val icon: Int) {
    Car("car", "Car", R.drawable.ic_car),
    Truck("truck", "Truck", R.drawable.ic_vehicle_truck),
    Lorry("lorry", "Lorry", R.drawable.ic_vehicle_lorry),
    Bus("bus", "Bus", R.drawable.ic_vehicle_bus),
    Auto("auto", "Auto", R.drawable.ic_vehicle_auto),

    // Always gets right of way in a narrow road
    Ambulance("ambulance", "Ambulance", R.drawable.ic_vehicle_ambulance);

    companion object {
        // A newer type from the server shouldn't break an older app
        fun fromId(id: String?): VehicleType = entries.firstOrNull { it.id == id } ?: Car
    }
}
