package com.craiovadata.android.messenger.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.craiovadata.android.messenger.R
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import kotlinx.android.synthetic.main.item_conversation.view.*

open class PeopleAdapter(query: Query, private val listener: OnItemSelectedListener) :
        FirestoreAdapter<PeopleAdapter.ViewHolder>(query) {

    interface OnItemSelectedListener {
        fun onItemSelected(personData: Map<String, Any>)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return ViewHolder(inflater.inflate(R.layout.item_conversation, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getSnapshot(position), listener)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(documentSnapshot: DocumentSnapshot, listener: OnItemSelectedListener?) {

            val personData: Map<String, Any> = documentSnapshot.data as Map<String, Any>

            Glide.with(itemView.personPhoto.context)
                    .load(personData["photoUrl"])
                    .into(itemView.personPhoto)

            itemView.personName.text = personData["name"] as? String
            itemView.personText1.text = personData["email"] as? String
            itemView.personText2.text = "..."

            // Click listener
            itemView.setOnClickListener {
                listener?.onItemSelected(personData)
            }
        }
    }
}
