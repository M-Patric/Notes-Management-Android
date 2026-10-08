package com.example.notesmanagement.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.content.Context

import androidx.appcompat.app.AppCompatActivity

import com.example.notesmanagement.R
import com.example.notesmanagement.databinding.ActivityNoteEditorBinding

class NoteEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNoteEditorBinding

    private var isEditMode = false
    private var noteId = 0L
    private var createdAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding =
            ActivityNoteEditorBinding.inflate(layoutInflater)

        setContentView(binding.root)

        readIntentData()
        setupScreen()
        setupButtons()
    }

    private fun readIntentData() {

        noteId =
            intent.getLongExtra(
                EXTRA_NOTE_ID,
                0L
            )

        createdAt =
            intent.getLongExtra(
                EXTRA_CREATED_AT,
                0L
            )

        val title =
            intent.getStringExtra(
                EXTRA_TITLE
            ).orEmpty()

        val content =
            intent.getStringExtra(
                EXTRA_CONTENT
            ).orEmpty()

        isEditMode = noteId != 0L

        binding.titleEditText.setText(title)
        binding.contentEditText.setText(content)
    }

    private fun setupScreen() {

        if (isEditMode) {

            binding.editorTitleText.text =
                getString(R.string.edit_note_screen_title)

            binding.saveButton.text =
                getString(R.string.save_changes)

        } else {

            binding.editorTitleText.text =
                getString(R.string.new_note)

            binding.saveButton.text =
                getString(R.string.save_note)
        }
    }

    private fun setupButtons() {

        binding.cancelButton.setOnClickListener {
            finish()
        }

        binding.saveButton.setOnClickListener {
            saveNote()
        }
    }

    private fun saveNote() {

        val title =
            binding.titleEditText.text
                ?.toString()
                .orEmpty()
                .trim()

        val content =
            binding.contentEditText.text
                ?.toString()
                .orEmpty()
                .trim()

        if (title.isBlank() && content.isBlank()) {

            binding.titleInputLayout.error =
                getString(
                    R.string.note_validation_error
                )

            return
        }

        binding.titleInputLayout.error = null

        hideKeyboard()

        val resultIntent =
            Intent().apply {

                putExtra(
                    EXTRA_NOTE_ID,
                    noteId
                )

                putExtra(
                    EXTRA_CREATED_AT,
                    createdAt
                )

                putExtra(
                    EXTRA_TITLE,
                    title
                )

                putExtra(
                    EXTRA_CONTENT,
                    content
                )
            }

        setResult(
            Activity.RESULT_OK,
            resultIntent
        )

        finish()
    }

    private fun hideKeyboard() {

        val inputMethodManager =
            getSystemService(
                Context.INPUT_METHOD_SERVICE
            ) as InputMethodManager

        inputMethodManager.hideSoftInputFromWindow(
            currentFocus?.windowToken,
            0
        )
    }

    companion object {

        const val EXTRA_NOTE_ID =
            "com.example.notesmanagement.EXTRA_NOTE_ID"

        const val EXTRA_CREATED_AT =
            "com.example.notesmanagement.EXTRA_CREATED_AT"

        const val EXTRA_TITLE =
            "com.example.notesmanagement.EXTRA_TITLE"

        const val EXTRA_CONTENT =
            "com.example.notesmanagement.EXTRA_CONTENT"
    }
}