package com.kryptup.kryptup.ui.screens.banking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kryptup.kryptup.data.repository.BankingRepository
import com.kryptup.kryptup.security.PinSecurityManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BankingViewModel @Inject constructor(
    private val repository: BankingRepository,
    val pinSecurityManager: PinSecurityManager
) : ViewModel() {

    private val _isVerifying = MutableStateFlow(false)
    val isVerifying: StateFlow<Boolean> = _isVerifying

    private val _linkingSuccess = MutableStateFlow(false)
    val linkingSuccess: StateFlow<Boolean> = _linkingSuccess

    fun verifyCard(cardNumber: String, expiry: String, cvv: String) {
        viewModelScope.launch {
            _isVerifying.value = true
            val success = repository.verifyCard(cardNumber, expiry, cvv)
            _isVerifying.value = false
            // In a real flow, this would trigger the OTP step
        }
    }

    fun verifyOtp(otp: String) {
        viewModelScope.launch {
            _isVerifying.value = true
            val success = repository.verifyOtp(otp)
            _isVerifying.value = false
            if (success) {
                _linkingSuccess.value = true
            }
        }
    }
}
