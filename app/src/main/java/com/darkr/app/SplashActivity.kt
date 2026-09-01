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
 * Renders an animated Darkr emblem and progress loader before launching the main dashboard.
 */
@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Smooth entry animation
        binding.imgSplashLogo.alpha = 0f
        binding.imgSplashLogo.scaleX = 0.8f
        binding.imgSplashLogo.scaleY = 0.8f
        binding.tvSplashTitle.alpha = 0f

        binding.imgSplashLogo.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(700)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        binding.tvSplashTitle.animate()
            .alpha(1f)
            .setDuration(800)
            .setStartDelay(200)
            .start()

        // 1.1s delay to smooth loading transition
        handler.postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 1100)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
