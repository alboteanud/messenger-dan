package com.craiovadata.android.messenger.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.craiovadata.android.messenger.R
import com.craiovadata.android.messenger.model.Room
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import kotlinx.android.synthetic.main.item_restaurant.view.*

/**
 * RecyclerView adapter for a list of rooms.
 */
open class RoomAdapter(query: Query, private val listener: OnRoomSelectedListener) :
        FirestoreAdapter<RoomAdapter.ViewHolder>(query) {

    interface OnRoomSelectedListener {

        fun onRoomSelected(room: DocumentSnapshot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(inflater.inflate(R.layout.item_restaurant, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getSnapshot(position), listener)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(
            snapshot: DocumentSnapshot,
            listener: OnRoomSelectedListener?
        ) {

            val room = snapshot.toObject(Room::class.java)
            if (room == null) {
                return
            }

            val resources = itemView.resources

            // Load image
//            Glide.with(itemView.restaurantItemImage.context)
//                    .load(room.photoUrl)
//                    .into(itemView.restaurantItemImage)


            itemView.restaurantItemName.text = room.participants
            itemView.restaurantItemCategory.text = room.lastMsg
            itemView.restaurantItemCity.text = room.author

            // Click listener
            itemView.setOnClickListener {
                listener?.onRoomSelected(snapshot)
            }
        }
    }
}
