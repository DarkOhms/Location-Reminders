package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.snackbar.Snackbar
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
    private lateinit var mapFragment: SupportMapFragment
    private lateinit var reminderToSave : ReminderDataItem
    private lateinit var locationClient: FusedLocationProviderClient

    var geofencingClient: GeofencingClient? = null
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        PendingIntent.getBroadcast(requireContext(), 8675309, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE )
    }


    private lateinit var currentLocation: Location

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (!isBackgroundPermissionGranted()) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        requireActivity(),
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    )
                ) {
                    Log.d("SelectLocationFragment", "shouldShowPermissionRational returned true")
                    explainBackgroundPermission(requireActivity())
                }else{
                    Log.d("SaveReminderFragment", "shouldShowPermissionRational returned false")
                }
            }
        }
    }
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
        mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
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

            if(_viewModel.validateAndSaveReminder(reminderToSave)){
                //check api level for background permissions flow
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (!isBackgroundPermissionGranted()) {
                        requestBackgroundPermissionAndAddGeofence()
                    }else{
                        addGeofence()
                    }
                }else{
                    addGeofence()
                }
            }

            // TODO: use the user entered reminder details to:
            //  1) add a geofencing request
            //  2) save the reminder to the local db


        }

        @SuppressLint("MissingPermission")
        if(isPermissionGranted()){
            checkDeviceLocationSettings()
        }

    }

    private fun isPermissionGranted(): Boolean {

        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) === PackageManager.PERMISSION_GRANTED
    }

    private fun isBackgroundPermissionGranted() : Boolean {

        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun explainBackgroundPermission(activity: Activity) {
        val alertDialog = AlertDialog.Builder(activity)
            .setTitle(R.string.permission_required)
            .setMessage(getString(R.string.background_permission_rationale))
            .setNeutralButton(R.string.ok) { dialog, _ ->
                dialog.dismiss()
            }
            .create()


        alertDialog.show()
    }

    private fun enableMyLocation() {
        @SuppressLint("MissingPermission")
        map.isMyLocationEnabled = isPermissionGranted()
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

    @SuppressLint("MissingPermission")
    private fun checkDeviceLocationSettings(resolve:Boolean = true){
        Log.d("checkLocationSettings", "called")
        locationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        val locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireActivity())
            settingsClient.checkLocationSettings(builder.build())
                .addOnFailureListener { exception ->
                    if (exception is ResolvableApiException && resolve) {
                        try {
                            exception.startResolutionForResult(
                                requireActivity(),
                                REQUEST_TURN_DEVICE_LOCATION_ON
                            )
                        } catch (sendEx: IntentSender.SendIntentException) {
                            Log.d(
                                "SaveReminderFragment",
                                "Error getting location settings resolution: " + sendEx.message
                            )
                        }
                    } else {
                        Snackbar.make(
                            binding.root,
                            R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                        ).setAction(android.R.string.ok) {
                            checkDeviceLocationSettings()
                        }.show()
                    }
                }
            .addOnCompleteListener {
                if ( it.isSuccessful ) {
                    Log.d("checkLocationSettings", "locationSettingsResponseTask complete")
                    locationClient.lastLocation.addOnSuccessListener { location ->
                        currentLocation = location
                        setupMap()
                    }
                }
            }
        }


    fun setupMap() {
        Log.d("SetupMap", "setupMap() called")
        mapFragment.getMapAsync { googleMap ->
            map = googleMap
            if (isPermissionGranted()) {
                enableMyLocation()
            }
            Log.d("OnViewCreated", "latitude: " + currentLocation.longitude.toString())
            map.uiSettings.isZoomControlsEnabled = true
            var latLng = LatLng(-34.0, 151.0)
            enableMyLocation()
            @SuppressLint("MissingPermission")
            if (_viewModel.latitude.value != null) {

                val lat: Double = _viewModel.latitude.value!!
                val long: Double = _viewModel.longitude.value!!
                latLng = LatLng(lat, long)
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13f))
            } else {//show current location
                latLng = LatLng(currentLocation.latitude, currentLocation.longitude)
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13f))
            }
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == REQUEST_TURN_DEVICE_LOCATION_ON ){
            checkDeviceLocationSettings(false)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun requestBackgroundPermissionAndAddGeofence(){
        val requestPermissionLauncher = this.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { results ->
            val backgroundLocationGranted: Boolean = results.getValue("android.permission.ACCESS_BACKGROUND_LOCATION")
            if (backgroundLocationGranted) {
                // Permission granted, proceed with map functionality
                Log.d("requestBackgroundPermission", "granted")
                addGeofence()
            } else {
                // Permission denied, handle the scenario
                explainBackgroundPermission(requireActivity())
                Log.d("requestBackgroundPermission", "denied")
            }
        }

        val permissions = arrayOf(
            Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        requestPermissionLauncher.launch(permissions)
    }

    private val REQUEST_TURN_DEVICE_LOCATION_ON = 29

}