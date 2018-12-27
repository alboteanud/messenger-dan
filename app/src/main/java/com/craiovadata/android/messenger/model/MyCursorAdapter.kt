package com.craiovadata.android.messenger.model

//import android.widget.CursorAdapter
import android.content.Context
import android.database.Cursor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cursoradapter.widget.CursorAdapter
import com.craiovadata.android.messenger.R


class MyCursorAdapter(val context: Context, cursor: Cursor) : CursorAdapter(context, cursor, true) {
//class MyCursorAdapter(val context: Context, cursor: Cursor?) : SimpleCursorAdapter(context, R.layout.search_item, cursor, null, intArrayOf(R.id.textSearch), 0) {

    override fun newView(context: Context?, cursor: Cursor?, parent: ViewGroup?): View {
        return LayoutInflater.from(context).inflate(R.layout.search_item, parent, false)
    }

    override fun bindView(view: View?, context: Context?, cursor: Cursor) {
        val tvText = view?.findViewById(R.id.textSearch) as TextView
        val text = getString(cursor, "name")
        tvText.text = text

        val photoUrl = getString(cursor, "photoUrl")
        val imageView = view.findViewById(R.id.imageSearch) as ImageView

        GlideApp.with(imageView.context)
                .load(photoUrl)
                .placeholder(R.drawable.ic_person_black_24dp)
                .into(imageView)
    }

    private fun getString(cursor: Cursor, key: String): String {
        return cursor.getString(cursor.getColumnIndexOrThrow(key))
    }


}
