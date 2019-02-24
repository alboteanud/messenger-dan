package com.craiovadata.android.messenger.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.craiovadata.android.messenger.R
import com.craiovadata.android.messenger.model.GlideApp
import com.craiovadata.android.messenger.model.Message
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import kotlinx.android.synthetic.main.item_message.view.*

open class MessageAdapter(options: FirestoreRecyclerOptions<Message>) :
        FirestoreRecyclerAdapter<Message, MessageAdapter.ViewHolder>(options) {

    private var listener: OnItemAddedListener? = null

    interface OnItemAddedListener {
        fun onItemAdded()
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int, message: Message) {
        val snap = snapshots[position]
        holder.bind(snap)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message, parent, false))
    }

    override fun onDataChanged() {
        super.onDataChanged()
        listener?.onItemAdded()
        Log.d("tag", "OnDataChanged")

    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(message: Message) {

            itemView.messageItemText.text = message.text

//            if (snapshot.timestamp != null) itemView.ratingItemDate.text = FORMAT.format(snapshot.timestamp)

            GlideApp.with(itemView.context)
                    .load(message.photoUrl)
                    .placeholder(R.drawable.ic_person_white_24dp)
                    .into(itemView.messageItemImageView)


        }

        companion object {
//            private val FORMAT = SimpleDateFormat("HH:mm", Locale.US)
        }
    }


    fun setOnItemAddedListener(listener: OnItemAddedListener) {
        this.listener = listener
    }



}




