package com.craiovadata.android.messenger.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.craiovadata.android.messenger.R
import com.craiovadata.android.messenger.model.Message
import com.google.firebase.firestore.Query
import kotlinx.android.synthetic.main.item_rating.view.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * RecyclerView adapter for a list of [Message].
 */
open class MessageAdapter(query: Query) : FirestoreAdapter<MessageAdapter.ViewHolder>(query) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_rating, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getSnapshot(position).toObject(Message::class.java))
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(rating: Message?) {
            if (rating == null) {
                return
            }

            itemView.ratingItemName.text = rating.displayName
            itemView.ratingItemText.text = rating.text

            if (rating.timestamp != null) {
                itemView.ratingItemDate.text = FORMAT.format(rating.timestamp)
            }
        }

        companion object {

            private val FORMAT = SimpleDateFormat(
                    "MM/dd/yyyy", Locale.US)
        }
    }
}
