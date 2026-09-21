package com.example.liveflowwallpaper

import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val setButton = findViewById<Button>(R.id.set_wallpaper_button)
        val openButton = findViewById<Button>(R.id.open_picker_button)
        val status = findViewById<TextView>(R.id.status_text)

        setButton.setOnClickListener {
            try {
                val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                    putExtra(
                        WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                        ComponentName(this@MainActivity, FlowWallpaperService::class.java)
                    )
                }
                startActivity(intent)
            } catch (_: Exception) {
                startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
            }
        }

        openButton.setOnClickListener {
            startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
        }
    }
}
