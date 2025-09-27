package com.example.chatapp.feature.auth.signup

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(): ViewModel() {

    private val _state = MutableStateFlow<SignUpState>(SignUpState.Nothing)
    val state = _state.asStateFlow()

    fun signUp(name: String, email: String, password: String) {
        _state.value = SignUpState.Loading
        //Firebase SignUp
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    task.result.user?.let {
                        it.updateProfile(
                            com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                .setDisplayName(name)
                                .build()
                        )?.addOnCompleteListener {
                            _state.value = SignUpState.Success
                        }
                        return@addOnCompleteListener
                    }
                    _state.value = SignUpState.Error
                } else {
                    _state.value = SignUpState.Error
                }
            }

    }


}





sealed class SignUpState{
    data object Nothing: SignUpState()

    data object Loading: SignUpState()

    data object Success: SignUpState()

    data object Error: SignUpState()
}