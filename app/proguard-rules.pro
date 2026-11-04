# The server's JSON is matched to these classes by their field names,
# so shrinking must not rename or remove them.
-keep class com.example.vehiclesensormodule.network.LoginRequest { *; }
-keep class com.example.vehiclesensormodule.network.LoginResponse { *; }
-keep class com.example.vehiclesensormodule.network.RegisterRequest { *; }
-keep class com.example.vehiclesensormodule.network.RegisterResponse { *; }
-keep class com.example.vehiclesensormodule.network.DeviceAssociationRequest { *; }
-keep class com.example.vehiclesensormodule.network.DeviceAssociationResponse { *; }
-keep class com.example.vehiclesensormodule.network.User { *; }

# No routine debug logs in the release version
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
}
