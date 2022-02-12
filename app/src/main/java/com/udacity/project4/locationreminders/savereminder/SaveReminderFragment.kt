package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.savereminder.selectreminderlocation.SelectLocationFragment
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding

    private lateinit var geofencingClient: GeofencingClient

    private lateinit var reminderData: ReminderDataItem

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel

        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            //            Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener {
            reminderData = ReminderDataItem(
                _viewModel.reminderTitle.value,
                _viewModel.reminderDescription.value,
                _viewModel.reminderSelectedLocationStr.value,
                _viewModel.latitude.value,
                _viewModel.longitude.value
            )
            _viewModel.validateAndSaveReminder(reminderData)

            //Check if reminder was saved. If so, verify (or request)
            // permissions and add a geofence at this location
            if (_viewModel.dataSaved.value == true) {
                if (!isForegroundPermissionGranted()) {
                    requestForegroundLocationPermission()
                } else if (!isBackgroundPermissionGranted()) {
                    requestBackgroundLocationPosition()
                } else {
                    setGeofenceForReminder(reminderData)
                }
            } else {
                Toast.makeText(
                    activity,
                    getString(R.string.error_saving_data),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
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
    //ONLY be made if foreground is granted, and only for Version Q and above
    private fun requestForegroundLocationPermission() {
        val permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        val requestCode = SelectLocationFragment.REQUEST_FOREGROUND_ONLY
        requestPermissions(permissionsArray, requestCode)
    }

    //Background location will only be requested after foreground is granted, as this
    //is now a requirement in API 30
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestBackgroundLocationPosition() {
        val permissionArray = arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        val requestCode = SelectLocationFragment.REQUEST_BACKGROUND_ONLY
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
        if (requestCode == SelectLocationFragment.REQUEST_FOREGROUND_ONLY) {
            if (grantResults.isNotEmpty() && (grantResults[0] ==
                        PackageManager.PERMISSION_GRANTED)
            ) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.foreground_permission_granted),
                    Toast.LENGTH_LONG
                ).show()
                requestBackgroundLocationPosition()
            } else if (grantResults.isNotEmpty() && (grantResults[0] ==
                        PackageManager.PERMISSION_DENIED)
            ) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.permission_denied_explanation),
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else if (requestCode == SelectLocationFragment.REQUEST_BACKGROUND_ONLY) {
            if (grantResults.isNotEmpty() && (grantResults[0] ==
                        PackageManager.PERMISSION_GRANTED)
            ) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.background_permission_granted),
                    Toast.LENGTH_LONG
                ).show()
            setGeofenceForReminder(reminderData)
            } else if (grantResults.isNotEmpty() && (grantResults[0] ==
                        PackageManager.PERMISSION_DENIED)
            ) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.background_permission_denied),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun setGeofenceForReminder(reminderData: ReminderDataItem) {

        geofencingClient = LocationServices.getGeofencingClient(activity!!)

        //Create the geofence
        val geofenceForReminder = Geofence.Builder()
            .setRequestId(reminderData.id)
            .setCircularRegion(
                reminderData.latitude!!,
                reminderData.longitude!!,
                SaveReminderViewModel.GEOFENCE_AREA
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        //Use geofencingRequest to add geofence
        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofenceForReminder)
            .build()

        //Create the pending intent for action when the geofence is triggered
        val geofencePendingIntent: PendingIntent by lazy {
            val intent = Intent(activity!!, GeofenceBroadcastReceiver::class.java)
            PendingIntent.getBroadcast(activity!!, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)?.run {
            addOnSuccessListener {
                Toast.makeText(
                    activity!!, getString(R.string.reminder_saved),
                    Toast.LENGTH_SHORT
                )
                    .show()
            }

            addOnFailureListener {
                
                Toast.makeText(
                    activity!!, "${getString(R.string.geofences_not_added)}: ${it.message}",
                    Toast.LENGTH_SHORT
                )
                    .show()
                Log.i("Geofence: ", "Error: ${it.message}")
            }
        }

    }


    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

}
