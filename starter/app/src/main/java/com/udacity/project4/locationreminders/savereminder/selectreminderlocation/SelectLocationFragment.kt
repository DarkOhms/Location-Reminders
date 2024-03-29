package com.udacity.project4.locationreminders.savereminder.selectreminderlocation

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.CheckBox
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand.Back
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.Locale

class SelectLocationFragment : BaseFragment() {

    // Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var map: GoogleMap

    private var currentLocation: Location? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //check api level for background permissions flow
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (!isBackgroundPermissionGranted()) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        requireActivity(),
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    )
                ) {
                    Log.d("SelectLocationFragment", "shouldShowPermissionRational returned true")
                    requestBackgroundPermission()
                    explainBackgroundPermission(requireActivity())
                }else{
                    Log.d("SelectLocationFragment", "shouldShowPermissionRational returned false")
                    requestBackgroundPermission()
                }
            }
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val layoutId = R.layout.fragment_select_location
        binding = DataBindingUtil.inflate(inflater, layoutId, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this
        binding.saveLocationButton.setOnClickListener{
            onLocationSelected()
        }


        //use shared preferences to keep track of DONT_SHOW_REMINDER_INSTRUCTION
        val sharedPref = requireActivity().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val shouldShowInstruction = sharedPref.getBoolean("SHOW_REMINDER_INSTRUCTION", true)

        if (shouldShowInstruction) {
            showInstructionalDialog(requireActivity())
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
// ... (proceed with location handling)

        setDisplayHomeAsUpEnabled(true)

        // TODO: add the map setup implementation
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync { googleMap ->
            map = googleMap
            // TODO: add style to the map
            setMapStyle(map)
            setPoiClick(map)
            setMapLongClick(map)
            map.uiSettings.isZoomControlsEnabled = true
            if(isPermissionGranted()){
                enableMyLocation()
            }
            var latLng = LatLng(-34.0, 151.0)
            enableMyLocation()
            @SuppressLint("MissingPermission")
            if(isPermissionGranted()){
                Log.d("MapsAsync" , "Permission is Granted")
                val locationResultTask = locationClient.lastLocation
                locationResultTask.addOnSuccessListener { location:Location ->
                    currentLocation = location
                    latLng = LatLng(location.latitude, location.longitude)
                    Log.d("MapsAsynch", "current lat " +location.latitude.toString())
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13f))
                }
            }

// Move the camera to the user's location with a smooth animation
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13f))
        }

        // TODO: zoom to the user location after taking his permission

        // TODO: put a marker to location that the user selected

        _viewModel.navigationCommand.observe(viewLifecycleOwner){command ->
            if(command == Back){
                findNavController().popBackStack()
                Log.d("SelectLocation", "Command is back")
            }
        }
        return binding.root
    }

    private fun isPermissionGranted() : Boolean {

        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
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
        map.isMyLocationEnabled = isPermissionGranted()
    }

    private fun setPoiClick(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            val poiMarker = map.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name)
            )
            poiMarker?.showInfoWindow()
            //change the current lat and long for saving
            _viewModel.latitude.value = poi.latLng.latitude
            _viewModel.longitude.value = poi.latLng.longitude
            _viewModel.selectedPOI.value = poi
            _viewModel.reminderSelectedLocationStr.value = poi.name
        }
    }

    private fun setMapLongClick(map: GoogleMap){
        map.setOnMapLongClickListener { latLng ->
            _viewModel.latitude.value = latLng.latitude
            _viewModel.longitude.value = latLng.longitude
            _viewModel.selectedPOI.value = null

            val snippet = String.format(
                Locale.getDefault(),
                "Lat: %1$.3f, Long: %2$.3f",
                latLng.latitude,
                latLng.longitude
            )
            _viewModel.reminderSelectedLocationStr.value = snippet
            map.addMarker(MarkerOptions()
                .title(getString(R.string.dropped_pin))
                .snippet(snippet)
                .position(latLng))
                ?.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
        }

    }
    private fun onLocationSelected() {
        // TODO: When the user confirms on the selected location,
        //  send back the selected location details to the view model
        //  and navigate back to the previous fragment to save the reminder and add the geofence
        _viewModel.navigationCommand.value = Back
        Log.d("SelectLocationFragment", "onLocationSelected()")
    }
    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {

            R.id.normal_map -> {
                map.mapType = GoogleMap.MAP_TYPE_NORMAL
                return true
            }

            R.id.hybrid_map -> {
                map.mapType = GoogleMap.MAP_TYPE_HYBRID
                return true
            }

            R.id.satellite_map -> {
                map.mapType = GoogleMap.MAP_TYPE_SATELLITE
                return true
            }

            R.id.terrain_map -> {
                map.mapType = GoogleMap.MAP_TYPE_TERRAIN
                return true
            }

            else -> return true
        }
    }
    private fun setMapStyle(map: GoogleMap) {
        try {
            // Customize the styling of the base map using a JSON object defined
            // in a raw resource file.
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(),
                    R.raw.maps_style
                )
            )
            if (!success) {
                Log.e(TAG, "Style parsing failed.")
            }

        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Can't find style. Error: ", e)
        }
    }

    private fun showInstructionalDialog(context: Context) {
        val builder = AlertDialog.Builder(context)

        builder.setTitle(R.string.reminder_instruction_title) // Set your title string resource
            .setMessage(R.string.reminder_instruction_message) // Set your message string resource
            .setPositiveButton(R.string.ok) { dialog, _ ->
                dialog.dismiss()
            }

        val checkBox = CheckBox(context)
        checkBox.text = context.getString(R.string.dont_show_again) // Set checkbox text string resource
        builder.setView(checkBox)

        val dialog = builder.create()
        dialog.show()

        // Handle checkbox click for SharedPreferences
        checkBox.setOnCheckedChangeListener { _, isChecked ->
            val sharedPref = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val editor = sharedPref.edit()
            //using not checked as true
            editor.putBoolean("SHOW_REMINDER_INSTRUCTION", !isChecked)
            editor.apply()
        }
    }

    private fun isBackgroundPermissionGranted() : Boolean {

        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun requestBackgroundPermission(){
        val requestPermissionLauncher = this.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { results ->
            val backgroundLocationGranted: Boolean = results.getValue("android.permission.ACCESS_BACKGROUND_LOCATION")
            if (backgroundLocationGranted) {
                // Permission granted, proceed with map functionality
                Log.d("requestBackgroundPermission", "granted")
            } else {
                // Permission denied, handle the scenario
                Log.d("requestBackgroundPermission", "denied")
            }
        }

        val permissions = arrayOf(
            Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        requestPermissionLauncher.launch(permissions)
    }
}