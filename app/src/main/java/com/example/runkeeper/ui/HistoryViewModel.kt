package com.example.runkeeper.ui

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runkeeper.database.Run
import com.example.runkeeper.repositories.HistoryRepository
import kotlinx.coroutines.launch

class HistoryViewModel @ViewModelInject constructor(
    val historyRepository: HistoryRepository
) : ViewModel() {

    val getAllRuns = historyRepository.getAllRuns()

    fun insertRun(run: Run) = viewModelScope.launch {
        historyRepository.insertRun(run)
    }
}