package com.craiovadata.android.messenger

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import com.craiovadata.android.messenger.adapter.SearchAdapter
import com.craiovadata.android.messenger.model.Person
import com.craiovadata.android.messenger.util.*
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_search.*


class SearchActivity : AppCompatActivity(), SearchAdapter.OnUserSelectedListener, TextWatcher {

    private lateinit var collectionReference: CollectionReference
    private var adapter: SearchAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        setSupportActionBar(toolbar)
        collectionReference = FirebaseFirestore.getInstance().collection(USERS)
        editText.requestFocus()

        editText.addTextChangedListener(this@SearchActivity)
//        recyclerSearchedUsers.setHasFixedSize(true)
    }

    override fun afterTextChanged(s: Editable?) {
        val str = s.toString()
        adapter?.stopListening()
        if (str.length < 4) return

        setUpRecyclerView(str)
    }



    private fun setUpRecyclerView(str: String) {

        val query = collectionReference.whereArrayContains(KEYWORDS, str)
                .limit(10L)

        val options = FirestoreRecyclerOptions.Builder<Person>()
                .setQuery(query, Person::class.java)
                .build()

        adapter = SearchAdapter(options, this@SearchActivity)

        recyclerSearchedUsers.adapter = adapter
        adapter?.startListening()
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onUserSelected(person: Person) {
        val intent = Intent(this@SearchActivity, DetailsActivity::class.java)
                .putExtra(KEY_USER_ID, person.uid)
                .putExtra(KEY_USER_NAME, person.name)
                .putExtra(KEY_USER_PHOTO_URL, person.photoUrl)

        startActivity(intent)
        finish()
    }

    // onBackPressed
    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left)
    }

    override fun onStop() {
        super.onStop()
        adapter?.stopListening()
    }

}
