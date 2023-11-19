package com.vkozhemi.bleapp.viewmodel

import androidx.databinding.ObservableBoolean
import androidx.lifecycle.ViewModel
import com.vkozhemi.bleapp.model.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(private val repository: Repository) : ViewModel() {
    var isConnect = ObservableBoolean(false)

    fun onClickDisconnect() {
        repository.disconnectGattServer()
    }

    companion object {
        private const val TAG: String = "DetailViewModel"
    }
}