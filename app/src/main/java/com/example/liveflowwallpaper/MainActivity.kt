package com.example.liveflowwallpaper

import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var previewButton: Button
    private lateinit var backgroundButton: Button
    private lateinit var flowButton: Button
    private lateinit var shapeSpinner: Spinner

    private val backgroundRequestCode = 101
    private val flowRequestCode = 102

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        previewButton =
            findViewById(R.id.previewButton)

        backgroundButton =
            findViewById(R.id.backgroundButton)

        flowButton =
            findViewById(R.id.flowButton)

        shapeSpinner =
            findViewById(R.id.shapeSpinner)

        setupShapeSpinner()

        previewButton.setOnClickListener {
            previewWallpaper()
        }

        backgroundButton.setOnClickListener {
            openPhotoPicker(backgroundRequestCode)
        }

        flowButton.setOnClickListener {
            openPhotoPicker(flowRequestCode)
        }
    }

    private fun setupShapeSpinner() {
        val shapes = arrayOf(
            "Circle",
            "Rectangle",
            "Triangle",
            "Pentagon",
            "Hexagon",
            "Random"
        )

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            shapes
        )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        shapeSpinner.adapter = adapter
    }

    private fun previewWallpaper() {
        try {
            val intent = Intent(
                WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER
            )

            intent.putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(
                    this,
                    FlowWallpaperService::class.java
                )
            )

            startActivity(intent)

        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Preview could not be opened",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun openPhotoPicker(
        requestCode: Int
    ) {
        val intent = Intent(
            Intent.ACTION_OPEN_DOCUMENT
        )

        intent.type = "image/*"

        intent.addCategory(
            Intent.CATEGORY_OPENABLE
        )

        intent.addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        )

        startActivityForResult(
            intent,
            requestCode
        )
    }
        override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (
            resultCode != RESULT_OK ||
            data?.data == null
        ) {
            return
        }

        val imageUri: Uri =
            data.data!!

        try {
            contentResolver.takePersistableUriPermission(
                imageUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {
        }

        val preferences =
            getSharedPreferences(
                "LiveFlow",
                MODE_PRIVATE
            )

        if (requestCode == backgroundRequestCode) {

            preferences.edit()
                .putString(
                    "background_photo",
                    imageUri.toString()
                )
                .apply()

            Toast.makeText(
                this,
                "Background photo selected",
                Toast.LENGTH_SHORT
            ).show()

        } else if (requestCode == flowRequestCode) {

            preferences.edit()
                .putString(
                    "flow_photo",
                    imageUri.toString()
                )
                .apply()

            Toast.makeText(
                this,
                "Flow photo selected",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
