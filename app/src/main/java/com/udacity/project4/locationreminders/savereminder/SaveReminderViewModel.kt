package com.udacity.project4.locationreminders.savereminder

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.PointOfInterest
import com.udacity.project4.R
import com.udacity.project4.base.BaseViewModel
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SaveReminderViewModel(val app: Application, val dataSource: ReminderDataSource) :
    BaseViewModel(app) {
    val reminderTitle = MutableLiveData<String>()
    val reminderDescription = MutableLiveData<String>()
    val reminderSelectedLocationStr = MutableLiveData<String>()
    val selectedPOI = MutableLiveData<PointOfInterest>()
    val latitude = MutableLiveData<Double>()
    val longitude = MutableLiveData<Double>()

    private lateinit var geofencingClient: GeofencingClient

    companion object {
        const val GEOFENCE_AREA = 100f
    }


    /**
     * Clear the live data objects to start fresh next time the view model gets called
     */
    fun onClear() {
        reminderTitle.value = null
        reminderDescription.value = null
        reminderSelectedLocationStr.value = null
        selectedPOI.value = null
        latitude.value = null
        longitude.value = null
    }

    /**
     * Validate the entered data then saves the reminder data to the DataSource
     */

    fun provideDataInput() {
        val reminderData = ReminderDataItem(
            reminderTitle.value,
            reminderDescription.value,
            reminderSelectedLocationStr.value,
            latitude.value,
            longitude.value
        )
        validateAndSaveReminder(reminderData)
    }

    private fun validateAndSaveReminder(reminderData: ReminderDataItem) {
        if (validateEnteredData(reminderData)) {
            saveReminder(reminderData)
        } else {
            showSnackBar
        }
    }

    /**
     * Save the reminder to the data source
     */
    fun saveReminder(reminderData: ReminderDataItem) {
        showLoading.value = true
        viewModelScope.launch {
            dataSource.saveReminder(
                ReminderDTO(
                    reminderData.title,
                    reminderData.description,
                    reminderData.location,
                    reminderData.latitude,
                    reminderData.longitude,
                    reminderData.id
                )
            )
            setGeofenceForReminder(reminderData)
            showLoading.value = false
            showToast.value = app.getString(R.string.reminder_saved)
            navigationCommand.value =
                NavigationCommand.To(
                    SaveReminderFragmentDirections
                        .actionSaveReminderFragmentToReminderListFragment()
                )
        }
    }

    @SuppressLint("MissingPermission")
    private fun setGeofenceForReminder(reminderData: ReminderDataItem) {

        geofencingClient = LocationServices.getGeofencingClient(Activity().applicationContext)

        val geofenceForReminder = Geofence.Builder()
            .setRequestId(reminderData.id)
            .setCircularRegion(
                reminderData.latitude!!,
                reminderData.longitude!!,
                GEOFENCE_AREA
            )
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofenceForReminder)
            .build()

        val geofencePendingIntent: PendingIntent by lazy {
            val intent = Intent()
            PendingIntent.getBroadcast(Activity(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        geofencingClient = LocationServices.getGeofencingClient(Activity())

        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)?.run {
            addOnSuccessListener {
                Toast.makeText(
                    Activity(), "Geofence added to reminder location",
                    Toast.LENGTH_SHORT
                )
                    .show()
            }

            addOnFailureListener {
                Toast.makeText(
                    Activity(), "Geofence failed",
                    Toast.LENGTH_SHORT
                )
                    .show()
                Log.i("Geofence: ", "Error: ${it.message}")
            }
        }


    }

    /**
     * Validate the entered data and show error to the user if there's any invalid data
     */
    private fun validateEnteredData(reminderData: ReminderDataItem): Boolean {
        if (reminderData.title.isNullOrEmpty()) {
            showSnackBarInt.value = R.string.err_enter_title
            return false
        }

        if (reminderData.location.isNullOrEmpty()) {
            Log.i("TAG", "Location: ${reminderData.location}")

            showSnackBarInt.value = R.string.err_select_location
            return false
        }
        return true
    }
}
