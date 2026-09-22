package com.example.liveflowwallpaper

import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView

class MainActivity : Activity() {

    private val pickPhotoCode = 1001

    private lateinit var preview: ImageView
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        preview = findViewById(R.id.photo_preview)
        status = findViewById(R.id.status_text)

        val chooseButton =
            findViewById<Button>(R.id.choose_photo_button)

        val setButton =
            findViewById<Button>(R.id.set_wallpaper_button)

        loadSavedPhoto()

        chooseButton.setOnClickListener {
            choosePhoto()
        }

        setButton.setOnClickListener {
            openLiveWallpaper()
        }
    }

    private fun choosePhoto() {

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }

        startActivityForResult(intent, pickPhotoCode)
    }

    @Deprecated("Using Activity Result for maximum compatibility")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != pickPhotoCode ||
            resultCode != RESULT_OK
        ) {
            return
        }

        val uri = data?.data ?: return

        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {
        }

        getSharedPreferences(
            "LiveFlow",
            MODE_PRIVATE
        )
            .edit()
            .putString("photo_uri", uri.toString())
            .apply()

        showPhoto(uri)

        status.text = "Photo selected ✓"
    }

    private fun loadSavedPhoto() {

        val saved =
            getSharedPreferences(
                "LiveFlow",
                MODE_PRIVATE
            )
                .getString("photo_uri", null)

        if (saved != null) {
            try {
                showPhoto(Uri.parse(saved))
                status.text = "Photo selected ✓"
            } catch (_: Exception) {
            }
        }
    }

    private fun showPhoto(uri: Uri) {

        try {
            val stream =
                contentResolver.openInputStream(uri)

            val bitmap =
                BitmapFactory.decodeStream(stream)

            stream?.close()

            if (bitmap != null) {
                preview.setImageBitmap(bitmap)
            }

        } catch (_: Exception) {
            status.text = "Could not load photo"
        }
    }

    private fun openLiveWallpaper() {

        try {

            val intent =
                Intent(
                    WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER
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
