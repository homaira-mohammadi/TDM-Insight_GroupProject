package com.tdminsight.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tdminsight.app.data.HistoryEntry
import com.tdminsight.app.data.HistoryStore
import com.tdminsight.app.engine.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val _history = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val history: StateFlow<List<HistoryEntry>> = _history

    private val _lastResult = MutableStateFlow<HistoryEntry?>(null)
    val lastResult: StateFlow<HistoryEntry?> = _lastResult

    init {
        refreshHistory()
    }

    private fun refreshHistory() {
        _history.value = HistoryStore.load(getApplication())
    }

    /** Runs validation + calculation. Returns errors, or null on success (result stored in lastResult). */
    fun calculate(workflow: Workflow, form: TdmFormInputs): List<String> {
        val (parsed, errors) = TdmValidator.parse(workflow, form)
        if (parsed == null || errors.isNotEmpty()) return errors
        val result = TdmEngine.run(workflow, parsed)
        val entry = HistoryEntry(
            id = HistoryStore.newId(),
            timestamp = System.currentTimeMillis(),
            workflow = workflow,
            result = result,
        )
        HistoryStore.save(getApplication(), entry)
        _lastResult.value = entry
        refreshHistory()
        return emptyList()
    }

    fun selectHistoryEntry(id: String) {
        _lastResult.value = HistoryStore.findById(getApplication(), id)
    }

    fun clearHistory() {
        viewModelScope.launch {
            HistoryStore.clear(getApplication())
            refreshHistory()
        }
    }
}
