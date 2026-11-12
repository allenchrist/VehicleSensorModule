# Connected Vehicle (Android app)

The phone half of [Narrow Road Navigation](https://github.com/allenchrist/narrow-road-navigation), a college project that helps drivers pass safely through narrow roads.

We can't modify a real car, so this app stands in for its connectivity module. It sends the car's position to the server, which compares nearby cars. **Alerts appear on the web dashboard**, which stands in for the car's screen, not on the phone.

## Install it

You need [Android Studio](https://developer.android.com/studio) and an Android phone running Android 7.0 or newer with USB debugging on.

1. Open this folder in Android Studio and wait for it to finish syncing.
2. Connect the phone and press **Run**.

To build from a terminal, run `gradlew.bat :app:assembleDebug` on Windows or `sh ./gradlew :app:assembleDebug` elsewhere. Android Studio's own Java works if you set `JAVA_HOME` to its `jbr` folder.

## Build a version to share

The version Android Studio's **Run** installs is for testing. To make one you can send to other phones, you need a release key. Create it once, outside any folder you share:

```
keytool -genkeypair -v -keystore release.jks -alias connected-vehicle -keyalg RSA -keysize 2048 -validity 10000
```

Then create `keystore.properties` in this folder. Git ignores this file and `.jks` files, so neither is uploaded:

```
storeFile=release.jks
storePassword=your password
keyAlias=connected-vehicle
keyPassword=your password
```

Build it with `gradlew.bat :app:assembleRelease`. The file is at `app/build/outputs/apk/release/app-release.apk`.

Keep the key and its password safe. A phone can only update the app with the same key, so a lost key means uninstalling and installing again. For the same reason, a phone with the testing version must uninstall it first, which logs it out.

## Use it

1. Create an account and choose what you drive: car, truck, lorry, bus, auto or priority vehicle (any emergency vehicle, which always gets right of way). Enter the code we email you, then log in. You can change the vehicle later on the main screen.
2. Tap **Connect**. Allow location, and allow notifications so you can see the **Stop sharing** button.
3. If the app shows **Battery limits are on** or **Battery Saver is on**, follow its button. These can delay your position while the screen is locked.
4. Open the dashboard website and log in with the same account.

Each phone needs its own account. Logging in on a new phone moves the account to that phone.

**Quick Settings tile:** add **Share position** to your phone's Quick Settings (pull down twice, then edit). Tapping it when off opens the app and connects. Tapping it when on stops sharing. Android only lets an app start location sharing while it's on screen, which is why turning it on opens the app.

**Mark a narrow road:** if a narrow road is missing, tap **Start recording** on the main screen while connected, drive through it, then tap **Finish**. Name it and send it. The app sends a shape a few metres either side of your track, and an admin reviews it on the website before it's used. Only accurate GPS positions are recorded, and it needs at least 15 m.

If you forget your password, tap **Forgot password?** on the login screen to get a code by email. To delete your account, tap the car icon or your name at the top of the main screen, then **Delete account**. It asks for your password first.

## When it shares your position

| What you do | Sharing |
|---|---|
| Lock the screen | Continues |
| Press Home or switch to another app | Continues |
| Tap **Disconnect** | Stops |
| Close the app from recent apps | Stops |
| Tap **Stop sharing** on the notification | Stops |
| Log out | Stops |
| Open the app again after it stopped | Stays off until you tap **Connect** |

After restarting the phone, open the app and tap **Connect** to start sharing again.

## How it's organised

```
app/src/main/java/com/example/vehiclesensormodule/
  MainActivity.kt      screens and permission requests
  service/             the background service: GPS, sensors and the connection
  sensors/             GPS, accelerometer, gyroscope and compass
  network/             login requests and the live connection to the server
  auth/                login and signup screens, and the encrypted login token
  screen/, ui/         the main screen, shared components and theme
```

The background service does the real work, so sharing carries on while the screen is off. The screens only display what it reports.

## Server address

The app connects to the address in [ServerConfig.kt](app/src/main/java/com/example/vehiclesensormodule/network/ServerConfig.kt). Change it there if your server is elsewhere.

The app only allows encrypted connections, so plain `http://` and `ws://` addresses, such as a laptop on the same Wi-Fi, won't work.

## Permissions

| Permission | Why |
|---|---|
| Location (fine and coarse) | The car's position |
| Foreground service, and foreground service location | Sharing with the screen off |
| Notifications | The notification with the **Stop sharing** button |
| Wake lock | Keeps updates going while the screen is off |
| Internet, and network state | Reaching the server, and reconnecting when the network returns |
| Ignore battery optimisations | Lets the app ask to run without battery limits |

## Privacy and security

- The login token is encrypted with a key held in Android's secure storage. On the few phones where that storage is faulty, it's stored unencrypted so the driver can still log in.
- Logins and the device ID are left out of Android backups and phone-to-phone transfers.
- Passwords aren't stored on the phone.

## Run the checks

```
gradlew.bat :app:ktlintCheck :app:testDebugUnitTest :app:lintDebug
```

`ktlintCheck` checks the code style, set in `.editorconfig`, and `gradlew.bat :app:ktlintFormat` fixes most of it. The unit tests cover login encryption, the compass heading and its direction names, the wait between reconnect attempts, and turning a recorded track into a road shape.

GitHub also builds the app and runs these on every push. Results are in the repository's **Actions** tab. Android's secure storage and background behaviour can only be tested on a real phone.

## Common problems

| Problem | What to check |
|---|---|
| The email code doesn't arrive | Check your spam folder and that the address is right. You can ask for a new code after a minute |
| Stuck on **Connecting…** | The phone's internet connection, and whether the server is awake. Free hosts can take a little while |
| Stuck on **Finding your position** | Go outdoors. GPS needs a clear view of the sky |
| Car updates lag with the screen locked | Turn Battery Saver off, and remove the battery limits. Mobile data is also more dependable than Wi-Fi |
| No **Stop sharing** button | Allow notifications for this app in the phone's settings |
| Logged out unexpectedly | The account was probably used on another phone, or the 30-day login expired. Log in again |
| Stopped sharing after closing the app | Closing it from recent apps stops sharing. Open the app and tap **Connect** |
