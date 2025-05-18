package com.example.test5

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.test5.models.Purchase
import com.example.test5.data.PurchaseDatabase
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.DefaultValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class GraphActivity : AppCompatActivity() {

    private lateinit var database: PurchaseDatabase
    private lateinit var pieChart: PieChart
    private lateinit var allPurchases: List<Purchase>
    private var showPercentages: Boolean = true
    private var selectedPeriod: String = "Всё время"
    private var customStart: Date? = null
    private var customEnd: Date? = null
    private lateinit var customDateLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_graph)

        database = PurchaseDatabase.getDatabase(this)
        pieChart = findViewById(R.id.pieChart)
        customDateLabel = findViewById(R.id.customDateLabel)

        val backButton: ImageButton = findViewById(R.id.btn_back)
        backButton.setOnClickListener { finish() }

        val toggleViewButton = findViewById<Button>(R.id.btn_toggle_view)
        toggleViewButton.setOnClickListener { toggleDataView(toggleViewButton) }

        val dateFilter = findViewById<Spinner>(R.id.dateFilter)
        val periods = listOf("Всё время", "За неделю", "За месяц", "Произвольный диапазон")
        dateFilter.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, periods).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        dateFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                selectedPeriod = periods[pos]
                if (selectedPeriod == "Произвольный диапазон") {
                    openDateRangePicker()
                } else {
                    customDateLabel.visibility = View.GONE
                    updateFilteredChart()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        loadExpenseData()
    }

    private fun openDateRangePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(this, { _, startYear, startMonth, startDay ->
            val start = Calendar.getInstance().apply {
                set(startYear, startMonth, startDay)
            }
            DatePickerDialog(this, { _, endYear, endMonth, endDay ->
                val end = Calendar.getInstance().apply {
                    set(endYear, endMonth, endDay)
                }
                customStart = start.time
                customEnd = end.time

                val format = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                val label = "Период: ${format.format(customStart!!)} — ${format.format(customEnd!!)}"
                customDateLabel.text = label
                customDateLabel.visibility = View.VISIBLE

                updateFilteredChart()
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun loadExpenseData() {
        CoroutineScope(Dispatchers.IO).launch {
            allPurchases = database.purchaseDao().getAll()
            withContext(Dispatchers.Main) { updateFilteredChart() }
        }
    }

    private fun updateFilteredChart() {
        val filtered = allPurchases.filter { purchase ->
            val inPeriod = when (selectedPeriod) {
                "Всё время" -> true
                "За неделю" -> isInLastDays(purchase.date, 7)
                "За месяц" -> isInLastDays(purchase.date, 30)
                "Произвольный диапазон" -> isInCustomRange(purchase.date)
                else -> true
            }
            inPeriod
        }

        val grouped: Map<String, Double> = filtered.groupBy { it.category }.mapValues { entry ->
            entry.value.sumOf { it.price }
        }

        val totalAmount = grouped.values.sum()
        val entries = grouped.map { (category, total) ->
            val value = if (showPercentages) (total / totalAmount * 100).toFloat() else total.toFloat()
            PieEntry(value, category)
        }

        setPieChartData(entries, totalAmount)
    }

    private fun isInLastDays(dateStr: String, days: Int): Boolean {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = format.parse(dateStr)
            val now = Calendar.getInstance()
            val cutoff = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -days) }
            date != null && date.after(cutoff.time) && date.before(now.time)
        } catch (e: Exception) {
            false
        }
    }

    private fun isInCustomRange(dateStr: String): Boolean {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = format.parse(dateStr)
            customStart != null && customEnd != null && date != null && !date.before(customStart) && !date.after(customEnd)
        } catch (e: Exception) {
            false
        }
    }

    private fun setPieChartData(entries: List<PieEntry>, totalAmount: Double) {
        val dataSet = PieDataSet(entries, if (showPercentages) "Покупки (%)" else "Покупки ($totalAmount)")
        dataSet.setColors(ColorTemplate.JOYFUL_COLORS.toMutableList())
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = Color.BLACK
        dataSet.sliceSpace = 3f
        dataSet.yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        dataSet.xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        dataSet.valueLinePart1Length = 0.5f
        dataSet.valueLinePart2Length = 0.3f
        dataSet.valueLineWidth = 1f
        dataSet.valueLineColor = Color.BLACK

        val pieData = PieData(dataSet)
        pieData.setValueFormatter(if (showPercentages) PercentFormatter(pieChart) else DefaultValueFormatter(0))

        pieChart.data = pieData
        pieChart.description.isEnabled = false
        pieChart.legend.isEnabled = false
        pieChart.centerText = "Сумма: $totalAmount"
        pieChart.setCenterTextSize(14f)
        pieChart.animateY(1000, Easing.EaseInOutQuad)
        pieChart.invalidate()
        pieChart.setEntryLabelColor(Color.BLACK)
        pieChart.setCenterTextColor(Color.BLACK)
        pieChart.legend.textColor = Color.BLACK
    }

    private fun toggleDataView(toggleViewButton: Button) {
        showPercentages = !showPercentages
        updateFilteredChart()
        toggleViewButton.text = if (showPercentages) "%" else "₽"
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
