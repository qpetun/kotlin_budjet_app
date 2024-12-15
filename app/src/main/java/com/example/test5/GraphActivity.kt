package com.example.test5

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.test5.R
import com.example.test5.data.CategoryExpense
import com.example.test5.data.PurchaseDatabase
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GraphActivity : AppCompatActivity() {

    private lateinit var database: PurchaseDatabase
    private lateinit var pieChart: PieChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_graph)

        database = PurchaseDatabase.getDatabase(this)
        pieChart = findViewById(R.id.pieChart)

        loadExpenseData()
    }

    private fun loadExpenseData() {
        CoroutineScope(Dispatchers.IO).launch {
            // Получаем данные о расходах по категориям
            val expenses = database.purchaseDao().getCategoryExpenses()

            // Преобразуем данные в формат, подходящий для диаграммы
            val entries = expenses.map { categoryExpense ->
                PieEntry(categoryExpense.totalAmount.toFloat(), categoryExpense.category)
            }

            // Обновляем диаграмму в главном потоке
            withContext(Dispatchers.Main) {
                setPieChartData(entries)
            }
        }
    }

    private fun setPieChartData(entries: List<PieEntry>) {
        val dataSet = PieDataSet(entries, "Expenses by Category")

        // Настроим цвета
        dataSet.setColors(*ColorTemplate.MATERIAL_COLORS)

        // Добавим описание
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = Color.BLACK

        val pieData = PieData(dataSet)
        pieChart.data = pieData

        // Дополнительные настройки диаграммы
        pieChart.description.isEnabled = false // Отключаем описание
        pieChart.legend.isEnabled = true // Включаем легенду
        pieChart.animateY(1400, Easing.EaseInOutQuad) // Анимация

        pieChart.invalidate() // Обновляем диаграмму
    }

}
