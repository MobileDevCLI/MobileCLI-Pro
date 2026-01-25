package com.termux.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.view.ViewGroup
import android.view.Gravity
import androidx.appcompat.app.AppCompatActivity

/**
 * SettingsActivity - MobileCLI settings screen.
 *
 * Shows app info, support contact, and settings.
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

            // Title
            addView(TextView(context).apply {
                text = "Settings"
                textSize = 24f
                setTextColor(0xFF1C1B1F.toInt())
            })

            // About Section
            addView(TextView(context).apply {
                text = "About"
                textSize = 18f
                setTextColor(0xFF1C1B1F.toInt())
                setPadding(0, 48, 0, 16)
            })

            addView(TextView(context).apply {
                text = "MobileCLI Pro"
                textSize = 16f
                setTextColor(0xFF49454F.toInt())
            })

            addView(TextView(context).apply {
                text = "Version 2.0.0"
                textSize = 14f
                setTextColor(0xFF79747E.toInt())
                setPadding(0, 8, 0, 0)
            })

            // Support Section
            addView(TextView(context).apply {
                text = "Support"
                textSize = 18f
                setTextColor(0xFF1C1B1F.toInt())
                setPadding(0, 48, 0, 16)
            })

            addView(TextView(context).apply {
                text = "Email: mobiledevcli@gmail.com"
                textSize = 16f
                setTextColor(0xFF6750A4.toInt())
                setPadding(0, 8, 0, 0)
                setOnClickListener {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:mobiledevcli@gmail.com")
                        putExtra(Intent.EXTRA_SUBJECT, "MobileCLI Pro Support")
                    }
                    startActivity(intent)
                }
            })

            addView(TextView(context).apply {
                text = "Website: mobilecli.com"
                textSize = 16f
                setTextColor(0xFF6750A4.toInt())
                setPadding(0, 16, 0, 0)
                setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://mobilecli.com"))
                    startActivity(intent)
                }
            })

            addView(TextView(context).apply {
                text = "GitHub: github.com/MobileDevCLI"
                textSize = 16f
                setTextColor(0xFF6750A4.toInt())
                setPadding(0, 16, 0, 0)
                setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/MobileDevCLI"))
                    startActivity(intent)
                }
            })

            // Subscription Section
            addView(TextView(context).apply {
                text = "Subscription"
                textSize = 18f
                setTextColor(0xFF1C1B1F.toInt())
                setPadding(0, 48, 0, 16)
            })

            addView(TextView(context).apply {
                text = "Manage Subscription"
                textSize = 16f
                setTextColor(0xFF6750A4.toInt())
                setPadding(0, 8, 0, 0)
                setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.com/myaccount/autopay/"))
                    startActivity(intent)
                }
            })

            addView(TextView(context).apply {
                text = "Cancel anytime via PayPal"
                textSize = 14f
                setTextColor(0xFF79747E.toInt())
                setPadding(0, 8, 0, 0)
            })

            // Legal Section
            addView(TextView(context).apply {
                text = "Legal"
                textSize = 18f
                setTextColor(0xFF1C1B1F.toInt())
                setPadding(0, 48, 0, 16)
            })

            addView(TextView(context).apply {
                text = "Terms of Service"
                textSize = 16f
                setTextColor(0xFF6750A4.toInt())
                setPadding(0, 8, 0, 0)
                setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://mobilecli.com/terms.html"))
                    startActivity(intent)
                }
            })

            addView(TextView(context).apply {
                text = "Privacy Policy"
                textSize = 16f
                setTextColor(0xFF6750A4.toInt())
                setPadding(0, 16, 0, 0)
                setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://mobilecli.com/privacy.html"))
                    startActivity(intent)
                }
            })

            addView(TextView(context).apply {
                text = "Refund Policy: 7-day full refund, no questions asked"
                textSize = 14f
                setTextColor(0xFF79747E.toInt())
                setPadding(0, 24, 0, 0)
            })
        }

        setContentView(layout)
    }
}
