package com.udacity.project4.locationreminders.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent

/**
 * Triggered by the Geofence.  Since we can have many Geofences at once, we pull the request
 * ID from the first Geofence, and locate it within the cached data in our Room DB
 *
 * Or users can add the reminders and then close the app, So our app has to run in the background
 * and handle the geofencing in the background.
 * To do that you can use https://developer.android.com/reference/android/support/v4/app/JobIntentService to do that.
 *
 */

private const val TAG = "GeofenceBroadcastReceiver"
class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {

        Log.d("Broadcast Receiver", "something received")
        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        if (geofencingEvent != null) {
            if (geofencingEvent.hasError()) {
                val errorMessage = GeofenceStatusCodes
                    .getStatusCodeString(geofencingEvent.errorCode)
                Log.e(TAG, errorMessage)
                return
            }

            // Get the transition type.
            val geofenceTransition = geofencingEvent.geofenceTransition
            Log.d("Geofence event non null", geofenceTransition.toString())

            // Test that the reported transition was of interest.
            if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER)
            {
                //use work manager to enqueue work
                //GeofenceWorker.GeofenceWorkerManager.enqueueWork(context, intent)

                //use JobIntentService
                GeofenceTransitionsJobIntentService.enqueueWork(context, intent)


                Log.v(TAG,"Geofence Entered")
                val fenceId = when {
                    geofencingEvent.triggeringGeofences!!.isNotEmpty() ->
                        geofencingEvent.triggeringGeofences!![0].requestId
                    else -> {
                        Log.e(TAG, "No Geofence Trigger Found! Abort mission!")
                        return
                    }
                }

                // Send notification and log the transition details.
            } else {
                // Log the error.
                Log.e(
                    TAG, "invalid transition type"
                )
            }
        }else{
            Log.d("GeofencingEvent", "geofencing event null!!")
        }
    }

    private fun getGeofenceTransitionDetails(
        context: Context,
        geofenceTransition: Int,
        triggeringGeofences: List<Geofence>
    ): String {

        val geofenceTransitionString = getTransitionString(geofenceTransition)

        // Get the IDs of each geofence that was triggered.
        val triggeringGeofencesIds = triggeringGeofences.mapNotNull { it.requestId }
        val triggeringGeofencesIdsString = triggeringGeofencesIds.joinToString(", ")

        return "$geofenceTransitionString: $triggeringGeofencesIdsString"
    }

    private fun getTransitionString(transitionType: Int): String {
        return when (transitionType) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> "transition - enter"
            Geofence.GEOFENCE_TRANSITION_EXIT -> "transition - exit"
            else -> "unknown transition"
        }
    }
}