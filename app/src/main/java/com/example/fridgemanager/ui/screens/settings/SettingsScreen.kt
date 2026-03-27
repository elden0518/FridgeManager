package com.example.fridgemanager.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.fridgemanager.data.model.Category
import com.example.fridgemanager.data.preferences.ReminderSettings
import com.example.fridgemanager.ui.viewmodel.FoodViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.BreakIterator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: FoodViewModel) {
    val state by viewModel.homeState.collectAsState()
    val scope = rememberCoroutineScope()

    // 提醒设置本地状态
    var reminderEnabled by remember { mutableStateOf(state.reminderSettings.enabled) }
    var reminderDays by remember { mutableStateOf(state.reminderSettings.daysBefore.toString()) }
    LaunchedEffect(state.reminderSettings) {
        reminderEnabled = state.reminderSettings.enabled
        reminderDays = state.reminderSettings.daysBefore.toString()
    }

    // 分类管理
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    // AI API Key
    val savedApiKey by viewModel.aiApiKey.collectAsState()
    var apiKey by remember { mutableStateOf("") }
    var showApiKey by remember { mutableStateOf(false) }
    var apiKeySaved by remember { mutableStateOf(false) }

    // 首次加载时填入已保存的 Key
    LaunchedEffect(savedApiKey) {
        if (apiKey.isBlank() && savedApiKey.isNotBlank()) {
            apiKey = savedApiKey
        }
    }
    // 编辑时重置"已保存"状态
    LaunchedEffect(apiKey) { apiKeySaved = false }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── 临期提醒 ────────────────────────────────────────────────
        item {
            Text("临期提醒", style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary)
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("开启临期提醒")
                            Text(
                                "在首页显示即将过期食材",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = reminderEnabled,
                            onCheckedChange = {
                                reminderEnabled = it
                                scope.launch {
                                    viewModel.updateReminderSettings(
                                        ReminderSettings(it, reminderDays.toIntOrNull() ?: 7)
                                    )
                                }
                            }
                        )
                    }

                    AnimatedVisibilityWrapper(reminderEnabled) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("提前提醒天数", style = MaterialTheme.typography.bodyMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(3, 7, 14, 30).forEach { days ->
                                    FilterChip(
                                        selected = reminderDays == days.toString(),
                                        onClick = {
                                            reminderDays = days.toString()
                                            scope.launch {
                                                viewModel.updateReminderSettings(
                                                    ReminderSettings(reminderEnabled, days)
                                                )
                                            }
                                        },
                                        label = { Text("${days}天") }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── AI 识别设置 ─────────────────────────────────────────────
        item {
            Text("AI 识别设置", style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary)
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "输入阿里云百炼 API Key 以启用拍照识别（模型：qwen-vl-plus）\n在 dashscope.console.aliyun.com 获取",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (showApiKey) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showApiKey = !showApiKey }) {
                                Icon(
                                    if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            viewModel.saveApiKey(apiKey)
                            apiKeySaved = true
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        if (apiKeySaved) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("已保存")
                        } else {
                            Text("保存")
                        }
                    }
                }
            }
        }

        // ── 分类管理 ────────────────────────────────────────────────
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("分类管理", style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                TextButton(onClick = { showAddCategoryDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("新增")
                }
            }
        }
        items(state.categories) { cat ->
            CategoryItem(
                category = cat,
                onDelete = { viewModel.deleteCategory(cat) }
            )
        }

        item { Spacer(Modifier.height(32.dp)) }
    }

    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onConfirm = { name, emoji ->
                viewModel.addCategory(name, emoji)
                showAddCategoryDialog = false
            },
            onDismiss = { showAddCategoryDialog = false }
        )
    }
}

@Composable
private fun CategoryItem(category: Category, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(category.emoji, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(12.dp))
            Text(category.name, Modifier.weight(1f))
            if (!category.isDefault) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "默认",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun AddCategoryDialog(
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("🍽️") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增分类") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = emoji,
                    onValueChange = { input ->
                        if (input.isBlank()) {
                            emoji = input
                        } else {
                            // 取第一个字形簇（grapheme cluster），正确处理带变体选择符或 ZWJ 的 emoji
                            val bi = BreakIterator.getCharacterInstance()
                            bi.setText(input)
                            val end = bi.next()
                            if (end != BreakIterator.DONE) emoji = input.substring(0, end)
                        }
                    },
                    label = { Text("Emoji") },
                    modifier = Modifier.width(80.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("分类名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name, emoji) },
                enabled = name.isNotBlank()
            ) { Text("添加") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun AnimatedVisibilityWrapper(visible: Boolean, content: @Composable () -> Unit) {
    androidx.compose.animation.AnimatedVisibility(visible = visible) {
        content()
    }
}
