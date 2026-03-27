package com.example.fridgemanager.ui.screens.stats

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.fridgemanager.ui.viewmodel.FoodViewModel
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

@Composable
fun StatsScreen(viewModel: FoodViewModel) {
    val state by viewModel.statsState.collectAsState()
    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            "食材统计",
            style = MaterialTheme.typography.headlineSmall
        )

        // ── 折线图：近12个月过期/丢弃趋势 ─────────────────────────
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "近12个月过期/丢弃趋势",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))

                if (state.monthlyData.isEmpty()) {
                    EmptyChartHint("暂无消耗记录")
                } else {
                    AndroidView(
                        factory = { ctx ->
                            LineChart(ctx).apply {
                                description.isEnabled = false
                                setTouchEnabled(true)
                                isDragEnabled = true
                                setScaleEnabled(false)
                                legend.isEnabled = false
                                axisRight.isEnabled = false
                                xAxis.position = XAxis.XAxisPosition.BOTTOM
                                xAxis.granularity = 1f
                                xAxis.setDrawGridLines(false)
                                axisLeft.setDrawGridLines(true)
                                animateX(800)
                            }
                        },
                        update = { chart ->
                            val labels = state.monthlyData.map { it.first.takeLast(5) }
                            val entries = state.monthlyData.mapIndexed { i, (_, count) ->
                                Entry(i.toFloat(), count.toFloat())
                            }
                            val dataSet = LineDataSet(entries, "过期/丢弃").apply {
                                color = primaryColor
                                setCircleColor(primaryColor)
                                lineWidth = 2f
                                circleRadius = 4f
                                valueTextSize = 10f
                                mode = LineDataSet.Mode.CUBIC_BEZIER
                            }
                            chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                            chart.data = LineData(dataSet)
                            chart.invalidate()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    )
                }
            }
        }

        // ── 饼图：当前库存分类占比 ─────────────────────────────────
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "当前库存分类占比",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))

                if (state.categoryData.isEmpty()) {
                    EmptyChartHint("冰箱目前是空的")
                } else {
                    AndroidView(
                        factory = { ctx ->
                            PieChart(ctx).apply {
                                description.isEnabled = false
                                isDrawHoleEnabled = true
                                holeRadius = 48f
                                setHoleColor(AndroidColor.TRANSPARENT)
                                transparentCircleRadius = 52f
                                setDrawEntryLabels(true)
                                setEntryLabelTextSize(11f)
                                legend.isEnabled = true
                                animateY(800)
                            }
                        },
                        update = { chart ->
                            val palette = intArrayOf(
                                0xFF4CAF50.toInt(), 0xFF2196F3.toInt(), 0xFFFF9800.toInt(), 0xFFE91E63.toInt(),
                                0xFF9C27B0.toInt(), 0xFF00BCD4.toInt(), 0xFFFF5722.toInt(), 0xFF607D8B.toInt()
                            )

                            val entries = state.categoryData.mapIndexed { _, (name, count) ->
                                PieEntry(count.toFloat(), name)
                            }
                            val dataSet = PieDataSet(entries, "分类").apply {
                                // 显式循环调色板，支持超过 8 个分类
                                setColors(IntArray(entries.size) { i -> palette[i % palette.size] }.toList())
                                valueTextSize = 12f
                                valueTextColor = AndroidColor.WHITE
                                sliceSpace = 2f
                            }
                            chart.data = PieData(dataSet)
                            chart.invalidate()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                    )
                }
            }
        }

        // 总计数字
        if (state.categoryData.isNotEmpty()) {
            val total = state.categoryData.sumOf { it.second }
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem("当前库存", "$total 件")
                    StatItem("品类数", "${state.categoryData.size} 类")
                    StatItem("历史消耗", "${state.monthlyData.sumOf { it.second }} 件")
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmptyChartHint(text: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
