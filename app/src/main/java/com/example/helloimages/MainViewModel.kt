package com.example.helloimages

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

enum class ImageState{
    NOT_SELECTED,
    SELECTED,
    UPLOADED
}

class MainViewModel:ViewModel() {

    val selectedImageUri = MutableLiveData<Uri?>()
    val tempCameraImageUri = MutableLiveData<Uri?>()
    val isLoading = MutableLiveData(false)
    val state = MutableLiveData(ImageState.NOT_SELECTED)

}