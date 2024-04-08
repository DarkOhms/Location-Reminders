package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SaveReminderFragment : BaseFragment() {

    // Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding
    private lateinit var map: GoogleMap
    private lateinit var reminderToSave : ReminderDataItem

    var geofencingClient: GeofencingClient? = null
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        PendingIntent.getBroadcast(requireContext(), 8675309, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE )
    }


    private lateinit var currentLocation: Location

    override fun onAttach(context: Context) {
        super.onAttach(context)
        geofencingClient = LocationServices.getGeofencingClient(requireActivity())
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val layoutId = R.layout.fragment_save_reminder
        binding = DataBindingUtil.inflate(inflater, layoutId, container, false)

        setDisplayHomeAsUpEnabled(true)
        binding.viewModel = _viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            // Navigate to another fragment to get the user location
            val directions = SaveReminderFragmentDirections
                .actionSaveReminderFragmentToSelectLocationFragment()
            _viewModel.navigationCommand.value = NavigationCommand.To(directions)
        }

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value

            reminderToSave = ReminderDataItem(title, description, location, latitude, longitude)

            if(_viewModel.validateAndSaveReminder(reminderToSave))
                addGeofence()

            // TODO: use the user entered reminder details to:
            //  1) add a geofencing request
            //  2) save the reminder to the local db


        }
        //checking 2 way binding
        _viewModel.reminderTitle.observe(viewLifecycleOwner) { title ->
            Log.d("SaveReminderFragment", "Title updated: $title")
        }

        //location setup
        val locationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        @SuppressLint("MissingPermission")
        if(isPermissionGranted()){
            val locationResultTask = locationClient.lastLocation
            locationResultTask.addOnSuccessListener { location:Location ->
                currentLocation = location
            }
        }

        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync { googleMap ->
            map = googleMap
            if (isPermissionGranted()) {
                enableMyLocation()
            }
            map.uiSettings.isZoomControlsEnabled = true
            var latLng = LatLng(-34.0, 151.0)
            enableMyLocation()
            @SuppressLint("MissingPermission")
            if (_viewModel.latitude.value != null) {

                val lat: Double = _viewModel.latitude.value!!
                val long: Double = _viewModel.longitude.value!!
                latLng = LatLng(lat, long)
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13f))
            }else {
                val locationResultTask = locationClient.lastLocation
                locationResultTask.addOnSuccessListener { location: Location ->
                    currentLocation = location
                    latLng = LatLng(location.latitude, location.longitude)
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13f))
                }
            }
        }

    }

    private fun isPermissionGranted(): Boolean {

        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) === PackageManager.PERMISSION_GRANTED
    }

    private fun enableMyLocation() {
        @SuppressLint("MissingPermission")
            map.isMyLocationEnabled = isPermissionGranted()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

    @SuppressLint("MissingPermission")
    fun addGeofence(){
        val geofence = Geofence.Builder()
            .setRequestId(reminderToSave.id)
            //possible change the radius in future implementation
            .setCircularRegion(reminderToSave.latitude!!, reminderToSave.longitude!!, 100f)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .build()

        val geofencingRequest = GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofence(geofence)
        }.build()

        geofencingClient?.addGeofences(geofencingRequest, geofencePendingIntent)?.run {
            addOnSuccessListener {
                // Geofences added
                Log.d("GeofenceRequest", "success!!! at " + reminderToSave.latitude)
            }
            addOnFailureListener {
                // Failed to add geofences
                Log.d("GeofenceRequest", "failure :( " + it.message + it.localizedMessage)
            }
        }


    }

}