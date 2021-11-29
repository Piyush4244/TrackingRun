package com.example.trackyourrun.ui.fragments

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.trackyourrun.R
import com.example.trackyourrun.databinding.FragmentRunBinding
import com.example.trackyourrun.other.Constants
import com.example.trackyourrun.ui.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class RunFragment : Fragment(){

    private val viewModel: MainViewModel by viewModels()
    private var _binding: FragmentRunBinding? = null
    private val binding get() = _binding!!
    private lateinit var requestPermissionLauncher:ActivityResultLauncher<Array<String>>
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        makeRequestPermissionLauncher()
        binding.fab.setOnClickListener{
            handlePermission()
        }
    }
    private fun handlePermission(){

        when {
            Constants.checkLocationPermission(context!!)-> {
                // You can use the API that requires the permission.
                Timber.d("permission already granted")
                this.findNavController().navigate(RunFragmentDirections.actionRunFragmentToTrackingFragment())
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) &&
                    shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)-> {
            AlertDialog.Builder(context)
                .setMessage("In order to track your run location is required")
                .create()
        }
            else -> {
                // You can directly ask for the permission.
                // The registered ActivityResultCallback gets the result of this request.
                requestPermissionLauncher.launch(arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ))
            }
        }
    }

    private fun makeRequestPermissionLauncher() {
        requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ){
                if(it[Manifest.permission.ACCESS_FINE_LOCATION]==true&&it[Manifest.permission.ACCESS_COARSE_LOCATION]==true){
                    Timber.d("Permission granted")
                }else {
                    Timber.d("Permission not granted")
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRunBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}