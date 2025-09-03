package com.ibylin.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ibylin.app.data.model.User
import com.ibylin.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {
    
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        loadUsers()
    }
    
    private fun loadUsers() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                userRepository.getUsers().collect { userList ->
                    _users.value = userList
                }
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun addUser(name: String, email: String) {
        viewModelScope.launch {
            val newUser = User(
                id = (_users.value.maxOfOrNull { it.id } ?: 0) + 1,
                name = name,
                email = email
            )
            userRepository.insertUser(newUser)
        }
    }
}
