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

        // Настройка цветов
        dataSet.setColors(ColorTemplate.JOYFUL_COLORS.toMutableList())

        // Настройка текста
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = Color.BLACK

        // Установка расстояния между секторами
        dataSet.sliceSpace = 3f // Пробел между секторами

        // Расположение подписей за пределами диаграммы
        dataSet.yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        dataSet.xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE

        // Установка линий связи для подписей
        dataSet.valueLinePart1Length = 0.5f // Длина первой части линии
        dataSet.valueLinePart2Length = 0.3f // Длина второй части линии
        dataSet.valueLineWidth = 1f // Толщина линий
        dataSet.valueLineColor = Color.BLACK // Цвет линий

        val pieData = PieData(dataSet)

        // Установка форматтера
        pieData.setValueFormatter(
            if (showPercentages) {
                PercentFormatter(pieChart)
            } else {
                DefaultValueFormatter(0)
            }
        )

        pieChart.data = pieData

        // Отключение описания
        pieChart.description.isEnabled = false

        // Настройка легенды
        pieChart.legend.isEnabled = false

        pieChart.legend.textSize = 12f
        pieChart.legend.formSize = 12f
        pieChart.legend.isWordWrapEnabled = true

        // Настройка текста в центре
        pieChart.centerText = "Сумма: $totalAmount"
        pieChart.setCenterTextSize(14f)

        // Анимация
        pieChart.animateY(1400, Easing.EaseInOutQuad)
        pieChart.invalidate()
    }




    private fun toggleDataView(toggleViewButton: Button) {
        // Переключаем режим отображения
        showPercentages = !showPercentages
        loadExpenseData()

        // Меняем текст кнопки
        toggleViewButton.text = if (showPercentages) "%" else "₽"
    }
}
