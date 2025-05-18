package com.example.test5

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.test5.data.PurchaseDatabase
import com.example.test5.models.Purchase
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.DefaultValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class WeekdayGraphActivity : AppCompatActivity() {

    private lateinit var barChart: BarChart
    private lateinit var database: PurchaseDatabase
    private lateinit var dateFilter: Spinner
    private lateinit var customDateLabel: TextView
    private lateinit var toggleButton: Button
    private var allPurchases: List<Purchase> = emptyList()
    private var selectedPeriod: String = "Всё время"
    private var customStart: Date? = null
    private var customEnd: Date? = null
    private var showPercentages: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weekday_graph)

        database = PurchaseDatabase.getDatabase(this)
        barChart = findViewById(R.id.barChart)
        dateFilter = findViewById(R.id.dateFilter)
        customDateLabel = findViewById(R.id.customDateLabel)
        toggleButton = findViewById(R.id.btn_toggle_view)

        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }

        val periods = listOf("Всё время", "За неделю", "За месяц", "Произвольный диапазон")
        dateFilter.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, periods).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        dateFilter.setSelection(0)

        dateFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedPeriod = periods[position]
                if (selectedPeriod == "Произвольный диапазон") {
                    openDateRangePicker()
                } else {
                    customDateLabel.visibility = View.GONE
                    updateFilteredChart()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        toggleButton.setOnClickListener {
            showPercentages = !showPercentages
            toggleButton.text = if (showPercentages) "%" else "₽"
            updateFilteredChart()
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
                updateFilteredChart()
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun loadPurchases() {
        CoroutineScope(Dispatchers.IO).launch {
            allPurchases = database.purchaseDao().getAll()
            withContext(Dispatchers.Main) { updateFilteredChart() }
        }
    }

    private fun updateFilteredChart() {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
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

        val weekdayMap = TreeMap<Int, Double>()
        for (purchase in filtered) {
            val date = format.parse(purchase.date) ?: continue
            val calendar = Calendar.getInstance().apply { time = date }
            val weekday = calendar.get(Calendar.DAY_OF_WEEK) // 1 (Sunday) to 7 (Saturday)
            val weekdayIndex = if (weekday == 1) 6 else weekday - 2 // 0 (Mon) to 6 (Sun)
            weekdayMap[weekdayIndex] = weekdayMap.getOrDefault(weekdayIndex, 0.0) + purchase.price
        }

        val total = weekdayMap.values.sum().takeIf { it > 0 } ?: 1.0

        val entries = weekdayMap.map { (index, value) ->
            val y = if (showPercentages) (value / total * 100).toFloat() else value.toFloat()
            BarEntry(index.toFloat(), y)
        }

        val label = if (showPercentages) "Траты (%)" else "Траты (₽)"
        val dataSet = BarDataSet(entries, label)
        dataSet.color = Color.parseColor("#2E3A59")
        dataSet.valueTextColor = Color.BLACK
        dataSet.valueTextSize = 12f
        if (showPercentages) dataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String = "%.1f%%".format(value)
        } else dataSet.valueFormatter = DefaultValueFormatter(0)

        val barData = BarData(dataSet)
        barData.barWidth = 0.9f

        barChart.data = barData
        barChart.setFitBars(true)
        barChart.description.isEnabled = false
        barChart.axisRight.isEnabled = false
        barChart.axisLeft.textColor = Color.BLACK
        barChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        barChart.xAxis.valueFormatter = WeekdayFormatter()
        barChart.xAxis.setDrawGridLines(false)
        barChart.xAxis.granularity = 1f
        barChart.xAxis.textColor = Color.BLACK
        barChart.legend.isEnabled = false
        barChart.invalidate()
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

class WeekdayFormatter : com.github.mikephil.charting.formatter.ValueFormatter() {
    private val days = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
    override fun getFormattedValue(value: Float): String {
        val index = value.toInt().coerceIn(0, 6)
        return days[index]
    }
}
