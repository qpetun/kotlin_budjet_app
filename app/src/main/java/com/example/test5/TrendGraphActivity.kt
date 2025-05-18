package com.example.test5

import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.test5.data.PurchaseDatabase
import com.example.test5.models.Purchase
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.DefaultValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class TrendGraphActivity : AppCompatActivity() {

    private lateinit var lineChart: LineChart
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
        setContentView(R.layout.activity_trend_graph)

        database = PurchaseDatabase.getDatabase(this)
        lineChart = findViewById(R.id.lineChart)
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
                    updateChart()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        toggleButton.setOnClickListener {
            showPercentages = !showPercentages
            toggleButton.text = if (showPercentages) "%" else "₽"
            updateChart()
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
                updateChart()
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun loadPurchases() {
        CoroutineScope(Dispatchers.IO).launch {
            allPurchases = database.purchaseDao().getAll()
            withContext(Dispatchers.Main) { updateChart() }
        }
    }

    private fun updateChart() {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val grouped = TreeMap<String, Double>()

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
            grouped[purchase.date] = grouped.getOrDefault(purchase.date, 0.0) + purchase.price
        }

        val total = grouped.values.sum().takeIf { it > 0 } ?: 1.0

        val entries = grouped.entries.mapIndexed { index, (dateStr, value) ->
            val y = if (showPercentages) (value / total * 100).toFloat() else value.toFloat()
            Entry(index.toFloat(), y)
        }

        val label = if (showPercentages) "Динамика (%)" else "Динамика (₽)"
        val dataSet = LineDataSet(entries, label)
        dataSet.color = Color.parseColor("#2E3A59")
        dataSet.valueTextColor = Color.BLACK
        dataSet.valueTextSize = 10f
        dataSet.setCircleColor(Color.parseColor("#2E3A59"))
        dataSet.lineWidth = 2f
        dataSet.circleRadius = 4f
        dataSet.setDrawValues(true)
        dataSet.setDrawFilled(true)
        dataSet.fillAlpha = 50
        if (showPercentages) dataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String = "%.1f%%".format(value)
        } else dataSet.valueFormatter = DefaultValueFormatter(0)

        val lineData = LineData(dataSet)

        lineChart.data = lineData
        lineChart.description.isEnabled = false
        lineChart.axisRight.isEnabled = false
        lineChart.axisLeft.textColor = Color.BLACK
        lineChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        lineChart.xAxis.textColor = Color.BLACK
        lineChart.xAxis.setDrawGridLines(false)
        lineChart.xAxis.granularity = 1f
        lineChart.xAxis.labelRotationAngle = -45f
        lineChart.xAxis.valueFormatter = DateAxisFormatter(grouped.keys.toList())
        lineChart.legend.isEnabled = false

        val marker = TrendMarkerView(this, grouped.keys.toList())
        marker.chartView = lineChart
        lineChart.marker = marker

        lineChart.invalidate()
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

class DateAxisFormatter(private val dates: List<String>) : ValueFormatter() {
    override fun getFormattedValue(value: Float): String {
        val index = value.toInt().coerceIn(0, dates.size - 1)
        return try {
            val raw = dates[index]
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(raw)
            SimpleDateFormat("dd.MM", Locale.getDefault()).format(date!!)
        } catch (e: Exception) {
            ""
        }
    }
}

class TrendMarkerView(context: Context, private val dates: List<String>) : MarkerView(context, R.layout.marker_view) {
    private val tvContent: TextView = findViewById(R.id.tvMarkerContent)
    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        if (e != null) {
            val date = dates.getOrNull(e.x.toInt()) ?: ""
            val value = "%.2f".format(e.y)
            tvContent.text = "$date\n$value"
        }
        super.refreshContent(e, highlight)
    }
    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2).toFloat(), -height.toFloat())
    }
}
