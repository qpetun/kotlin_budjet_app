package com.example.test5



import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
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
import android.graphics.Color
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PurchaseActivity : AppCompatActivity() {

    private lateinit var database: PurchaseDatabase
    private lateinit var adapter: PurchaseAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_purchase)

        database = PurchaseDatabase.getDatabase(this)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = PurchaseAdapter()
        recyclerView.adapter = adapter

        loadPurchases()

        val addButton = findViewById<Button>(R.id.btn_add)
        val nameEditText = findViewById<EditText>(R.id.et_name)
        val priceEditText = findViewById<EditText>(R.id.et_price)
        val categorySpinner = findViewById<Spinner>(R.id.sp_category)

        // Заполняем Spinner категориями
        val categories = resources.getStringArray(R.array.categories).toList()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter

        val tvSelectedDate = findViewById<TextView>(R.id.tv_selected_date)
        val dateContainer = findViewById<LinearLayout>(R.id.dateContainer)

        val storageFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())  // для сохранения
        val displayFormat = SimpleDateFormat("d MMMM yyyy", Locale("ru"))         // для отображения


        val calendar = Calendar.getInstance()
        var selectedDate = storageFormat.format(calendar.time)
        val visibleDate = displayFormat.format(calendar.time)

// Установим дату при загрузке
        tvSelectedDate.text = "Дата: $visibleDate"

// Обработка нажатия на поле даты
        dateContainer.setOnClickListener {
            val y = calendar.get(Calendar.YEAR)
            val m = calendar.get(Calendar.MONTH)
            val d = calendar.get(Calendar.DAY_OF_MONTH)

            val dpd = DatePickerDialog(this, { _, year, month, day ->
                calendar.set(year, month, day)
                selectedDate = storageFormat.format(calendar.time)
                val visibleDate = displayFormat.format(calendar.time)
                tvSelectedDate.text = "Дата: $visibleDate"
            }, y, m, d)

            dpd.show()
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





        val viewGraphButton = findViewById<Button>(R.id.btn_view_graph)
        viewGraphButton.setOnClickListener {
            val intent = Intent(this, GraphActivity::class.java)
            startActivity(intent)
        }

        val profileButton = findViewById<Button>(R.id.btn_profile)

        profileButton.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        val btnLimit = findViewById<Button>(R.id.btnSetLimit)
        btnLimit.setOnClickListener {
            val intent = Intent(this, LimitActivity::class.java)
            startActivity(intent)
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

                val categoryTotals = purchases.groupBy { it.category }.mapValues { entry ->
                    entry.value.sumOf { it.price }
                }

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

            private val background = ColorDrawable(Color.RED) // Красный фон
            private val icon = ContextCompat.getDrawable(this@PurchaseActivity, R.drawable.ic_delete) // Иконка удаления

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val purchaseToDelete = adapter.getPurchaseAt(position)
                deletePurchase(purchaseToDelete)
            }

            override fun onChildDraw(
                canvas: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    val itemView = viewHolder.itemView

                    // Рисуем красный фон
                    background.setBounds(
                        itemView.right + dX.toInt(),
                        itemView.top,
                        itemView.right,
                        itemView.bottom
                    )
                    background.draw(canvas)

                    // Рисуем иконку удаления
                    icon?.let {
                        val iconMargin = (itemView.height - it.intrinsicHeight) / 2
                        val iconTop = itemView.top + (itemView.height - it.intrinsicHeight) / 2
                        val iconBottom = iconTop + it.intrinsicHeight
                        val iconLeft = itemView.right - iconMargin - it.intrinsicWidth
                        val iconRight = itemView.right - iconMargin

                        it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                        it.draw(canvas)
                    }
                }

                super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
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

