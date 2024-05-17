package com.example.helloimages

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.registerForActivityResult
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.helloimages.databinding.ActivityMainBinding
import java.io.File
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private val binding:ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val viewModel: MainViewModel by viewModels()

    private val cameraXLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        ::onCameraXResult
    )
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        ::onCameraResult
    )
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        ::onGalleryResult
    )

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            permissions.entries.forEach {
                if(!it.value){
                    Toast.makeText(this, "Permission ${it.key} denied", Toast.LENGTH_SHORT).show()
                }
            }
        }

    private fun onGalleryResult(result: ActivityResult) {
        viewModel.isLoading.postValue(false)
        if (result.resultCode == RESULT_OK) {
            viewModel.selectedImageUri.postValue(result.data?.data)
            viewModel.state.postValue(ImageState.SELECTED)
        } else {
            viewModel.state.postValue(ImageState.NOT_SELECTED)
        }
    }

    private fun onCameraResult(result: ActivityResult) {
        viewModel.isLoading.postValue(false)
        if(result.resultCode != Activity.RESULT_OK){
            viewModel.selectedImageUri.postValue(null)
            Toast.makeText(this, "Could not save the image", Toast.LENGTH_SHORT).show()
            viewModel.state.postValue(ImageState.NOT_SELECTED)
        }else{
            viewModel.selectedImageUri.postValue(viewModel.tempCameraImageUri.value)
            viewModel.state.postValue(ImageState.SELECTED)
        }
    }

    private fun onCameraXResult(result: ActivityResult) {
        viewModel.isLoading.postValue(false)
        if(result.resultCode==Activity.RESULT_OK){
            val uri = result.data?.extras?.getString("uri")
            viewModel.selectedImageUri.postValue(uri?.toUri())
            viewModel.state.postValue(ImageState.SELECTED)
        }else{
            viewModel.state.postValue(ImageState.NOT_SELECTED)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        viewModel.selectedImageUri.observe(this){
            binding.imageView.setImageURI(it)
        }

        viewModel.isLoading.observe(this){ isLoading ->
            if(isLoading){
                binding.progressIndicator.visibility = View.VISIBLE
            }else{
                binding.progressIndicator.visibility = View.GONE
            }
        }

        viewModel.state.observe(this){
            binding.state.text = it.name
        }

        viewModel.selectedImageUri.observe(this){
            if(it!=null){
                binding.path.text = it.path.toString().ifEmpty {
                    "Non selected"
                }
            }
        }

        binding.btnGallery.setOnClickListener {
            if(areMediaPermissionsGranted()){
                viewModel.isLoading.postValue(true)
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.type = "image/*"
                galleryLauncher.launch(intent)
            } else {
                requestPermissions()
            }
        }

        binding.btnCamera.setOnClickListener {
            if(areMediaPermissionsGranted()){
                viewModel.isLoading.postValue(true)
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                val fileName = "photo_${Calendar.getInstance().time}.png"
                val file = File("${getExternalFilesDir(null)}/${fileName}")
                val uri = FileProvider.getUriForFile(this, packageName, file)
                intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
                viewModel.tempCameraImageUri.postValue(uri)
                cameraLauncher.launch(intent)
            } else {
                requestPermissions()
            }
        }

        binding.btnCameraX.setOnClickListener{
            viewModel.isLoading.postValue(true)
            val intent = Intent(this, CameraActivity::class.java)
            cameraXLauncher.launch(intent)
        }
    }

    private fun areMediaPermissionsGranted(): Boolean {
        val permissions = mutableListOf(
            Manifest.permission.CAMERA
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
        var allGranted = true
        permissions.forEach {
            allGranted = ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_DENIED && allGranted
        }
        return allGranted
    }

    private fun requestPermissions(){
        requestPermissionLauncher.launch(mutableListOf(
            Manifest.permission.CAMERA
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }.toTypedArray())
    }
}