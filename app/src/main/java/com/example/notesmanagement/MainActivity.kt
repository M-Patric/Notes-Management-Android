package com.example.notesmanagement

import android.content.Intent
import android.os.Bundle
import android.view.View

import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager

import com.example.notesmanagement.data.Note
import com.example.notesmanagement.data.NoteDatabase
import com.example.notesmanagement.data.NoteRepository
import com.example.notesmanagement.databinding.ActivityMainBinding
import com.example.notesmanagement.ui.NoteAdapter
import com.example.notesmanagement.ui.NoteEditorActivity
import com.example.notesmanagement.ui.NotesViewModel
import com.example.notesmanagement.ui.NotesViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder

import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var notesViewModel: NotesViewModel
    private lateinit var noteAdapter: NoteAdapter

    private val noteEditorLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            if (result.resultCode != RESULT_OK) {
                return@registerForActivityResult
            }

            val data = result.data
                ?: return@registerForActivityResult

            val noteId =
                data.getLongExtra(
                    NoteEditorActivity.EXTRA_NOTE_ID,
                    0L
                )

            val createdAt =
                data.getLongExtra(
                    NoteEditorActivity.EXTRA_CREATED_AT,
                    0L
                )

            val title =
                data.getStringExtra(
                    NoteEditorActivity.EXTRA_TITLE
                ).orEmpty()

            val content =
                data.getStringExtra(
                    NoteEditorActivity.EXTRA_CONTENT
                ).orEmpty()

            val existingNote =
                if (noteId != 0L) {

                    Note(
                        id = noteId,
                        title = title,
                        content = content,
                        createdAt = createdAt,
                        updatedAt = createdAt
                    )

                } else {
                    null
                }

            notesViewModel.saveNote(
                title = title,
                content = content,
                existingNote = existingNote
            )
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        binding =
            ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->

            val systemBars =
                insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                )

            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }

        initializeViewModel()
        initializeRecyclerView()
        setupSearch()
        setupAddNoteButton()
        observeNotes()
    }

    private fun initializeViewModel() {

        val database =
            NoteDatabase.getDatabase(
                applicationContext
            )

        val repository =
            NoteRepository(
                database.noteDao()
            )

        val factory =
            NotesViewModelFactory(
                repository
            )

        notesViewModel =
            ViewModelProvider(
                this,
                factory
            )[NotesViewModel::class.java]
    }

    private fun initializeRecyclerView() {

        noteAdapter = NoteAdapter(

            onEditClicked = { note ->
                openNoteEditor(note)
            },

            onDeleteClicked = { note ->
                showDeleteConfirmation(note)
            }
        )

        binding.notesRecyclerView.apply {

            layoutManager =
                LinearLayoutManager(
                    this@MainActivity
                )

            adapter = noteAdapter

            setHasFixedSize(true)
        }
    }

    private fun setupSearch() {

        binding.searchEditText.addTextChangedListener(
            object : android.text.TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    // No action needed.
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {

                    notesViewModel.setSearchQuery(
                        s?.toString().orEmpty()
                    )
                }

                override fun afterTextChanged(
                    s: android.text.Editable?
                ) {
                    // No action needed.
                }
            }
        )
    }

    private fun setupAddNoteButton() {

        binding.addNoteButton.setOnClickListener {
            openNoteEditor()
        }
    }

    private fun openNoteEditor(
        note: Note? = null
    ) {

        val intent =
            Intent(
                this,
                NoteEditorActivity::class.java
            ).apply {

                if (note != null) {

                    putExtra(
                        NoteEditorActivity.EXTRA_NOTE_ID,
                        note.id
                    )

                    putExtra(
                        NoteEditorActivity.EXTRA_CREATED_AT,
                        note.createdAt
                    )

                    putExtra(
                        NoteEditorActivity.EXTRA_TITLE,
                        note.title
                    )

                    putExtra(
                        NoteEditorActivity.EXTRA_CONTENT,
                        note.content
                    )
                }
            }

        noteEditorLauncher.launch(intent)
    }

    private fun showDeleteConfirmation(
        note: Note
    ) {

        val displayTitle =
            note.title.ifBlank {
                "Untitled Note"
            }

        val message =
            "${getString(R.string.delete_note_message)}\n\n\"$displayTitle\""

        MaterialAlertDialogBuilder(this)
            .setTitle(
                R.string.delete_note_title
            )
            .setMessage(message)
            .setNegativeButton(
                R.string.cancel,
                null
            )
            .setPositiveButton(
                R.string.delete_confirmation
            ) { _, _ ->

                notesViewModel.deleteNote(note)
            }
            .show()
    }

    private fun observeNotes() {

        lifecycleScope.launch {

            repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                notesViewModel.uiState.collect { state ->

                    noteAdapter.submitList(
                        state.notes
                    )

                    updateNotesVisibility(
                        notes = state.notes,
                        isLoading = state.isLoading,
                        searchQuery = state.searchQuery
                    )

                    updateLoadingState(
                        state.isLoading
                    )

                    updateErrorState(
                        state.errorMessage
                    )
                }
            }
        }
    }

    private fun updateNotesVisibility(
        notes: List<Note>,
        isLoading: Boolean,
        searchQuery: String
    ) {

        if (isLoading) {
            return
        }

        val hasNotes =
            notes.isNotEmpty()

        binding.notesRecyclerView.visibility =
            if (hasNotes) {
                View.VISIBLE
            } else {
                View.GONE
            }

        binding.emptyStateLayout.visibility =
            if (hasNotes) {
                View.GONE
            } else {
                View.VISIBLE
            }

        if (searchQuery.isBlank()) {

            binding.emptyStateTitle.text =
                getString(
                    R.string.empty_notes_title
                )

            binding.emptyStateMessage.text =
                getString(
                    R.string.empty_notes_message
                )

        } else {

            binding.emptyStateTitle.text =
                getString(
                    R.string.no_matching_notes_title
                )

            binding.emptyStateMessage.text =
                getString(
                    R.string.no_matching_notes_message
                )
        }
    }

    private fun updateLoadingState(
        isLoading: Boolean
    ) {

        binding.loadingProgressBar.visibility =
            if (isLoading) {
                View.VISIBLE
            } else {
                View.GONE
            }
    }

    private fun updateErrorState(
        errorMessage: String?
    ) {

        if (errorMessage.isNullOrBlank()) {

            binding.errorText.visibility =
                View.GONE

            return
        }

        binding.errorText.visibility =
            View.VISIBLE

        binding.errorText.text =
            errorMessage
    }
}