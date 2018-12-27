package com.craiovadata.android.messenger.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.craiovadata.android.messenger.R
import com.craiovadata.android.messenger.model.SearchedUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import kotlinx.android.synthetic.main.search_item.view.*

open class SearchAdapter(query: Query?, private val listener: OnUserSelectedListener) :
        FirestoreAdapter<SearchAdapter.ViewHolder>(query) {

    interface OnUserSelectedListener {

        fun onUserSelected(documentSnapshot: DocumentSnapshot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(inflater.inflate(R.layout.search_item, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getSnapshot(position), listener)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(
                snapshot: DocumentSnapshot,
                listener: OnUserSelectedListener?
        ) {

            val user = snapshot.toObject(SearchedUser::class.java)
            if (user == null) {
                return
            }

//            val resources = itemView.resources

            // Load image
            Glide.with(itemView.imageSearch.context)
                    .load(user.photoUrl)
                    .into(itemView.imageSearch)


            itemView.textSearch.text = user.name

            // Click listener
            itemView.setOnClickListener {
                listener?.onUserSelected(snapshot)
            }
        }
    }
}
