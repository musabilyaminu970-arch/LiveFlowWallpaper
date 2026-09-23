package com.example.liveflowwallpaper

import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView

class MainActivity : Activity() {

    private val backgroundPhotoCode = 1001
    private val flowPhotoCode = 1002

    private lateinit var backgroundPreview: ImageView
    private lateinit var flowPreview: ImageView
    private lateinit var status: TextView
    private lateinit var shapeSpinner: Spinner
    private lateinit var speedSeekBar: SeekBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        backgroundPreview =
            findViewById(R.id.background_preview)

        flowPreview =
            findViewById(R.id.flow_preview)

        status =
            findViewById(R.id.status_text)

        shapeSpinner =
            findViewById(R.id.shape_spinner)

        speedSeekBar =
            findViewById(R.id.speed_seekbar)

        val chooseBackground =
            findViewById<Button>(
                R.id.choose_background_button
            )

        val chooseFlow =
            findViewById<Button>(
                R.id.choose_flow_button
            )

        val previewButton =
            findViewById<Button>(
                R.id.preview_button
            )

        val setWallpaperButton =
            findViewById<Button>(
                R.id.set_wallpaper_button
            )

        setupShapeSpinner()

        loadSavedPhotos()
        loadSavedSettings()

        chooseBackground.setOnClickListener {
            choosePhoto(backgroundPhotoCode)
        }

        chooseFlow.setOnClickListener {
            choosePhoto(flowPhotoCode)
        }

        speedSeekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {

                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    saveSpeed(progress)
                }

                override fun onStartTrackingTouch(
                    seekBar: SeekBar?
                ) {
                }

                override fun onStopTrackingTouch(
                    seekBar: SeekBar?
                ) {
                }
            }
        )

        shapeSpinner.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    saveShape(position)
                }

                override fun onNothingSelected(
                    parent: android.widget.AdapterView<*>?
                ) {
                }
            }

        previewButton.setOnClickListener {
            status.text =
                "Preview will be available with the wallpaper engine."
        }

        setWallpaperButton.setOnClickListener {
            openLiveWallpaper()
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

        val adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                shapes
            )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        shapeSpinner.adapter = adapter
    }

    private fun choosePhoto(requestCode: Int) {

        val intent =
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {

                type = "image/*"

                addCategory(
                    Intent.CATEGORY_OPENABLE
                )

                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                addFlags(
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                )
            }

        startActivityForResult(
            intent,
            requestCode
        )
    }

    @Deprecated(
        "Using Activity Result for maximum compatibility"
    )
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

        if (resultCode != RESULT_OK) {
            return
        }

        val uri =
            data?.data ?: return

        try {

            contentResolver
                .takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

        } catch (_: Exception) {
        }

        when (requestCode) {

            backgroundPhotoCode -> {

                saveBackgroundPhoto(uri)

                showPhoto(
                    uri,
                    backgroundPreview
                )

                status.text =
                    "Background photo selected ✓"
            }

            flowPhotoCode -> {

                saveFlowPhoto(uri)

                showPhoto(
                    uri,
                    flowPreview
                )

                status.text =
                    "Flow photo selected ✓"
            }
        }
    }

    private fun saveBackgroundPhoto(
        uri: Uri
    ) {

        getSharedPreferences(
            "LiveFlow",
            MODE_PRIVATE
        )
            .edit()
            .putString(
                "background_uri",
                uri.toString()
            )
            .apply()
    }

    private fun saveFlowPhoto(
        uri: Uri
    ) {

        getSharedPreferences(
            "LiveFlow",
            MODE_PRIVATE
        )
            .edit()
            .putString(
                "flow_uri",
                uri.toString()
            )
            .apply()
    }

    private fun saveShape(
        position: Int
    ) {

        getSharedPreferences(
            "LiveFlow",
            MODE_PRIVATE
        )
            .edit()
            .putInt(
                "shape_mode",
                position
            )
            .apply()
    }

    private fun saveSpeed(
        progress: Int
    ) {

        getSharedPreferences(
            "LiveFlow",
            MODE_PRIVATE
        )
            .edit()
            .putInt(
                "speed",
                progress
            )
            .apply()
    }

    private fun loadSavedPhotos() {

        val prefs =
            getSharedPreferences(
                "LiveFlow",
                MODE_PRIVATE
            )

        val background =
            prefs.getString(
                "background_uri",
                null
            )

        val flow =
            prefs.getString(
                "flow_uri",
                null
            )

        if (background != null) {

            showPhoto(
                Uri.parse(background),
                backgroundPreview
            )
        }

        if (flow != null) {

            showPhoto(
                Uri.parse(flow),
                flowPreview
            )
        }
    }

    private fun loadSavedSettings() {

        val prefs =
            getSharedPreferences(
                "LiveFlow",
                MODE_PRIVATE
            )

        val shape =
            prefs.getInt(
                "shape_mode",
                0
            )

        val speed =
            prefs.getInt(
                "speed",
                50
            )

        shapeSpinner.setSelection(shape)

        speedSeekBar.progress = speed
    }

    private fun showPhoto(
        uri: Uri,
        imageView: ImageView
    ) {

        try {

            val stream =
                contentResolver
                    .openInputStream(uri)

            val bitmap =
                BitmapFactory
                    .decodeStream(stream)

            stream?.close()

            if (bitmap != null) {

                imageView.setImageBitmap(
                    bitmap
                )
            }

        } catch (_: Exception) {
        }
    }

    private fun openLiveWallpaper() {

        try {

            val intent =
                Intent(
                    WallpaperManager
                        .ACTION_CHANGE_LIVE_WALLPAPER
                ).apply {

                    putExtra(
                        WallpaperManager
                            .EXTRA_LIVE_WALLPAPER_COMPONENT,

                        ComponentName(
                            this@MainActivity,
                            FlowWallpaperService::class.java
                        )
                    )
                }

            startActivity(intent)

        } catch (_: Exception) {

            startActivity(
                Intent(
                    WallpaperManager
                        .ACTION_LIVE_WALLPAPER_CHOOSER
                )
            )
        }
    }
}
