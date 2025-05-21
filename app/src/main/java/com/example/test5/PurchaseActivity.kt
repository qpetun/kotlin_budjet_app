package com.example.test5

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.test5.data.PurchaseDatabase
import com.example.test5.models.Purchase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class PurchaseActivity : AppCompatActivity() {

    private lateinit var database: PurchaseDatabase
    private lateinit var adapter: PurchaseAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_purchase)

        database = PurchaseDatabase.getDatabase(this)

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val isPremium = prefs.getBoolean("premium", false)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = PurchaseAdapter()
        recyclerView.adapter = adapter

        loadPurchases()

        val addButton = findViewById<Button>(R.id.btn_add)
        val nameEditText = findViewById<EditText>(R.id.et_name)
        val priceEditText = findViewById<EditText>(R.id.et_price)
        val categorySpinner = findViewById<Spinner>(R.id.sp_category)

        val categories = resources.getStringArray(R.array.categories).toList()
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = spinnerAdapter

        val tvSelectedDate = findViewById<TextView>(R.id.tv_selected_date)
        val dateContainer = findViewById<LinearLayout>(R.id.dateContainer)
        val storageFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displayFormat = SimpleDateFormat("d MMMM yyyy", Locale("ru"))
        val calendar = Calendar.getInstance()
        var selectedDate = storageFormat.format(calendar.time)
        tvSelectedDate.text = "Дата: ${displayFormat.format(calendar.time)}"

        dateContainer.setOnClickListener {
            val y = calendar.get(Calendar.YEAR)
            val m = calendar.get(Calendar.MONTH)
            val d = calendar.get(Calendar.DAY_OF_MONTH)
            val dpd = DatePickerDialog(this, { _, year, month, day ->
                calendar.set(year, month, day)
                selectedDate = storageFormat.format(calendar.time)
                tvSelectedDate.text = "Дата: ${displayFormat.format(calendar.time)}"
            }, y, m, d)
            dpd.show()
        }

        val btnGraph = findViewById<Button>(R.id.btn_graph)
        val btnWeekdayGraph = findViewById<Button>(R.id.btn_weekday)
        val btnTrendGraph = findViewById<Button>(R.id.btn_trend_graph)
        val btnTop = findViewById<Button>(R.id.btn_top_categories)
        val profileButton = findViewById<Button>(R.id.btn_profile)
        val btnLimit = findViewById<Button>(R.id.btnSetLimit)

        // Блокировка и затемнение кнопок премиум-функций
        btnGraph.alpha = if (isPremium) 1f else 0.5f
        btnWeekdayGraph.alpha = if (isPremium) 1f else 0.5f
        btnTrendGraph.alpha = if (isPremium) 1f else 0.5f

        btnGraph.setOnClickListener {
            if (isPremium) startActivity(Intent(this, GraphActivity::class.java))
            else Toast.makeText(this, "Функция доступна только Premium пользователям", Toast.LENGTH_SHORT).show()
        }

        btnWeekdayGraph.setOnClickListener {
            if (isPremium) startActivity(Intent(this, WeekdayGraphActivity::class.java))
            else Toast.makeText(this, "Функция доступна только Premium пользователям", Toast.LENGTH_SHORT).show()
        }

        btnTrendGraph.setOnClickListener {
            if (isPremium) startActivity(Intent(this, TrendGraphActivity::class.java))
            else Toast.makeText(this, "Функция доступна только Premium пользователям", Toast.LENGTH_SHORT).show()
        }

        btnTop.setOnClickListener {
            startActivity(Intent(this, TopCategoriesActivity::class.java))
        }

        profileButton.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        btnLimit.setOnClickListener {
            startActivity(Intent(this, LimitActivity::class.java))
        }

        addButton.setOnClickListener {
            val name = nameEditText.text.toString()
            val price = priceEditText.text.toString().toDoubleOrNull()
            val categoryIndex = categorySpinner.selectedItemPosition
            if (categoryIndex == 0) {
                Toast.makeText(this, "Пожалуйста, выберите категорию", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val category = categorySpinner.selectedItem.toString()
            if (name.isNotEmpty() && price != null && selectedDate.isNotEmpty()) {
                val purchase = Purchase(name = name, price = price, category = category, date = selectedDate)
                addPurchase(purchase)
            } else {
                Toast.makeText(this, "Заполните все поля и выберите дату", Toast.LENGTH_SHORT).show()
            }
        }

        setupSwipeToDelete(recyclerView)
    }

    private fun loadPurchases() {
        CoroutineScope(Dispatchers.IO).launch {
            val purchases = database.purchaseDao().getAllPurchases()
            runOnUiThread {
                adapter.submitList(purchases.reversed())
                val prefs = getSharedPreferences("settings", MODE_PRIVATE)
                val limitsJson = prefs.getString("category_limits", "{}")
                val limits = JSONObject(limitsJson ?: "{}")
                val categoryTotals = purchases.groupBy { it.category }.mapValues { entry -> entry.value.sumOf { it.price } }
                for ((category, total) in categoryTotals) {
                    val limit = limits.optDouble(category, -1.0)
                    if (limit > 0 && total > limit) {
                        val over = total - limit
                        Toast.makeText(this@PurchaseActivity, "Превышение лимита по '$category' на ${"%.2f".format(over)}₽", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun addPurchase(purchase: Purchase) {
        CoroutineScope(Dispatchers.IO).launch {
            database.purchaseDao().insertPurchase(purchase)
            loadPurchases()
        }
    }

    private fun setupSwipeToDelete(recyclerView: RecyclerView) {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            private val background = ColorDrawable(Color.RED)
            private val icon = ContextCompat.getDrawable(this@PurchaseActivity, R.drawable.ic_delete)
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false
            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
                val purchaseToDelete = adapter.getPurchaseAt(vh.adapterPosition)
                deletePurchase(purchaseToDelete)
            }
            override fun onChildDraw(c: Canvas, rv: RecyclerView, vh: RecyclerView.ViewHolder, dX: Float, dY: Float, state: Int, active: Boolean) {
                if (state == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    val itemView = vh.itemView
                    background.setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
                    background.draw(c)
                    icon?.let {
                        val margin = (itemView.height - it.intrinsicHeight) / 2
                        val top = itemView.top + margin
                        val bottom = top + it.intrinsicHeight
                        val left = itemView.right - margin - it.intrinsicWidth
                        val right = itemView.right - margin
                        it.setBounds(left, top, right, bottom)
                        it.draw(c)
                    }
                }
                super.onChildDraw(c, rv, vh, dX, dY, state, active)
            }
        })
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private fun deletePurchase(purchase: Purchase) {
        CoroutineScope(Dispatchers.IO).launch {
            database.purchaseDao().deletePurchase(purchase)
            loadPurchases()
        }
    }
}
