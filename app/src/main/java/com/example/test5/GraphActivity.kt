package com.example.test5

import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.test5.data.PurchaseDatabase
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.DefaultValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GraphActivity : AppCompatActivity() {

    private lateinit var database: PurchaseDatabase
    private lateinit var pieChart: PieChart
    private var showPercentages: Boolean = true // Флаг переключения

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_graph)

        database = PurchaseDatabase.getDatabase(this)
        pieChart = findViewById(R.id.pieChart)



        val backButton: ImageButton = findViewById(R.id.btn_back)
        backButton.setOnClickListener {
            finish() // Завершаем активность и возвращаемся на предыдущий экран
        }
        // Загружаем данные для диаграммы
        loadExpenseData()

        val toggleViewButton = findViewById<Button>(R.id.btn_toggle_view)
        toggleViewButton.setOnClickListener {
            toggleDataView(toggleViewButton)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish() // Завершаем текущую активность и возвращаемся
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun loadExpenseData() {
        CoroutineScope(Dispatchers.IO).launch {
            // Получаем данные о расходах по категориям
            val expenses = database.purchaseDao().getCategoryExpenses()

            // Преобразуем данные в формат, подходящий для диаграммы
            val totalAmount = expenses.sumOf { it.totalAmount }
            val entries = expenses.map { categoryExpense ->
                val label = categoryExpense.category
                val value = if (showPercentages) {
                    (categoryExpense.totalAmount / totalAmount * 100).toFloat()
                } else {
                    categoryExpense.totalAmount.toFloat()
                }
                PieEntry(value, label)
            }

            // Обновляем диаграмму в главном потоке
            withContext(Dispatchers.Main) {
                setPieChartData(entries, totalAmount)
            }
        }
    }

    private fun setPieChartData(entries: List<PieEntry>, totalAmount: Double) {
        val dataSet = PieDataSet(entries, if (showPercentages) "Покупки (%)" else "Покупки ($totalAmount)")

        // Настроим цвета
        dataSet.setColors(*ColorTemplate.MATERIAL_COLORS)

        // Настроим размер текста для подписей внутри графика
        dataSet.valueTextSize = 12f // Уменьшаем размер текста для подписей
        dataSet.valueTextColor = Color.BLACK

        val pieData = PieData(dataSet)

        // Устанавливаем правильный valueFormatter для процентов или валюты
        pieData.setValueFormatter(
            if (showPercentages) {
                PercentFormatter(pieChart) // Форматируем как %
            } else {
                DefaultValueFormatter(0) // Форматируем как целое число
            }
        )

        pieChart.data = pieData

        // Дополнительные настройки диаграммы
        pieChart.description.isEnabled = false // Отключаем описание
        pieChart.legend.isEnabled = true // Включаем легенду

        // Размещение легенды по бокам
        pieChart.legend.orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.VERTICAL
        pieChart.legend.verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.TOP
        pieChart.legend.horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.LEFT

        // Настроим легенду
        pieChart.legend.textSize = 12f // Уменьшаем размер текста в легенде
        pieChart.legend.formSize = 12f // Уменьшаем размер символов в легенде

        // Настроим текст в центре графика
        pieChart.centerText = "Сумма: $totalAmount"
        pieChart.setCenterTextSize(14f) // Уменьшаем размер текста в центре

        pieChart.animateY(1400, Easing.EaseInOutQuad)
        pieChart.invalidate() // Обновляем диаграмму
    }



    private fun toggleDataView(toggleViewButton: Button) {
        // Переключаем режим отображения
        showPercentages = !showPercentages
        loadExpenseData()

        // Меняем текст кнопки
        toggleViewButton.text = if (showPercentages) "%" else "₽"
    }
}
