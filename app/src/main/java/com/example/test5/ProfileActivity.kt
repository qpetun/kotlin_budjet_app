package com.example.test5

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
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

    }

    private fun navigateToLoginScreen() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}
