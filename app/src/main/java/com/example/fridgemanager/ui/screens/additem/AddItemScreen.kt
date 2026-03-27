package com.example.fridgemanager.ui.screens.additem

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.fridgemanager.data.model.Category
import com.example.fridgemanager.data.model.FoodItem
import com.example.fridgemanager.ui.viewmodel.FoodViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemScreen(
    viewModel: FoodViewModel,
    itemId: Long = -1,
    onNavigateBack: () -> Unit,
    // 预填数据（来自AI识别）
    prefillItem: FoodItem? = null
) {
    val categories by viewModel.homeState.collectAsState()
    val catList = categories.categories

    var name     by remember { mutableStateOf(prefillItem?.name ?: "") }
    var quantity by remember { mutableStateOf(prefillItem?.quantity ?: "") }
    var remark   by remember { mutableStateOf(prefillItem?.remark ?: "") }
    var selectedCategoryId by remember { mutableStateOf(prefillItem?.categoryId) }
    var expiryDate by remember { mutableStateOf(prefillItem?.expiryDate) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showCategoryDropdown by remember { mutableStateOf(false) }

    val isEditing = itemId > 0
    val title = if (isEditing) "编辑食材" else "添加食材"
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd", Locale.CHINA) }

    // 如果是编辑，直接通过 ID 从 DB 加载，避免依赖内存中可能已过滤的列表
    LaunchedEffect(itemId) {
        if (isEditing) {
            viewModel.getItemById(itemId)?.let { item ->
                name = item.name
                quantity = item.quantity
                remark = item.remark
                selectedCategoryId = item.categoryId
                expiryDate = item.expiryDate
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (name.isBlank()) return@TextButton
                            val item = if (isEditing) {
                                FoodItem(
                                    id = itemId,
                                    name = name.trim(),
                                    quantity = quantity.trim(),
                                    categoryId = selectedCategoryId,
                                    expiryDate = expiryDate,
                                    remark = remark.trim(),
                                    imageUri = prefillItem?.imageUri
                                )
                            } else {
                                FoodItem(
                                    name = name.trim(),
                                    quantity = quantity.trim(),
                                    categoryId = selectedCategoryId,
                                    expiryDate = expiryDate,
                                    remark = remark.trim(),
                                    imageUri = prefillItem?.imageUri
                                )
                            }
                            if (isEditing) viewModel.updateItem(item) else viewModel.addItem(item)
                            onNavigateBack()
                        },
                        enabled = name.isNotBlank()
                    ) {
                        Text("保存")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // 名称
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("食材名称 *") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Label, contentDescription = null) },
                singleLine = true,
                isError = name.isBlank()
            )

            // 规格/数量
            OutlinedTextField(
                value = quantity,
                onValueChange = { quantity = it },
                label = { Text("规格/数量") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("如：250g、6个、1袋") },
                leadingIcon = { Icon(Icons.Default.Scale, contentDescription = null) },
                singleLine = true
            )

            // 分类选择
            ExposedDropdownMenuBox(
                expanded = showCategoryDropdown,
                onExpandedChange = { showCategoryDropdown = it }
            ) {
                OutlinedTextField(
                    value = catList.find { it.id == selectedCategoryId }
                        ?.let { "${it.emoji} ${it.name}" } ?: "选择分类",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("分类") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryDropdown) }
                )
                ExposedDropdownMenu(
                    expanded = showCategoryDropdown,
                    onDismissRequest = { showCategoryDropdown = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("不分类") },
                        onClick = { selectedCategoryId = null; showCategoryDropdown = false }
                    )
                    catList.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text("${cat.emoji} ${cat.name}") },
                            onClick = { selectedCategoryId = cat.id; showCategoryDropdown = false }
                        )
                    }
                }
            }

            // 到期日
            OutlinedTextField(
                value = expiryDate?.let { dateFormat.format(it) } ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("到期日") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                trailingIcon = {
                    Row {
                        if (expiryDate != null) {
                            IconButton(onClick = { expiryDate = null }) {
                                Icon(Icons.Default.Clear, contentDescription = "清除日期")
                            }
                        }
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.EditCalendar, contentDescription = "选择日期")
                        }
                    }
                },
                placeholder = { Text("点击选择") }
            )

            // 备注
            OutlinedTextField(
                value = remark,
                onValueChange = { remark = it },
                label = { Text("备注") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("如：开封后3天内食用") },
                leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) },
                maxLines = 3
            )

            Spacer(Modifier.height(16.dp))
        }
    }

    // 日期选择器 Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = expiryDate?.time ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        expiryDate = Date(millis)
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
