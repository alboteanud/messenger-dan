package com.craiovadata.android.messenger.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.craiovadata.android.messenger.R
import com.craiovadata.android.messenger.model.Room
import com.google.firebase.auth.FirebaseAuth
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

            val user = FirebaseAuth.getInstance().currentUser
            var participants = ""
            val photoUrls: MutableList<String> = mutableListOf()

            for (one in room.participants) {
                if (one["uid"] != user!!.uid) {
                    participants += (one["name"].toString() + " ")
                    val url = one["photoUrl"].toString()
                    photoUrls.add(url)
                }

            }

            val resources = itemView.resources

            if (!photoUrls.isEmpty())
                Glide.with(itemView.restaurantItemImage.context)
                        .load(photoUrls[0])
                        .into(itemView.restaurantItemImage)


            itemView.restaurantItemName.text = participants
            itemView.restaurantItemCategory.text = room.lastMsg
            itemView.restaurantItemCity.text = room.lastMsgAuthor

            // Click listener
            itemView.setOnClickListener {
                listener?.onRoomSelected(snapshot)
            }
        }
    }
}
