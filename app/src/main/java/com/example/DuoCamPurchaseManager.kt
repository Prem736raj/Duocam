package com.example

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.util.Log

enum class SimulatedPurchaseState {
    ACTIVE_INDIVIDUAL,
    FAMILY_SHARED,
    REFUNDED,
    EXPIRED,
    NO_PURCHASE
}

object DuoCamPurchaseManager {
    private const val TAG = "DuoCamPurchaseManager"
    private const val PREFS_NAME = "duocam_prefs"
    private const val KEY_IS_PRO = "is_pro_upgraded"
    private const val KEY_SIMULATED_STATE = "purchase_simulated_state"
    private const val KEY_SIMULATE_NO_INTERNET = "purchase_simulate_no_internet"

    /**
     * Helper to verify if physical network connectivity is active.
     */
    fun isNetworkConnected(context: Context): Boolean {
        // Also respect our simulated network toggle for developer testing
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_SIMULATE_NO_INTERNET, false)) {
            return false
        }

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Get target simulation mode.
     */
    fun getSimulatedState(context: Context): SimulatedPurchaseState {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stateName = prefs.getString(KEY_SIMULATED_STATE, SimulatedPurchaseState.ACTIVE_INDIVIDUAL.name)
        return try {
            SimulatedPurchaseState.valueOf(stateName ?: SimulatedPurchaseState.ACTIVE_INDIVIDUAL.name)
        } catch (e: Exception) {
            SimulatedPurchaseState.ACTIVE_INDIVIDUAL
        }
    }

    /**
     * Update target simulation mode.
     */
    fun setSimulatedState(context: Context, state: SimulatedPurchaseState) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SIMULATED_STATE, state.name).apply()
        Log.d(TAG, "Simulated purchase state updated to: $state")
    }

    /**
     * Update simulated internet connection availability.
     */
    fun setSimulateNoInternet(context: Context, simulateNoInternet: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SIMULATE_NO_INTERNET, simulateNoInternet).apply()
    }

    /**
     * Get simulated internet configuration.
     */
    fun getSimulateNoInternet(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SIMULATE_NO_INTERNET, false)
    }

    /**
     * Simulates Google Play Billing auto-verification on app launch.
     * Checks if a valid license exists under the user's Google Play accounts or Google Play Family Library.
     */
    fun autoVerifyOnLaunch(context: Context, callback: (isPro: Boolean, message: String) -> Unit) {
        Handler(Looper.getMainLooper()).postDelayed({
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val currentState = getSimulatedState(context)

            if (!isNetworkConnected(context)) {
                // Return cached SharedPreferences billing info because network is down
                val cachedPro = prefs.getBoolean(KEY_IS_PRO, false)
                Log.d(TAG, "No internet on launch. Relying on local cached license: $cachedPro")
                callback(cachedPro, "No internet connection. Loaded local cached license.")
                return@postDelayed
            }

            // Internet is available: perform active query against Google Play API services
            when (currentState) {
                SimulatedPurchaseState.ACTIVE_INDIVIDUAL -> {
                    Log.d(TAG, "Google Play API: Active personal license found.")
                    prefs.edit().putBoolean(KEY_IS_PRO, true).apply()
                    callback(true, "Auto-Verified: DuoCam Pro unlocked via Google Play Licensing.")
                }
                SimulatedPurchaseState.FAMILY_SHARED -> {
                    Log.d(TAG, "Google Play API: Active family-shared library license found.")
                    prefs.edit().putBoolean(KEY_IS_PRO, true).apply()
                    callback(true, "Auto-Verified: DuoCam Pro unlocked via Google Play Family Library sharing.")
                }
                SimulatedPurchaseState.REFUNDED -> {
                    Log.w(TAG, "Google Play API: Purchase was previously refunded.")
                    prefs.edit().putBoolean(KEY_IS_PRO, false).apply()
                    callback(false, "Verification notice: Your previous Pro purchase was refunded. App reverted to free tier.")
                }
                SimulatedPurchaseState.EXPIRED -> {
                    Log.w(TAG, "Google Play API: Subscription or promotional license expired.")
                    prefs.edit().putBoolean(KEY_IS_PRO, false).apply()
                    callback(false, "Verification notice: Your promotional pro access has expired.")
                }
                SimulatedPurchaseState.NO_PURCHASE -> {
                    Log.d(TAG, "Google Play API: No active licenses found for this account.")
                    // Make sure we clear it if the play store says none (unless user has some offline bypass)
                    prefs.edit().putBoolean(KEY_IS_PRO, false).apply()
                    callback(false, "DuoCam Pro not possessed. Standard free edition active.")
                }
            }
        }, 1500) // Aesthetic delay simulating network call
    }

    /**
     * Restore purchases on-demand (e.g. after cleanly re-installing).
     * Simulates Google Play Billing queries of active and family-shared purchases.
     */
    fun restorePurchase(context: Context, callback: (success: Boolean, message: String, isPro: Boolean) -> Unit) {
        Handler(Looper.getMainLooper()).postDelayed({
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val currentState = getSimulatedState(context)

            if (!isNetworkConnected(context)) {
                Log.w(TAG, "Cannot restore purchase without internet.")
                callback(
                    false,
                    "Connection Error: Accessing Google Play server failed. Please check your internet connection and try again.",
                    prefs.getBoolean(KEY_IS_PRO, false)
                )
                return@postDelayed
            }

            // Network is active: query Google Play Licensing standard & family pools
            when (currentState) {
                SimulatedPurchaseState.ACTIVE_INDIVIDUAL -> {
                    Log.i(TAG, "Restore successful: Personal license fetched.")
                    prefs.edit().putBoolean(KEY_IS_PRO, true).apply()
                    callback(true, "DuoCam Pro Restored! Personal lifetime purchase retrieved from Google Play Billing.", true)
                }
                SimulatedPurchaseState.FAMILY_SHARED -> {
                    Log.i(TAG, "Restore successful: Family-shared license fetched.")
                    prefs.edit().putBoolean(KEY_IS_PRO, true).apply()
                    callback(true, "DuoCam Pro Restored! Family-shared lifetime license retrieved via Google Play Family Library.", true)
                }
                SimulatedPurchaseState.REFUNDED -> {
                    Log.w(TAG, "Restore failed: Purchase was refunded.")
                    prefs.edit().putBoolean(KEY_IS_PRO, false).apply()
                    callback(false, "Restore Failed: Your purchase history indicates this item was refunded. Please repurchase to upgrade.", false)
                }
                SimulatedPurchaseState.EXPIRED -> {
                    Log.w(TAG, "Restore failed: License expired.")
                    prefs.edit().putBoolean(KEY_IS_PRO, false).apply()
                    callback(false, "Restore Failed: Your professional promotional access or subscription license has already expired.", false)
                }
                SimulatedPurchaseState.NO_PURCHASE -> {
                    Log.i(TAG, "Restore failed: No purchase history.")
                    prefs.edit().putBoolean(KEY_IS_PRO, false).apply()
                    callback(false, "Restore Failed: No previous purchases for DuoCam Pro found on this Google Play account.", false)
                }
            }
        }, 1800) // Aesthetic delay for professional network feedback
    }
}
