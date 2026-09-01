package com.darkr.app

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.darkr.app.databinding.ActivitySplashBinding

/**
 * Startup Splash Loading Screen.
 * Runs a 3.5s smooth animated loading process before transitioning into the main dashboard.
 */
@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Smooth entry animation on logo
        binding.imgSplashLogo.alpha = 0f
        binding.imgSplashLogo.scaleX = 0.75f
        binding.imgSplashLogo.scaleY = 0.75f
        binding.tvSplashTitle.alpha = 0f

        binding.imgSplashLogo.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(800)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        binding.tvSplashTitle.animate()
            .alpha(1f)
            .setDuration(800)
            .setStartDelay(250)
            .start()

        // 3.5 seconds total loading delay
        handler.postDelayed({
            binding.tvLoadingStatus.text = "Initializing AMOLED Engine..."
        }, 1200)

        handler.postDelayed({
            binding.tvLoadingStatus.text = "Starting Darkr..."
        }, 2400)

        handler.postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 3500)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
