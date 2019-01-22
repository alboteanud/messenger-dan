package com.craiovadata.android.messenger.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.craiovadata.android.messenger.R
import com.craiovadata.android.messenger.model.Conversation
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import kotlinx.android.synthetic.main.item_conversation.view.*

open class ConversationAdapter(query: Query, private val listener: OnConversationSelectedListener) :
        FirestoreAdapter<ConversationAdapter.ViewHolder>(query) {

    interface OnConversationSelectedListener {
        fun onConversationSelected(conversation: Conversation)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return ViewHolder(inflater.inflate(R.layout.item_conversation, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getSnapshot(position), listener)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(documentSnapshot: DocumentSnapshot, listener: OnConversationSelectedListener?) {

            val conversation = documentSnapshot.toObject(Conversation::class.java) ?: return
            Log.d("TAG", "DocumentSnapshot data: " + conversation)
//            val resources = itemView.resources

            Glide.with(itemView.roomItemImage.context)
                    .load(conversation.palPhotoUrl)
                    .into(itemView.roomItemImage)

            itemView.roomName.text = conversation.palName
            itemView.lastMsg.text = conversation.lastMessage
            itemView.author.text = conversation.msgAuthor

            // Click listener
            itemView.setOnClickListener {
                listener?.onConversationSelected(conversation)
            }
        }
    }
}
