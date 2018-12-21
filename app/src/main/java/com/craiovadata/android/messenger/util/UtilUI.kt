package com.craiovadata.android.messenger.util


import android.app.Activity
import android.app.SearchManager
import android.content.Intent
import android.database.MatrixCursor
import android.provider.BaseColumns
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import androidx.cursoradapter.widget.CursorAdapter
import androidx.cursoradapter.widget.SimpleCursorAdapter
import com.craiovadata.android.messenger.MainActivity
import com.craiovadata.android.messenger.MessagesActivity
import com.craiovadata.android.messenger.model.MyCursorAdapter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

object UtilUI{

    fun setSearch(activity: Activity, searchItem: MenuItem?) {

        val form = arrayOf(SearchManager.SUGGEST_COLUMN_TEXT_1)
        val cursorAdapter1: CursorAdapter = SimpleCursorAdapter(activity,
                android.R.layout.simple_list_item_1,
                null,
                form,
                intArrayOf(android.R.id.text1),
                0)

        val myCursorAdapter: MyCursorAdapter = MyCursorAdapter(activity, null)
        var users: QuerySnapshot? = null

        val searchView = searchItem?.actionView as SearchView
//        searchView.suggestionsAdapter = cursorAdapter
        searchView.suggestionsAdapter = myCursorAdapter

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextChange(newText: String): Boolean {
                Log.d(MainActivity.TAG, "text changed: " + newText)

                if (newText.length <= 3) return false

                FirebaseFirestore.getInstance().collection("users")
                        .whereArrayContains("keywords", newText.toLowerCase())
                        .get()
                        .addOnSuccessListener { documents ->
                            users = documents
                            for (document in documents) {
//                                Log.d(TAG, document.id + " => " + document.data)
                            }
                            addSugestions(users!!)
                        }

                return false
            }

            private fun addSugestions(users: QuerySnapshot) {

                val columns = arrayOf(
                        BaseColumns._ID,
                        SearchManager.SUGGEST_COLUMN_TEXT_1,
                        SearchManager.SUGGEST_COLUMN_INTENT_DATA,
                        "photoUrl"
                )

                val cursor = MatrixCursor(columns)
                for (i in 0 until users.size()) {
                    val name = users.elementAt(i)["name"]
                    val photoUrl = users.elementAt(i)["photoUrl"]
                    val tmp = arrayOf(Integer.toString(i), name, name, photoUrl)
                    cursor.addRow(tmp)

                }

//                cursorAdapter.swapCursor(cursor)
                myCursorAdapter.swapCursor(cursor)
            }

            override fun onQueryTextSubmit(query: String): Boolean {
                // task HERE
                return false
            }

        })

        searchView.setOnSuggestionListener(object : SearchView.OnSuggestionListener {
            override fun onSuggestionClick(position: Int): Boolean {

                val palUserSnap = users?.elementAt(position)

                searchView.setQuery(palUserSnap?.get("name").toString(), false)
                searchView.clearFocus()
                searchItem.collapseActionView()

                val intent = Intent(activity, MessagesActivity::class.java)
                intent.putExtra(MessagesActivity.KEY_USER_ID, palUserSnap?.id)
                activity.startActivity(intent)
                activity.overridePendingTransition(com.craiovadata.android.messenger.R.anim.slide_in_from_right, com.craiovadata.android.messenger.R.anim.slide_out_to_left)
                return true
            }

            override fun onSuggestionSelect(position: Int): Boolean {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

        })

    }
}