package com.example.test5

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class ProfileActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()

        val emailTextView = findViewById<TextView>(R.id.tv_user_email)
        val logoutButton = findViewById<Button>(R.id.btn_logout)

        // Отображаем email пользователя
        val currentUser = auth.currentUser
        emailTextView.text = "Email: ${currentUser?.email}"

        // Выход из аккаунта
        logoutButton.setOnClickListener {
            auth.signOut()
            navigateToLoginScreen()
        }


        val premiumButton = findViewById<Button>(R.id.btn_activate_premium)
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val isPremium = prefs.getBoolean("premium", false)

        if (isPremium) {
            premiumButton.text = "Отменить подписку"
            premiumButton.setBackgroundColor(resources.getColor(android.R.color.holo_red_dark))
        } else {
            premiumButton.text = "Активировать Premium"
            premiumButton.setBackgroundColor(resources.getColor(android.R.color.holo_green_dark))
        }

        premiumButton.setOnClickListener {
            val current = prefs.getBoolean("premium", false)
            prefs.edit().putBoolean("premium", !current).apply()

            if (!current) {
                premiumButton.text = "Отменить подписку"
                premiumButton.setBackgroundColor(resources.getColor(android.R.color.holo_red_dark))
                Toast.makeText(this, "Premium активирован", Toast.LENGTH_SHORT).show()
            } else {
                premiumButton.text = "Активировать Premium"
                premiumButton.setBackgroundColor(resources.getColor(android.R.color.holo_green_dark))
                Toast.makeText(this, "Premium отключён", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun navigateToLoginScreen() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}
