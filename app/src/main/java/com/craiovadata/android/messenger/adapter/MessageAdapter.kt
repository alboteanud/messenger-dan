package com.craiovadata.android.messenger.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.craiovadata.android.messenger.R
import com.craiovadata.android.messenger.model.GlideApp
import com.craiovadata.android.messenger.util.PHOTO_URL
import com.craiovadata.android.messenger.util.MSG_TEXT
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import kotlinx.android.synthetic.main.item_message.view.*
import java.text.SimpleDateFormat
import java.util.*

open class MessageAdapter(query: Query) : FirestoreAdapter<MessageAdapter.ViewHolder>(query) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getSnapshot(position))
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(snapshot: DocumentSnapshot) {
            val msg = snapshot.data  ?: return

            val txtToShow = msg[MSG_TEXT].toString()
            itemView.messageItemText.text = txtToShow

//            if (snapshot.timestamp != null) itemView.ratingItemDate.text = FORMAT.format(snapshot.timestamp)

            GlideApp.with(itemView.context)
                    .load(msg[PHOTO_URL])
                    .placeholder(R.drawable.ic_person_black_24dp)
                    .into(itemView.messageItemImageView)
        }

        companion object {

            private val FORMAT = SimpleDateFormat(
                    "HH:mm", Locale.US)
        }
    }
}




