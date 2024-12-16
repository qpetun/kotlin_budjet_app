package com.example.test5


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
import com.example.test5.R.id.recyclerView
import com.example.test5.data.PurchaseDatabase
import com.example.test5.models.Purchase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.graphics.Color

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

        addButton.setOnClickListener {
            val name = nameEditText.text.toString()
            val price = priceEditText.text.toString().toDoubleOrNull()
            val category = categorySpinner.selectedItem.toString()

            if (name.isNotEmpty() && price != null) {
                val purchase = Purchase(name = name, price = price, category = category)
                addPurchase(purchase)
            } else {
                Toast.makeText(this, "Пожалуйста, заполните все поля", Toast.LENGTH_SHORT).show()
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

        setupSwipeToDelete(recyclerView)


    }

    private fun loadPurchases() {
        CoroutineScope(Dispatchers.IO).launch {
            val purchases = database.purchaseDao().getAllPurchases()
            runOnUiThread {
                adapter.submitList(purchases)
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

