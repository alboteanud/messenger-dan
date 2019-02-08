package com.craiovadata.android.messenger.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.craiovadata.android.messenger.R
import com.craiovadata.android.messenger.util.MSG_AUTHOR
import com.craiovadata.android.messenger.util.NAME
import com.craiovadata.android.messenger.util.PHOTO_URL
import com.craiovadata.android.messenger.util.MSG_TEXT
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import kotlinx.android.synthetic.main.item_conversation.view.*

open class ConversationAdapter(query: Query, private val listener: OnConversationSelectedListener) :
        FirestoreAdapter<ConversationAdapter.ViewHolder>(query) {

    interface OnConversationSelectedListener {
        fun onConversationSelected(conversation: HashMap<String, String?>)
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

            val conversation: HashMap<String, String?> = documentSnapshot.data as HashMap<String, String?>?
                    ?: return
//            val resources = itemView.resources

            Glide.with(itemView.roomItemImage.context)
                    .load(conversation[PHOTO_URL])
                    .into(itemView.roomItemImage)

            itemView.roomName.text = conversation[NAME]
            itemView.author.text = conversation[MSG_AUTHOR]
            itemView.lastMsg.text = conversation[MSG_TEXT]

            // Click listener
            itemView.setOnClickListener {
                listener?.onConversationSelected(conversation)
            }
        }
    }
}
