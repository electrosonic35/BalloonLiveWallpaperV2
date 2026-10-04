package com.example.balloonwallpaper

import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(48, 72, 48, 48)
        }

        val title = TextView(this).apply {
            text = "Balloon Live Wallpaper"
            textSize = 28f
            gravity = Gravity.CENTER
        }

        val description = TextView(this).apply {
            text = "\nVersion 2.0\n\nA cartoon green balloon floats around a black home-screen background using a simple physics simulation. The wallpaper stays underneath your launcher icons, so your normal home-screen controls remain usable."
            textSize = 17f
            gravity = Gravity.CENTER
        }

        val setButton = Button(this).apply {
            text = "Set Balloon Wallpaper"
            setOnClickListener { openWallpaperPicker() }
        }

        root.addView(title, LinearLayout.LayoutParams(-1, -2))
        root.addView(description, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(setButton, LinearLayout.LayoutParams(-1, -2))

        setContentView(root)
    }

    private fun openWallpaperPicker() {
        val component = ComponentName(this, BalloonWallpaperService::class.java)
        val directIntent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, component)
        }

        try {
            startActivity(directIntent)
        } catch (_: Exception) {
            startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
        }
    }
}
