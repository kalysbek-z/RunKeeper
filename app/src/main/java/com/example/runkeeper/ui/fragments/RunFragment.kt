package com.example.runkeeper.ui.fragments

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.example.runkeeper.*
import com.example.runkeeper.database.Run
import com.example.runkeeper.ui.HistoryViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolylineOptions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.run_fragment.*
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import java.lang.Math.round
import java.util.*

const val REQUEST_CODE_LOC_PERMISSION = 0

//const val POLYLINE_COLOR = Color.parseColor("#FF6347")
const val POLYLINE_WIDTH = 10f

const val MAP_ZOOM = 18f

@AndroidEntryPoint
class RunFragment : Fragment(R.layout.run_fragment), EasyPermissions.PermissionCallbacks {

    private val viewModel: HistoryViewModel by viewModels()

    private var isTracking = false
    private var pathPoint = mutableListOf<Polyline>()

    private var map: GoogleMap? = null

    private var currentTime = 0L

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requestPermission()

        mapView.onCreate(savedInstanceState)

        mapView.getMapAsync {
            map = it
            addAllPolylines()
        }

        subscribeToObservers()

        start_button.setOnClickListener {
            toggleRun()
        }

        stop_button.setOnClickListener {
//            stopRun()
            zoomTrack()
            stopAndSave()
        }

    }

    private fun subscribeToObservers() {
        TrackingService.isTracking.observe(viewLifecycleOwner, Observer {
            updateCameraTracking(it)
        })

        TrackingService.pathPoints.observe(viewLifecycleOwner, Observer {
            pathPoint = it
            addLastPolyline()
            followUser()
        })

        TrackingService.timeInMillis.observe(viewLifecycleOwner, Observer {
            currentTime = it
            val formattedTime = TrackingUtility.getFormattedTime(currentTime)
            time_tv_runFrag.text = formattedTime
        })
    }

    private fun toggleRun() {
        if (isTracking) {
            sendCommandToService(ACTION_PAUSE_SERVICE)
        } else {
            sendCommandToService(ACTION_START_OR_RESUME_SERVICE)
        }
    }

    private fun stopRun() {
        sendCommandToService(ACTION_STOP_SERVICE)
    }

    private fun updateCameraTracking(isTracking: Boolean) {
        this.isTracking = isTracking
        if (!isTracking) {
            stop_button.visibility = View.GONE
            start_button.visibility = View.VISIBLE
        } else {
            start_button.visibility = View.GONE
            stop_button.visibility = View.VISIBLE
        }
    }

    private fun followUser() {
        if (pathPoint.isNotEmpty() && pathPoint.last().isNotEmpty()) {
            map?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    pathPoint.last().last(),
                    MAP_ZOOM
                )
            )
        }
    }

    private fun zoomTrack() {
        val bounds = LatLngBounds.Builder()
        for (polyline in pathPoint) {
            for (pos in polyline) {
                bounds.include(pos)
            }
        }

        map?.moveCamera(
            CameraUpdateFactory.newLatLngBounds(
                bounds.build(),
                mapView.width,
                mapView.height,
                (mapView.height * 0.05f).toInt()
            )
        )
    }

    private fun stopAndSave() {
        map?.snapshot { bmp ->
            var distance = 0f
            for (polyline in pathPoint) {
                distance += TrackingUtility.calculateDistance(polyline)
            }
            val speed = round((distance / 1000f) / (currentTime / 1000f / 60 / 60) * 10) / 10f
            val dateTimestamp = Calendar.getInstance().timeInMillis

            val run = Run(bmp, dateTimestamp, speed, distance, currentTime)
            viewModel.insertRun(run)

            Toast.makeText(
                context,
                "Your run was successfully saved in history!",
                Toast.LENGTH_SHORT
            ).show()
            stopRun()
        }
    }

    private fun addAllPolylines() {
        for (polyline in pathPoint) {
            val polylineOptions = PolylineOptions()
                .color(Color.parseColor("#FF6347"))
                .width(POLYLINE_WIDTH)
                .addAll(polyline)

            map?.addPolyline(polylineOptions)
        }
    }

    private fun addLastPolyline() {
        if (pathPoint.isNotEmpty() && pathPoint.last().size > 1) {
            val preLastLatLng = pathPoint.last()[pathPoint.last().size - 2]
            val lastLatLng = pathPoint.last().last()
            val polylineOptions = PolylineOptions()
                .color(Color.parseColor("#FF6347"))
                .width(POLYLINE_WIDTH)
                .add(preLastLatLng)
                .add(lastLatLng)
            map?.addPolyline(polylineOptions)
        }
    }

    private fun sendCommandToService(action: String) =
        Intent(requireContext(), TrackingService::class.java).also {
            it.action = action
            requireContext().startService(it)
        }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }

    private fun requestPermission() {
        if (TrackingUtility.hasLocationPermissions(requireContext())) {
            return
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            EasyPermissions.requestPermissions(
                this,
                "You need accept location permissions to use app",
                REQUEST_CODE_LOC_PERMISSION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            EasyPermissions.requestPermissions(
                this,
                "You need accept location permissions to use app",
                REQUEST_CODE_LOC_PERMISSION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this).build().show()
        } else {
            requestPermission()
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }
}