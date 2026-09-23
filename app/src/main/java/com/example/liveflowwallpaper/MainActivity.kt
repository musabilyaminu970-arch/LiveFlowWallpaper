package com.example.liveflowwallpaper

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var setWallpaperButton: Button
    private lateinit var openPickerButton: Button

    private val backgroundRequestCode = 101
    private val flowRequestCode = 102

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        setWallpaperButton =
            findViewById(R.id.set_wallpaper_button)

        openPickerButton =
            findViewById(R.id.open_picker_button)

        setWallpaperButton.setOnClickListener {
            openPhotoPicker(backgroundRequestCode)
        }

        openPickerButton.setOnClickListener {
            openPhotoPicker(flowRequestCode)
        }
    }

    private fun openPhotoPicker(requestCode: Int) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)

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

        when (requestCode) {

            backgroundRequestCode -> {

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
            }

            flowRequestCode -> {

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
}
