package com.craiovadata.android.messenger.util


import android.app.Activity
import android.content.Intent
import android.database.MatrixCursor
import android.provider.BaseColumns
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import com.craiovadata.android.messenger.MessagesActivity
import com.craiovadata.android.messenger.model.MyCursorAdapter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

object UtilUI {

    fun setSearch(activity: Activity, searchItem: MenuItem?) {
        var snapshot: QuerySnapshot? = null
        val searchView = searchItem?.actionView as SearchView
        val columns = arrayOf(BaseColumns._ID, "name", "photoUrl")

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onQueryTextChange(newText: String): Boolean {
                if (newText.length <= 3) return false
                FirebaseFirestore.getInstance().collection("userKeywords")
                        .whereArrayContains("keywords", newText.toLowerCase())
                        .get()
                        .addOnSuccessListener { documents ->
                            snapshot = documents
                            addSugestions(snapshot)
                        }
                return true
            }

            private fun addSugestions(querySnapshot: QuerySnapshot?) {
                if (querySnapshot == null) return


                val cursor = MatrixCursor(columns)

                for (i in 0 until querySnapshot.size()) {
                    val userSnap = querySnapshot.elementAt(i)
                    val name = userSnap["name"]
                    val photoUrl = userSnap["photoUrl"]
                    cursor.addRow(arrayOf(i.toString(), name, photoUrl))
                }

                if (searchView.suggestionsAdapter == null)
                    searchView.suggestionsAdapter = MyCursorAdapter(activity, cursor)
                else
                    searchView.suggestionsAdapter.changeCursor(cursor)


            }


        })

        searchView.setOnSuggestionListener(object : SearchView.OnSuggestionListener {
            override fun onSuggestionSelect(position: Int): Boolean {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onSuggestionClick(position: Int): Boolean {

                val palUserSnap = snapshot?.elementAt(position)
//                searchView.setQuery(palUserSnap?.get("name").toString(), false)
//                searchView.clearFocus()
                searchItem.collapseActionView()

                val intent = Intent(activity, MessagesActivity::class.java)
                intent.putExtra(MessagesActivity.KEY_USER_ID, palUserSnap?.id)
                activity.startActivity(intent)
                activity.overridePendingTransition(com.craiovadata.android.messenger.R.anim.slide_in_from_right, com.craiovadata.android.messenger.R.anim.slide_out_to_left)

                return true
            }


        })

    }
}