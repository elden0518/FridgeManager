package com.example.fridgemanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fridgemanager.data.model.Category
import com.example.fridgemanager.data.model.FoodItem
import com.example.fridgemanager.data.preferences.ReminderSettings
import com.example.fridgemanager.data.preferences.UserPreferences
import com.example.fridgemanager.data.repository.FoodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val items: List<FoodItem> = emptyList(),
    val expiringItems: List<FoodItem> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Long? = null,
    val searchQuery: String = "",
    val reminderSettings: ReminderSettings = ReminderSettings()
)

data class StatsUiState(
    val monthlyData: List<Pair<String, Int>> = emptyList(),  // month → count
    val categoryData: List<Pair<String, Int>> = emptyList()  // categoryName → count
)

sealed class UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent()
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class FoodViewModel @Inject constructor(
    private val repository: FoodRepository,
    private val prefs: UserPreferences
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    // 搜索防抖：300ms 内连续输入只触发一次数据库查询
    private val debouncedQuery = _searchQuery
        .debounce(300)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    private val reminderSettings = prefs.reminderSettings.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), ReminderSettings()
    )

    val homeState: StateFlow<HomeUiState> = combine(
        debouncedQuery,         // 防抖后的查询（驱动 DB）
        _searchQuery,           // 原始输入（驱动 UI 文本框即时显示）
        _selectedCategoryId,
        reminderSettings,
        repository.getAllCategories()
    ) { args ->
        // combine 5个flow时必须用数组形式
        @Suppress("UNCHECKED_CAST")
        val dbQuery    = args[0] as String
        val uiQuery    = args[1] as String
        val catId      = args[2] as Long?
        val reminder   = args[3] as ReminderSettings
        val categories = args[4] as List<Category>
        Triple(dbQuery to (uiQuery to catId), reminder, categories)
    }.flatMapLatest { (queryBundle, reminder, categories) ->
        val (dbQuery, uiAndCat) = queryBundle
        val (uiQuery, catId) = uiAndCat
        combine(
            repository.getActiveItems(dbQuery, catId),
            repository.getExpiringItems(reminder.daysBefore)
        ) { items, expiring ->
            HomeUiState(
                items = items,
                expiringItems = if (reminder.enabled) expiring else emptyList(),
                categories = categories,
                selectedCategoryId = catId,
                searchQuery = uiQuery,   // 即时显示用户输入
                reminderSettings = reminder
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    val statsState: StateFlow<StatsUiState> = combine(
        repository.getMonthlyStats(),
        repository.getCategoryDistribution(),
        repository.getAllCategories()
    ) { monthly, distribution, categories ->
        val categoryMap = categories.associateBy { it.id }
        StatsUiState(
            monthlyData = monthly.map { it.month to it.count },
            categoryData = distribution.map { dist ->
                val catName = dist.categoryId?.let { categoryMap[it]?.name } ?: "未分类"
                catName to dist.count
            }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsUiState())

    // ── 搜索 & 筛选 ──────────────────────────────────────────────────

    fun onSearchQueryChange(query: String) { _searchQuery.value = query }
    fun onCategorySelect(categoryId: Long?) { _selectedCategoryId.value = categoryId }

    // ── 食材 CRUD ────────────────────────────────────────────────────

    fun addItem(item: FoodItem) = viewModelScope.launch {
        repository.addItem(item)
        _uiEvent.emit(UiEvent.ShowSnackbar("「${item.name}」已添加"))
    }

    fun updateItem(item: FoodItem) = viewModelScope.launch {
        repository.updateItem(item)
    }

    suspend fun getItemById(id: Long): FoodItem? = repository.getItemById(id)

    fun markConsumed(item: FoodItem) = viewModelScope.launch {
        repository.markConsumed(item.id)
        _uiEvent.emit(UiEvent.ShowSnackbar("「${item.name}」已标记为吃完"))
    }

    fun markDiscarded(item: FoodItem) = viewModelScope.launch {
        repository.markDiscarded(item.id)
        _uiEvent.emit(UiEvent.ShowSnackbar("「${item.name}」已标记为丢弃"))
    }

    fun deleteItem(item: FoodItem) = viewModelScope.launch {
        repository.deleteItem(item)
        _uiEvent.emit(UiEvent.ShowSnackbar("「${item.name}」已删除"))
    }

    // ── 分类管理 ────────────────────────────────────────────────────

    fun addCategory(name: String, emoji: String) = viewModelScope.launch {
        repository.addCategory(Category(name = name, emoji = emoji))
    }

    fun deleteCategory(category: Category) = viewModelScope.launch {
        if (category.isDefault) {
            _uiEvent.emit(UiEvent.ShowSnackbar("默认分类不可删除"))
            return@launch
        }
        repository.deleteCategory(category)
    }

    // ── 提醒设置 ────────────────────────────────────────────────────

    fun updateReminderSettings(settings: ReminderSettings) = viewModelScope.launch {
        prefs.updateReminderSettings(settings)
    }

    fun saveApiKey(key: String) = viewModelScope.launch {
        prefs.updateAiApiKey(key)
    }
}
