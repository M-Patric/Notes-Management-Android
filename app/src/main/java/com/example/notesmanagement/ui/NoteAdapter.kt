package com.example.notesmanagement.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.notesmanagement.data.Note
import com.example.notesmanagement.databinding.ItemNoteBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NoteAdapter(
    private val onEditClicked: (Note) -> Unit,
    private val onDeleteClicked: (Note) -> Unit
) : ListAdapter<Note, NoteAdapter.NoteViewHolder>(NoteDiffCallback()) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): NoteViewHolder {

        val binding = ItemNoteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: NoteViewHolder,
        position: Int
    ) {

        holder.bind(getItem(position))
    }

    inner class NoteViewHolder(
        private val binding: ItemNoteBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(note: Note) {

            binding.noteTitleText.text =
                if (note.title.isBlank()) {
                    "Untitled Note"
                } else {
                    note.title
                }

            binding.noteContentText.text =
                if (note.content.isBlank()) {
                    "No content"
                } else {
                    note.content
                }

            binding.noteDateText.text =
                formatDate(note.updatedAt)

            binding.editNoteButton.setOnClickListener {
                onEditClicked(note)
            }

            binding.deleteNoteButton.setOnClickListener {
                onDeleteClicked(note)
            }
        }

        private fun formatDate(timestamp: Long): String {

            val formatter = SimpleDateFormat(
                "dd MMM yyyy",
                Locale.getDefault()
            )

            return formatter.format(Date(timestamp))
        }
    }

    private class NoteDiffCallback : DiffUtil.ItemCallback<Note>() {

        override fun areItemsTheSame(
            oldItem: Note,
            newItem: Note
        ): Boolean {

            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: Note,
            newItem: Note
        ): Boolean {

            return oldItem == newItem
        }
    }
}