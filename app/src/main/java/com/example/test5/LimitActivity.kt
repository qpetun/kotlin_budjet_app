package com.example.test5

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject

class LimitActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_limit)

        val container = findViewById<LinearLayout>(R.id.limitsContainer)
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val limitsJson = prefs.getString("category_limits", "{}")
        val limitsMap = JSONObject(limitsJson ?: "{}")

        val categories = resources.getStringArray(R.array.categories)
        val inputMap = mutableMapOf<String, EditText>()

        for (category in categories.drop(1)) { // пропускаем "Выберите категорию"
            val row = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, 16) }
                orientation = LinearLayout.HORIZONTAL
            }

            val label = TextView(this).apply {
                text = category
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }

            val input = EditText(this).apply {
                hint = "Лимит"
                inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                setText(limitsMap.optDouble(category, 0.0).takeIf { it > 0 }?.toString() ?: "")
            }

            inputMap[category] = input

            row.addView(label)
            row.addView(input)
            container.addView(row)
        }

        val saveAllButton = Button(this).apply {
            text = "Сохранить лимиты"
            setOnClickListener {
                for ((category, input) in inputMap) {
                    val limit = input.text.toString().toDoubleOrNull()
                    if (limit != null && limit > 0) {
                        limitsMap.put(category, limit)
                    }
                }
                prefs.edit().putString("category_limits", limitsMap.toString()).apply()
                Toast.makeText(this@LimitActivity, "Лимиты сохранены", Toast.LENGTH_SHORT).show()
            }
        }

        val resetButton = Button(this).apply {
            text = "Сбросить все лимиты"
            setOnClickListener {
                prefs.edit().remove("category_limits").apply()
                Toast.makeText(this@LimitActivity, "Все лимиты сброшены", Toast.LENGTH_SHORT).show()
                recreate()
            }
        }

        container.addView(saveAllButton)
        container.addView(resetButton)
    }
}
