package com.udacity.project4.authentication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityAuthenticationBinding
import com.udacity.project4.locationreminders.RemindersActivity

/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */
class AuthenticationActivity : AppCompatActivity() {

    companion object {
        //Define a constant to identify the result of the authentication activity
        //to ensure we are responding to the correct activity
        const val SIGN_IN_REQUEST = 1001
    }

    private lateinit var binding: ActivityAuthenticationBinding

    //Get a reference to the AuthenticationViewModel
    private val viewModel by viewModels<AuthenticationViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_authentication)


        //Set listener on the login/signup button to launch Firebase authentication
        binding.buttonLogin.setOnClickListener {
            loginOrSignUpWithFirebase()
        }

        viewModel.authenticatedState.observe(this, Observer { authenticatedState ->
            when (authenticatedState) {
                AuthenticationViewModel.AuthenticatedState.AUTHENTICATED -> {
                    intent = Intent(applicationContext, RemindersActivity::class.java)
                    startActivity(intent)
                }

                else -> {

                }
            }

        })


    }

    private fun loginOrSignUpWithFirebase() {
        //Define available sign-in options
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )
        //Set custom layout for sign-in screen
        val customLogin = AuthMethodPickerLayout
            .Builder(R.layout.authenticate_custom_layout)
            .setGoogleButtonId(R.id.google_login_provider)
            .setEmailButtonId(R.id.email_login_provider)
            .build()

        //Start Firebase authentication. Provide result code to identify the
        //result of this activity
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setAuthMethodPickerLayout(customLogin)
                .build(),
            SIGN_IN_REQUEST
        )
    }

    //Check result of authentication activity - Log response (for now)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == SIGN_IN_REQUEST) {
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK) {
                Log.i(
                    "Sign-in: ", "OK, user ${
                        FirebaseAuth.getInstance().currentUser?.displayName
                    }"
                )
            } else {
                Log.i(
                    "Sign-in: ", "Unsuccessful, ${response?.error?.errorCode}"
                )
            }
        }
    }

}
