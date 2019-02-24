package com.craiovadata.android.messenger.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.craiovadata.android.messenger.R
import com.craiovadata.android.messenger.model.GlideApp
import com.craiovadata.android.messenger.model.Person
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import kotlinx.android.synthetic.main.search_item.view.*

open class SearchAdapter(options: FirestoreRecyclerOptions<Person>, private val listener: OnUserSelectedListener) :
        FirestoreRecyclerAdapter<Person, SearchAdapter.ViewHolder>(options) {

    interface OnUserSelectedListener {
        fun onUserSelected(person: Person)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(inflater.inflate(R.layout.search_item, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, person: Person) {
        holder.bind(person, listener)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(person: Person, listener: OnUserSelectedListener?) {
            GlideApp.with(itemView.imageSearch.context)
                    .load(person.photoUrl)
                    .placeholder(R.drawable.ic_person_white_24dp)
                    .into(itemView.imageSearch)

            itemView.textSearch.text = person.name

            itemView.setOnClickListener {
                listener?.onUserSelected(person)
            }
        }
    }
}
