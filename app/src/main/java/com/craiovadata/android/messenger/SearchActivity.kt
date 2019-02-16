package com.craiovadata.android.messenger

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.craiovadata.android.messenger.adapter.SearchAdapter
import com.craiovadata.android.messenger.model.UserToSearch
import com.craiovadata.android.messenger.util.*
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_search.*


class SearchActivity : AppCompatActivity(), SearchAdapter.OnUserSelectedListener, TextWatcher {

    lateinit var searchRef: CollectionReference
    lateinit var searchAdapter: SearchAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        setSupportActionBar(toolbar)

        editText.requestFocus()

        searchRef = FirebaseFirestore.getInstance().collection(USERS)

        searchAdapter = object : SearchAdapter(null, this@SearchActivity) {
            override fun onDataChanged() {
                super.onDataChanged()
                if (itemCount == 0) {
                    recyclerSearchedUsers.visibility = View.GONE
                } else {
                    recyclerSearchedUsers.visibility = View.VISIBLE
                }
            }
        }
        recyclerSearchedUsers.layoutManager = LinearLayoutManager(this)
        recyclerSearchedUsers.adapter = searchAdapter

        editText.addTextChangedListener(this@SearchActivity)
    }

    override fun afterTextChanged(s: Editable?) {
        val str = s.toString()
        if (str.length < 4) return
        var query = searchRef.whereArrayContains(KEYWORDS, str)
        query = query.limit(20L)
        searchAdapter.setQuery(query)
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onUserSelected(userToSearch: UserToSearch) {
        val intent = Intent(this@SearchActivity, DetailsActivity::class.java)
                .putExtra(KEY_USER_ID, userToSearch.uid)
                .putExtra(KEY_USER_NAME, userToSearch.name)
                .putExtra(KEY_USER_PHOTO_URL, userToSearch.photoUrl)

        startActivity(intent)
        finish()
    }

    // onBackPressed
    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left)
    }


}
