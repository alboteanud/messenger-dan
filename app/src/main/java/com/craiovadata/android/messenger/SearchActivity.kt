package com.craiovadata.android.messenger

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.craiovadata.android.messenger.adapter.SearchAdapter
import com.craiovadata.android.messenger.util.KEYWORDS
import com.craiovadata.android.messenger.util.KEY_USER_ID
import com.craiovadata.android.messenger.util.USER_KEYWORDS
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_search.*


class SearchActivity : Activity(), SearchAdapter.OnUserSelectedListener, TextWatcher {

    lateinit var searchRef: CollectionReference
    lateinit var searchAdapter: SearchAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        editText.requestFocus()
        backBtn.setOnClickListener { onBackArrowClicked() }

        searchRef = FirebaseFirestore.getInstance().collection(USER_KEYWORDS)

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
        query = query.limit(50L)
        searchAdapter.setQuery(query)
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
    }

    private fun onBackArrowClicked() {
        onBackPressed()
    }

    override fun onUserSelected(documentSnapshot: DocumentSnapshot) {
        val intent = Intent(this@SearchActivity, DetailsActivity::class.java)
        intent.putExtra(KEY_USER_ID, documentSnapshot.id)
        startActivity(intent)
        overridePendingTransition(com.craiovadata.android.messenger.R.anim.slide_in_from_right, com.craiovadata.android.messenger.R.anim.slide_out_to_left)
        finish()
    }


}
