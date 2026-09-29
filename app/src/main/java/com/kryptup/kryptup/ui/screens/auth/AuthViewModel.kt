package com.kryptup.kryptup.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kryptup.kryptup.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _authState.value = AuthState.Error("Please fill all fields")
            return
        }
        
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            repository.login(email, pass)
                .onSuccess {
                    _authState.value = AuthState.Success("Login successful")
                }
                .onFailure {
                    _authState.value = AuthState.Error(it.message ?: "Login failed")
                }
        }
    }

    fun register(email: String, pass: String, name: String) {
        if (email.isBlank() || pass.isBlank() || name.isBlank()) {
            _authState.value = AuthState.Error("Please fill all fields")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            repository.register(email, pass, name)
                .onSuccess {
                    _authState.value = AuthState.Success("Registration successful")
                }
                .onFailure {
                    _authState.value = AuthState.Error(it.message ?: "Registration failed")
                }
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            repository.firebaseSignInWithGoogle(idToken)
                .onSuccess {
                    _authState.value = AuthState.Success("Google Sign-In successful")
                }
                .onFailure {
                    _authState.value = AuthState.Error(it.message ?: "Google Sign-In failed")
                }
        }
    }
}
