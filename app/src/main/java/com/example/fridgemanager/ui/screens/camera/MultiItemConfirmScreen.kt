package com.example.fridgemanager.ui.screens.camera

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.fridgemanager.data.model.Category
import com.example.fridgemanager.data.model.FoodItem
import com.example.fridgemanager.ui.viewmodel.FoodViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 每一项识别结果的可编辑状态（Compose 委托属性，字段变化自动触发重组） */
class ConfirmItemState(
    checked: Boolean = true,
    name: String,
    categoryId: Long?,
    val categoryHint: String,
    val quantity: String,
    val expiryInfo: String
) {
    var checked by mutableStateOf(checked)
    var name by mutableStateOf(name)
    var categoryId by mutableStateOf(categoryId)
}

private fun parseDate(text: String): Date? {
    if (text.isBlank()) return null
    val formats = listOf(
        "yyyy/MM/dd", "yyyy-MM-dd", "yyyy.MM.dd",
        "MM/dd/yyyy", "dd/MM/yyyy", "yyyyMMdd"
    )
    for (fmt in formats) {
        try {
            return SimpleDateFormat(fmt, Locale.CHINA).apply { isLenient = false }.parse(text)
        } catch (_: Exception) { }
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiItemConfirmScreen(
    cameraViewModel: CameraViewModel,
    foodViewModel: FoodViewModel,
    onNavigateBack: () -> Unit,
    onDone: () -> Unit
) {
    val cameraState by cameraViewModel.uiState.collectAsState()
    val homeState by foodViewModel.homeState.collectAsState()
    val categories = homeState.categories
    val results = cameraState.recognitionResults
    val imageUri = cameraState.selectedImageUri?.toString()

    // 为每个识别结果创建可编辑状态
    val editStates = remember(results) {
        results.map { result ->
            ConfirmItemState(
                name = result.name,
                categoryId = null,         // 延迟解析，等 categories 加载后填充
                categoryHint = result.categoryHint,
                quantity = result.quantity,
                expiryInfo = result.expiryInfo
            )
        }
    }

    // categories 加载后补全分类 ID（首次或 categories 变化时触发）
    LaunchedEffect(categories) {
        if (categories.isNotEmpty()) {
            editStates.forEach { state ->
                if (state.categoryId == null && state.categoryHint.isNotBlank()) {
                    state.categoryId = categories.firstOrNull { it.name == state.categoryHint }?.id
                }
            }
        }
    }

    val checkedCount = editStates.count { it.checked }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("确认识别结果") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Box(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Button(
                        onClick = {
                            val items = editStates
                                .filter { it.checked && it.name.isNotBlank() }
                                .map { state ->
                                    val parsedDate = parseDate(state.expiryInfo)
                                    val remarkText = if (parsedDate == null && state.expiryInfo.isNotBlank())
                                        "识别到期信息：${state.expiryInfo}" else ""
                                    FoodItem(
                                        name = state.name,
                                        quantity = state.quantity,
                                        categoryId = state.categoryId,
                                        expiryDate = parsedDate,
                                        remark = remarkText,
                                        imageUri = imageUri
                                    )
                                }
                            foodViewModel.addItems(items)
                            cameraViewModel.clearResult()
                            onDone()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = checkedCount > 0
                    ) {
                        Text("保存 $checkedCount 项食材")
                    }
                }
            }
        }
    ) { padding ->
        if (results.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("未识别到食材", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "共识别到 ${results.size} 项，勾选需要保存的食材",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            itemsIndexed(editStates) { _, state ->
                ConfirmItemCard(state = state, categories = categories)
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfirmItemCard(state: ConfirmItemState, categories: List<Category>) {
    var showCategoryMenu by remember { mutableStateOf(false) }
    val selectedCategory = categories.firstOrNull { it.id == state.categoryId }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (state.checked) CardDefaults.cardColors()
        else CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = state.checked,
                onCheckedChange = { state.checked = it },
                modifier = Modifier.padding(top = 6.dp)
            )
            Spacer(Modifier.width(4.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = { state.name = it },
                    label = { Text("食材名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = state.checked
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 分类选择
                    Box {
                        FilterChip(
                            selected = selectedCategory != null,
                            onClick = { if (state.checked) showCategoryMenu = true },
                            label = {
                                Text(
                                    if (selectedCategory != null)
                                        "${selectedCategory.emoji} ${selectedCategory.name}"
                                    else "选择分类"
                                )
                            },
                            enabled = state.checked
                        )
                        DropdownMenu(
                            expanded = showCategoryMenu,
                            onDismissRequest = { showCategoryMenu = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text("${cat.emoji} ${cat.name}") },
                                    onClick = {
                                        state.categoryId = cat.id
                                        showCategoryMenu = false
                                    }
                                )
                            }
                        }
                    }
                    // 规格/到期信息（只读展示）
                    val detail = buildString {
                        if (state.quantity.isNotBlank()) append(state.quantity)
                        if (state.expiryInfo.isNotBlank()) {
                            if (isNotEmpty()) append("  ·  ")
                            append(state.expiryInfo)
                        }
                    }
                    if (detail.isNotBlank()) {
                        Text(
                            detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
