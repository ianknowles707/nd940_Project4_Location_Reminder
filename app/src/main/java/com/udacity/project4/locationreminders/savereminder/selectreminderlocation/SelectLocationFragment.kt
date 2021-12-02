package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject


class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding


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


//        TODO: zoom to the user location after taking his permission

//        TODO: put a marker to location that the user selected


//        TODO: call this function after the user confirms on the selected location
        onLocationSelected()

        return binding.root
    }

    //Set up the map - initially just define a position for start
    override fun onMapReady(map: GoogleMap) {
        val home = LatLng(45.45, -73.59)
        val zoom = 15f

        //check location permissions
        val locationPermissionsGranted = isForegroundAndBackgroundPermissionGranted()
        Log.i("map: ", "Permissions granted: $locationPermissionsGranted")
        map.addMarker(MarkerOptions().position(home).title("Home"))
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(home, zoom))

        setMapStyle(map)


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
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
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

    private fun onLocationSelected() {
        //        TODO: When the user confirms on the selected location,
        //         send back the selected location details to the view model
        //         and navigate back to the previous fragment to save the reminder and add the geofence
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        // TODO: Change the map type based on the user's selection.
        R.id.normal_map -> {
            true
        }
        R.id.hybrid_map -> {
            true
        }
        R.id.satellite_map -> {
            true
        }
        R.id.terrain_map -> {
            true
        }
        else -> super.onOptionsItemSelected(item)
    }


}
