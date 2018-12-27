package com.craiovadata.android.messenger.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.craiovadata.android.messenger.R
import com.craiovadata.android.messenger.model.Room
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import kotlinx.android.synthetic.main.item_room.view.*

/**
 * RecyclerView roomAdapter for a list of rooms.
 */
open class RoomAdapter(query: Query, private val listener: OnRoomSelectedListener, private val userName: String?) :
        FirestoreAdapter<RoomAdapter.ViewHolder>(query) {

    interface OnRoomSelectedListener {

        fun onRoomSelected(room: DocumentSnapshot)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return ViewHolder(inflater.inflate(R.layout.item_room, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getSnapshot(position), listener, userName)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(
                snapshot: DocumentSnapshot,
                listener: OnRoomSelectedListener?,
                userName: String?
        ) {

            val room = snapshot.toObject(Room::class.java) ?: return


//            val resources = itemView.resources

            Glide.with(itemView.roomItemImage.context)
                    .load(room.palPhotoUrl)
                    .into(itemView.roomItemImage)


            val roomName = room.palName
            itemView.roomName.text = roomName
            itemView.lastMsg.text = room.lastMsg

            var text = ""
            if (room.lastMsgAuthor != roomName)
                text = room.lastMsgAuthor + ":"
            if (room.lastMsgAuthor == userName)
                text = "You:"
            itemView.author.text = text

            // Click listener
            itemView.setOnClickListener {
                listener?.onRoomSelected(snapshot)
            }
        }
    }
}
