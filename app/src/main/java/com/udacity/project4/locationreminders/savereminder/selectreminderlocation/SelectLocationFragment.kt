package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject


class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding

    private lateinit var map: GoogleMap

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        //Initialize map
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map_display) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.saveButton.setOnClickListener {
            onLocationSelected()
        }

        return binding.root
    }

    //Set up the map - initially just define a position for start
    override fun onMapReady(newMap: GoogleMap) {
        map = newMap
        val home = LatLng(45.45, -73.59)
        val zoom = 15f

        //check location permissions
        val locationPermissionsGranted = isForegroundAndBackgroundPermissionGranted()

        //Request permissions if not already granted. API 30 requires a request for
        //foreground location ONLY, and then if granted, a request for background
        //location
        if (!locationPermissionsGranted) {
            requestForegroundLocationPermission()
        }

        //Check that the device location settings is enabled and resolve this if not
        checkDeviceLocationSettings()

        if (locationPermissionsGranted) {
            setDeviceLocation(map)
        }

        newMap.moveCamera(CameraUpdateFactory.newLatLngZoom(home, zoom))

        setMapStyle(map)

        //NOT WORKING TO BE RESOLVED
        map.uiSettings.isZoomControlsEnabled = true

        setPoiMarker(map)

        setLocationMarker(map)
    }

    //Apply custom map style from JSON file
    private fun setMapStyle(map: GoogleMap) {
        try {
            val mapStyled = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    context,
                    R.raw.map_style
                )
            )
            if (!mapStyled) {
                Log.e("map: ", "Failed to add style")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e("map: ", "Error: ", e)
        }
    }

    //Check to see if permissions are granted or not. Returns TRUE if all needed
    //permissions are already granted, of FALSE if permission needs to be requested
    private fun isForegroundAndBackgroundPermissionGranted(): Boolean {
        val foreground = isForegroundPermissionGranted()
        val background = isBackgroundPermissionGranted()
        return background && foreground
    }

    //Check if foreground location permission already granted
    private fun isForegroundPermissionGranted(): Boolean {
        return (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                )
    }

    //Check if background location permission already granted
    private fun isBackgroundPermissionGranted(): Boolean {
        val backgroundLocationApproved = (
                //Background location permission only required for Android Q and above
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    )
                } else {
                    true
                }
                )
        return backgroundLocationApproved
    }

    //Request foreground location permissions. Request for background location will
    //ONLY be made if foreground is granted
    private fun requestForegroundLocationPermission() {
        val permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        val requestCode = REQUEST_FOREGROUND_ONLY
        requestPermissions(permissionsArray, requestCode)
    }

    //Background location will only be requested after foreground is granted, as this
    //is now a requirement in API 30
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestBackgroundLocationPosition() {
        val permissionArray = arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        val requestCode = REQUEST_BACKGROUND_ONLY
        requestPermissions(permissionArray, requestCode)
    }

    //Respond to the permission request result. If Foreground request approved, request
    //background. If not, show message that it is needed for app functionality
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_FOREGROUND_ONLY) {
            if (grantResults.isNotEmpty() && (grantResults[0] ==
                        PackageManager.PERMISSION_GRANTED)
            ) {
                requestBackgroundLocationPosition()
            } else if (grantResults.isNotEmpty() && (grantResults[0] ==
                        PackageManager.PERMISSION_DENIED)
            ) {
                Toast.makeText(
                    requireContext(),
                    "App requires location permission. Check settings.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else if (requestCode == REQUEST_BACKGROUND_ONLY) {
            if (grantResults.isNotEmpty() && (grantResults[0] ==
                        PackageManager.PERMISSION_GRANTED)
            ) {
                Toast.makeText(
                    requireContext(),
                    "Background location permission granted",
                    Toast.LENGTH_LONG
                ).show()
            } else if (grantResults.isNotEmpty() && (grantResults[0] ==
                        PackageManager.PERMISSION_DENIED)
            ) {
                Toast.makeText(
                    requireContext(),
                    "App requires background location permission. Check settings.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    //Check the device location settings is on, and ask user to resolve the issue
    //if it is not
    private fun checkDeviceLocationSettings() {
        val requestUserLocation = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(
            requestUserLocation
        )
        val settingsClient = LocationServices.getSettingsClient(requireContext())
        val responseTask = settingsClient.checkLocationSettings(builder.build())

        //If the location request fails, try to resolve the issue
        responseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    startIntentSenderForResult(
                        exception.resolution.intentSender,
                        REQUEST_TURN_DEVICE_LOCATION_ON,
                        null, 0, 0, 0, null
                    )

                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.i("Location settings: ", "Error resolving" + sendEx.message)
                }
            } else {
                Toast.makeText(
                    context,
                    "Location required. Check device settings",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        responseTask.addOnSuccessListener {
            Log.i("Location settings: ", "Location enabled")
        }
    }


    //Check that when the user was presented with location settings option, they
    //selected to turn on location. If not, send them round the loop again!
    //If they keep selecting no, they will keep going to the checkDeviceLocationSettings
    //function
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            checkDeviceLocationSettings()
        }
    }

    //If the permissions are granted and location setting is on, enable device location
    //Called from a permission check so does not need to do another permission check here
    @SuppressLint("MissingPermission")
    private fun setDeviceLocation(map: GoogleMap) {
        map.isMyLocationEnabled = true
    }

    //Allow user to select any Lat/Lng as reminder location
    private fun setLocationMarker(map: GoogleMap) {
        map.setOnMapLongClickListener {
            map.clear()
            val displayedLocation = String.format(
                "Lat: %1.3f, Long: %2.3f",
                it.latitude,
                it.longitude
            )
            map.addMarker(
                MarkerOptions().position(it)
                    .title(getString(R.string.dropped_pin))
                    .snippet(displayedLocation)
            )
            _viewModel.reminderSelectedLocationStr.value = getString(R.string.user_defined)
            _viewModel.latitude.value = it.latitude
            _viewModel.longitude.value = it.longitude

            //Save location only enabled after a location is selected
            binding.saveButton.isEnabled = true
        }
    }

    //Allow user to select a POI as the reminder location
    private fun setPoiMarker(map: GoogleMap) {
        map.setOnPoiClickListener {
            map.clear()
            val poiMarker = map.addMarker(
                MarkerOptions()
                    .position(it.latLng)
                    .title(it.name)
            )
            poiMarker.showInfoWindow()
            _viewModel.reminderSelectedLocationStr.value = it.name
            _viewModel.latitude.value = it.latLng.latitude
            _viewModel.longitude.value = it.latLng.longitude

            //Save location only enabled after a location is selected
            binding.saveButton.isEnabled = true
        }
    }

    private fun onLocationSelected() {
        _viewModel.navigationCommand.value =
            NavigationCommand.To(
                SelectLocationFragmentDirections
                    .actionSelectLocationFragmentToSaveReminderFragment()
            )
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {

        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    companion object {
        const val REQUEST_FOREGROUND_ONLY = 444
        const val REQUEST_BACKGROUND_ONLY = 888
        const val REQUEST_TURN_DEVICE_LOCATION_ON = 222
    }

}
