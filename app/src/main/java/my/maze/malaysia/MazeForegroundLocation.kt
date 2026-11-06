package my.maze.malaysia

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock

internal sealed interface LocationOutcome {
    data class Fix(val point: GeoPoint, val description: String) : LocationOutcome
    data class Unavailable(val message: String) : LocationOutcome
}

/** One user-requested foreground session. No Play Services, polling or background tracking. */
internal class MazeForegroundLocation(private val activity: Activity) {
    private val manager = activity.getSystemService(LocationManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val listeners = mutableListOf<LocationListener>()
    private var generation = 0
    private var deadline: Runnable? = null

    fun cancel(): Boolean {
        val wasActive = deadline != null || listeners.isNotEmpty()
        generation++
        deadline?.let(handler::removeCallbacks)
        deadline = null
        listeners.forEach { runCatching { manager.removeUpdates(it) } }
        listeners.clear()
        return wasActive
    }

    fun request(callback: (LocationOutcome) -> Unit) {
        cancel()
        val token = generation
        val precise = activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = activity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!precise && !coarse) {
            callback(LocationOutcome.Unavailable("Location is optional. Choose a start point instead."))
            return
        }
        val enabled = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }.toSet()
        val providers = preferredProviders(precise, enabled)
        if (providers.isEmpty()) {
            callback(LocationOutcome.Unavailable(if (!precise && "gps" in enabled)
                "Approximate location has no available network provider. You can choose a start point, or enable precise location in Android settings."
            else "Location services are off or unavailable. Enable them in Android settings or choose a start point."))
            return
        }
        var best: FixCandidate? = null
        fun finish(fix: FixCandidate?) {
            if (token != generation) return
            cancel()
            callback(if (fix != null) LocationOutcome.Fix(fix.point, fixDescription(fix, precise))
            else LocationOutcome.Unavailable("No recent, usable location fix. Try outdoors with a clear sky view, or choose a start point."))
        }
        // GPS may win with a good fix; a coarse fix is labelled and never called a doorway.
        fun accept(location: Location) {
            if (token != generation) return
            val point = runCatching { GeoPoint(location.latitude, location.longitude) }.getOrNull() ?: return
            val fix = FixCandidate(point, if (location.hasAccuracy()) location.accuracy else null,
                location.elapsedRealtimeNanos, location.provider ?: "unknown", location.isFromMockProvider)
            if (!usableFix(fix, SystemClock.elapsedRealtimeNanos(), precise)) return
            if (best == null || fix.accuracyMeters!! < best!!.accuracyMeters!!) best = fix
            if (!precise || fix.accuracyMeters!! <= 50f) finish(best)
        }
        deadline = Runnable { finish(best) }.also { handler.postDelayed(it, 20_000) }
        for (provider in providers) {
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) = accept(location)
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
                @Deprecated("Deprecated by Android")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            }
            try {
                // Updates are bounded by the 20 s session and cancelled at the first good fix/onStop.
                manager.requestLocationUpdates(provider, 1000L, 0f, listener, Looper.getMainLooper())
                listeners.add(listener)
            } catch (_: SecurityException) {
                // Permission may have been revoked while the request was starting.
            } catch (_: IllegalArgumentException) {
                // Provider disappeared. Another selected provider may still work.
            }
        }
        if (listeners.isEmpty()) finish(null)
    }
}
