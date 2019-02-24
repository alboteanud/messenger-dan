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
import kotlinx.android.synthetic.main.item_conversation.view.*
import java.text.SimpleDateFormat
import java.util.*

open class PeopleAdapter(options: FirestoreRecyclerOptions<Person>, private val listener: OnItemSelectedListener) :
        FirestoreRecyclerAdapter<Person, PeopleAdapter.ViewHolder>(options) {

    interface OnItemSelectedListener {
        fun onItemSelected(person: Person)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return ViewHolder(inflater.inflate(R.layout.item_conversation, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, person: Person) {
//        val person = snapshots[position]
        holder.bind(person, listener)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(person: Person, listener: OnItemSelectedListener?) {


            GlideApp.with(itemView.personPhoto.context)
                    .load(person.photoUrl)
                    .placeholder(R.drawable.ic_person_white_24dp)
                    .into(itemView.personPhoto)

            itemView.personName.text = person.name
            person.timestamp?.let {
                        itemView.personText1.text = FORMAT.format(it)
            }
            itemView.personText2.text = person.statusTxt

            // Click listener
            itemView.setOnClickListener {
                listener?.onItemSelected(person)
            }
        }
    }


    companion object {
        private val FORMAT = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())
    }

}
