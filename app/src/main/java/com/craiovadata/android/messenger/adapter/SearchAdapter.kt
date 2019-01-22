package com.craiovadata.android.messenger.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.craiovadata.android.messenger.R
import com.craiovadata.android.messenger.model.UserToSearch
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import kotlinx.android.synthetic.main.search_item.view.*

open class SearchAdapter(query: Query?, private val listener: OnUserSelectedListener) :
        FirestoreAdapter<SearchAdapter.ViewHolder>(query) {

    interface OnUserSelectedListener {

        fun onUserSelected(userToSearch: UserToSearch)
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

            val searchedUser = snapshot.toObject(UserToSearch::class.java) ?: return

//            val resources = itemView.resources

            // Load image
            Glide.with(itemView.imageSearch.context)
                    .load(searchedUser.photoUrl)
                    .into(itemView.imageSearch)


            itemView.textSearch.text = searchedUser.name

            // Click listener
            itemView.setOnClickListener {
                listener?.onUserSelected(searchedUser)
            }
        }
    }
}
