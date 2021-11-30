package com.udacity.project4.authentication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.map

class AuthenticationViewModel : ViewModel() {

    enum class AuthenticatedState {
        AUTHENTICATED, UNAUTHENTICATED
    }

    val authenticatedState = UserLiveData().map { user ->
        if (user != null) {
            AuthenticatedState.AUTHENTICATED
        } else {
            AuthenticatedState.UNAUTHENTICATED
        }
    }
}