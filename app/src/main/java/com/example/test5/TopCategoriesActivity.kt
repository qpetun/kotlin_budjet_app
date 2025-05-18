package com.example.test5

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.test5.data.PurchaseDatabase
import com.example.test5.models.Purchase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class TopCategoriesActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var database: PurchaseDatabase
    private lateinit var dateFilter: Spinner
    private lateinit var customDateLabel: TextView
    private lateinit var toggleButton: Button
    private var allPurchases: List<Purchase> = emptyList()
    private var selectedPeriod: String = "Всё время"
    private var customStart: Date? = null
    private var customEnd: Date? = null
    private var showPercentages: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_top_categories)

        database = PurchaseDatabase.getDatabase(this)
        listView = findViewById(R.id.categoryList)
        dateFilter = findViewById(R.id.dateFilter)
        customDateLabel = findViewById(R.id.customDateLabel)
        toggleButton = findViewById(R.id.btn_toggle_view)

        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }

        val periods = listOf("Всё время", "За неделю", "За месяц", "Произвольный диапазон")
        dateFilter.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, periods).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        dateFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedPeriod = periods[position]
                if (selectedPeriod == "Произвольный диапазон") {
                    openDateRangePicker()
                } else {
                    customDateLabel.visibility = View.GONE
                    updateList()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        toggleButton.setOnClickListener {
            showPercentages = !showPercentages
            toggleButton.text = if (showPercentages) "%" else "₽"
            updateList()
        }

        loadPurchases()
    }

    private fun openDateRangePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(this, { _, startYear, startMonth, startDay ->
            val start = Calendar.getInstance().apply { set(startYear, startMonth, startDay) }
            DatePickerDialog(this, { _, endYear, endMonth, endDay ->
                val end = Calendar.getInstance().apply { set(endYear, endMonth, endDay) }
                customStart = start.time
                customEnd = end.time
                val format = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                customDateLabel.text = "Период: ${format.format(customStart!!)} — ${format.format(customEnd!!)}"
                customDateLabel.visibility = View.VISIBLE
                updateList()
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun loadPurchases() {
        CoroutineScope(Dispatchers.IO).launch {
            allPurchases = database.purchaseDao().getAll()
            withContext(Dispatchers.Main) { updateList() }
        }
    }

    private fun updateList() {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val categoryMap = TreeMap<String, Double>()

        val filtered = allPurchases.filter { purchase ->
            val date = try { format.parse(purchase.date) } catch (_: Exception) { null }
            when (selectedPeriod) {
                "Всё время" -> true
                "За неделю" -> date != null && isInLastDays(date, 7)
                "За месяц" -> date != null && isInLastDays(date, 30)
                "Произвольный диапазон" -> date != null && isInCustomRange(date)
                else -> true
            }
        }

        for (purchase in filtered) {
            categoryMap[purchase.category] = categoryMap.getOrDefault(purchase.category, 0.0) + purchase.price
        }

        val sorted = categoryMap.entries.sortedByDescending { it.value }
        val total = sorted.sumOf { it.value }.takeIf { it > 0 } ?: 1.0

        val items = sorted.mapIndexed { index, entry ->
            if (showPercentages) {
                val percent = entry.value / total * 100
                "${index + 1}. ${entry.key} — %.1f%%".format(percent)
            } else {
                "${index + 1}. ${entry.key} — %.0f ₽".format(entry.value)
            }
        }

        listView.adapter = ArrayAdapter(this, R.layout.list_item_category, R.id.textItem, items)

    }

    private fun isInLastDays(date: Date, days: Int): Boolean {
        val now = Calendar.getInstance()
        val cutoff = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -days) }
        return date.after(cutoff.time) && date.before(now.time)
    }

    private fun isInCustomRange(date: Date): Boolean {
        return customStart != null && customEnd != null && !date.before(customStart) && !date.after(customEnd)
    }
}
