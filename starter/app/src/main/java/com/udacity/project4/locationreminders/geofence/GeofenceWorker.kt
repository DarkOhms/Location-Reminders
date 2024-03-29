package com.udacity.project4.locationreminders.geofence

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.udacity.project4.locationreminders.data.dto.Result
import org.koin.core.component.KoinComponent

class GeofenceWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams), KoinComponent {

    private val TAG = "GeofenceWorker"
    override fun doWork(): Result {
        // Register the GeofenceBroadcastReceiver here

        // Indicate success (or retry if registration fails)
        return Result.success()
    }


}
