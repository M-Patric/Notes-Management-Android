package com.example.notesmanagement.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notesmanagement.data.Note
import com.example.notesmanagement.data.NoteRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotesUiState(
    val notes: List<Note> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val errorMessage: String? = null
)

class NotesViewModel(
    private val repository: NoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotesUiState())

    val uiState: StateFlow<NotesUiState> =
        _uiState.asStateFlow()

    private var notesCollectionJob: Job? = null

    init {
        observeNotes()
    }

    fun setSearchQuery(query: String) {

        _uiState.update {
            it.copy(
                searchQuery = query,
                isLoading = true,
                errorMessage = null
            )
        }

        observeNotes()
    }

    private fun observeNotes() {

        notesCollectionJob?.cancel()

        notesCollectionJob = viewModelScope.launch {

            val query = _uiState.value.searchQuery.trim()

            val notesFlow = if (query.isBlank()) {
                repository.allNotes
            } else {
                repository.searchNotes(query)
            }

            notesFlow
                .catch { exception ->

                    _uiState.update {
                        it.copy(
                            notes = emptyList(),
                            isLoading = false,
                            errorMessage =
                                exception.message
                                    ?: "Unable to load notes."
                        )
                    }
                }
                .collect { notes ->

                    _uiState.update {
                        it.copy(
                            notes = notes,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
        }
    }

    fun saveNote(
        title: String,
        content: String,
        existingNote: Note? = null
    ) {

        val cleanedTitle = title.trim()
        val cleanedContent = content.trim()

        if (cleanedTitle.isBlank() && cleanedContent.isBlank()) {
            _uiState.update {
                it.copy(
                    errorMessage = "Please enter a title or note content."
                )
            }
            return
        }

        if (_uiState.value.isSaving) {
            return
        }

        viewModelScope.launch {

            _uiState.update {
                it.copy(
                    isSaving = true,
                    errorMessage = null
                )
            }

            try {

                val currentTime = System.currentTimeMillis()

                val note = if (existingNote == null) {

                    Note(
                        title = cleanedTitle,
                        content = cleanedContent,
                        createdAt = currentTime,
                        updatedAt = currentTime
                    )

                } else {

                    existingNote.copy(
                        title = cleanedTitle,
                        content = cleanedContent,
                        updatedAt = currentTime
                    )
                }

                if (existingNote == null) {
                    repository.insert(note)
                } else {
                    repository.update(note)
                }

                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = null
                    )
                }

            } catch (exception: Exception) {

                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage =
                            exception.message
                                ?: "Unable to save the note."
                    )
                }
            }
        }
    }

    fun deleteNote(note: Note) {

        if (_uiState.value.isDeleting) {
            return
        }

        viewModelScope.launch {

            _uiState.update {
                it.copy(
                    isDeleting = true,
                    errorMessage = null
                )
            }

            try {

                repository.delete(note)

                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        errorMessage = null
                    )
                }

            } catch (exception: Exception) {

                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        errorMessage =
                            exception.message
                                ?: "Unable to delete the note."
                    )
                }
            }
        }
    }

    fun clearError() {

        _uiState.update {
            it.copy(errorMessage = null)
        }
    }

    override fun onCleared() {

        notesCollectionJob?.cancel()

        super.onCleared()
    }
}