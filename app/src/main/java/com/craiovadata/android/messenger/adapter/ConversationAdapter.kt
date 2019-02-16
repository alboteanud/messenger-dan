package com.craiovadata.android.messenger.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.craiovadata.android.messenger.R
import com.craiovadata.android.messenger.model.Conversation
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import kotlinx.android.synthetic.main.item_conversation.view.*


open class ConversationAdapter(options: FirestoreRecyclerOptions<Conversation>, private val listener: OnConversationSelectedListener) :
        FirestoreRecyclerAdapter<Conversation, ConversationAdapter.ViewHolder>(options) {

    interface OnConversationSelectedListener {
        fun onConversationSelected(conversation: Conversation, adapterPosition: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return ViewHolder(inflater.inflate(R.layout.item_conversation, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, conversation: Conversation) {
        val conversation = snapshots[position]
        holder.bind(conversation, listener)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(conversation: Conversation, listener: OnConversationSelectedListener?) {

//            val conversation = documentSnapshot.toObject(Conversation::class.java) ?: return

            Glide.with(itemView.personPhoto.context)
                    .load(conversation.photoUrlP)
                    .into(itemView.personPhoto)

            itemView.personName.text = conversation.nameP
            itemView.personText1.text = conversation.author
            itemView.personText2.text = conversation.text

            // Click listener
            itemView.setOnClickListener {

                if (adapterPosition != RecyclerView.NO_POSITION) {
                    listener?.onConversationSelected(conversation, adapterPosition)
                }
            }
        }
    }


}
