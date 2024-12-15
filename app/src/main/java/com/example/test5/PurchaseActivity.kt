package com.example.test5


import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.test5.R.id.recyclerView
import com.example.test5.data.PurchaseDatabase
import com.example.test5.models.Purchase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
        val categories = listOf("Electronics", "Clothing", "Groceries", "Other")
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
                Toast.makeText(this, "Fill all fields correctly", Toast.LENGTH_SHORT).show()
            }
        }

        val viewGraphButton = findViewById<Button>(R.id.btn_view_graph)
        viewGraphButton.setOnClickListener {
            val intent = Intent(this, GraphActivity::class.java)
            startActivity(intent)
        }



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
}

