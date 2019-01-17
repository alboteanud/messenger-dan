package com.craiovadata.android.messenger.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.craiovadata.android.messenger.R
import com.craiovadata.android.messenger.model.GlideApp
import com.craiovadata.android.messenger.model.Message
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.Query
import kotlinx.android.synthetic.main.item_message.view.*
import java.text.SimpleDateFormat
import java.util.*

open class MessageAdapter(query: Query, private val user: FirebaseUser) : FirestoreAdapter<MessageAdapter.ViewHolder>(query) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getSnapshot(position).toObject(Message::class.java), user)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(message: Message?, user: FirebaseUser) {
            if (message == null) return

            var txtToShow = message.text
            if (txtToShow != null) {
                if (txtToShow.startsWith("https://firebasestorage.googleapis.")) {
//                    val dateTxt = FORMAT.format(message.timestamp)
                    txtToShow = "audio message"
                }

                itemView.messageItemText.text = txtToShow
            }

//            if (message.timestamp != null) itemView.ratingItemDate.text = FORMAT.format(message.timestamp)

            if (user.uid == message.userId) {
//                itemView.messageItemText.setBackgroundColor(itemView.context.getColor(R.color.gray2))
            }
            GlideApp.with(itemView.context)
                    .load(message.photoUrl)
                    .placeholder(R.drawable.ic_person_black_24dp)
                    .into(itemView.messageItemImageView)
        }

        companion object {

            private val FORMAT = SimpleDateFormat(
                    "HH:mm", Locale.US)
        }
    }
}
