package com.termux.activities

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity

/**
 * SettingsActivity - MobileCLI settings screen.
 *
 * Placeholder for future settings implementation.
 */
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setPadding(48, 48, 48, 48)
            setBackgroundColor(0xFFFFFFFF.toInt())

            addView(TextView(context).apply {
                text = "Settings"
                textSize = 24f
                setTextColor(0xFF1C1B1F.toInt())
            })

            addView(TextView(context).apply {
                text = "Settings coming soon..."
                textSize = 16f
                setTextColor(0xFF49454F.toInt())
                setPadding(0, 24, 0, 0)
            })
        }

        setContentView(layout)
    }
}
