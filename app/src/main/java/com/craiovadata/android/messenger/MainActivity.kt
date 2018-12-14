package com.craiovadata.android.messenger

import android.app.Activity
import android.app.SearchManager
import android.content.Intent
import android.database.MatrixCursor
import android.os.Bundle
import android.provider.BaseColumns
import android.text.Html
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.cursoradapter.widget.CursorAdapter
import androidx.cursoradapter.widget.SimpleCursorAdapter
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.craiovadata.android.messenger.adapter.RoomAdapter
import com.craiovadata.android.messenger.model.User
import com.craiovadata.android.messenger.util.Util.getKeywords
import com.craiovadata.android.messenger.viewmodel.MainActivityViewModel
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(),
        FilterDialogFragment.FilterListener,
        RoomAdapter.OnRoomSelectedListener {

    lateinit var firestore: FirebaseFirestore
    lateinit var query: Query

    private lateinit var filterDialog: FilterDialogFragment
    lateinit var adapter: RoomAdapter

    private lateinit var viewModel: MainActivityViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)


        viewModel = ViewModelProviders.of(this).get(MainActivityViewModel::class.java)

        FirebaseFirestore.setLoggingEnabled(true)
        firestore = FirebaseFirestore.getInstance()

        initRoomAdapter()

    }

    private fun initRoomAdapter() {
        val user = FirebaseAuth.getInstance().currentUser ?: return

        toolbar.title = user.displayName

        query = firestore.collection("users").document(user.uid).collection("rooms")
                .orderBy("lastMsgTime", Query.Direction.DESCENDING)
                .limit(LIMIT.toLong())

        adapter = object : RoomAdapter(query, this@MainActivity) {
            override fun onDataChanged() {
                // Show/hide content if the query returns empty.
                if (itemCount == 0) {
                    recyclerRestaurants.visibility = View.GONE
                    viewEmpty.visibility = View.VISIBLE
                } else {
                    recyclerRestaurants.visibility = View.VISIBLE
                    viewEmpty.visibility = View.GONE
                }
            }

            override fun onError(e: FirebaseFirestoreException) {
                // Show a snackbar on errors
                Snackbar.make(findViewById(android.R.id.content),
                        "Error: check logs for info.", Snackbar.LENGTH_LONG).show()
            }
        }

        recyclerRestaurants.layoutManager = LinearLayoutManager(this)
        recyclerRestaurants.adapter = adapter

        filterDialog = FilterDialogFragment()

        filterBar.setOnClickListener { onFilterClicked() }
        buttonClearFilter.setOnClickListener { onClearFilterClicked() }

    }

    public override fun onStart() {
        super.onStart()

        // Start sign in if necessary
        if (shouldStartSignIn()) {
            startSignIn()
            return
        }

        // Apply filters
        onFilter(viewModel.filters)

        // Start listening for Firestore updates
        adapter.startListening()

    }

    public override fun onStop() {
        super.onStop()
        if (::adapter.isInitialized)
            adapter.stopListening()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        setSearch(menu.findItem(R.id.action_search))
        return true
//        return super.onCreateOptionsMenu(menu)
    }

    private fun setSearch(searchItem: MenuItem?) {
        val searchView = searchItem!!.actionView as SearchView
        val form = arrayOf(SearchManager.SUGGEST_COLUMN_TEXT_1)
        val suggestionAdapter: CursorAdapter = SimpleCursorAdapter(this@MainActivity,
                android.R.layout.simple_list_item_1,
                null,
                form,
                intArrayOf(android.R.id.text1),
                0);
        var users: QuerySnapshot? = null

        searchView.setSuggestionsAdapter(suggestionAdapter)

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextChange(newText: String): Boolean {
                Log.d(TAG, "text changed: " + newText)

                if (newText.length <= 3) return false

                firestore.collection("users")
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
                        SearchManager.SUGGEST_COLUMN_INTENT_DATA
                )

                val cursor = MatrixCursor(columns)
                for (i in 0 until users.size()) {
                    val name = users.elementAt(i)["name"]
                    val tmp = arrayOf(Integer.toString(i), name, name)
                    cursor.addRow(tmp)

                }

                suggestionAdapter.swapCursor(cursor)
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

                val intent = Intent(this@MainActivity, MessagesActivity::class.java)
                intent.putExtra(MessagesActivity.KEY_ROOM_ID, palUserSnap?.id)
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left)

                return true;
            }

            override fun onSuggestionSelect(position: Int): Boolean {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

        })

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_sign_out -> {
                AuthUI.getInstance().signOut(this)
                startSignIn()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)
            viewModel.isSigningIn = false

            if (resultCode == Activity.RESULT_OK) {
                // Successfully signed in
                val firebaseUser = FirebaseAuth.getInstance().currentUser
                if (firebaseUser != null) {
                    writeNewUser(firebaseUser)
                    initRoomAdapter()
                    adapter.startListening()
                }
            } else {
                if (response == null) {
                    // User pressed the back button.
                    finish()
                } else if (response.error != null && response.error!!.errorCode == ErrorCodes.NO_NETWORK) {
                    showSignInErrorDialog(R.string.message_no_network)
                } else {
                    showSignInErrorDialog(R.string.message_unknown)
                }
            }
        }
    }

    private fun writeNewUser(firebaseUser: FirebaseUser) {

        val email = firebaseUser.email.toString()
        val displayName = firebaseUser.displayName.toString()

        val keywords = getKeywords(email, displayName)

        val user = User(email, displayName, firebaseUser.photoUrl.toString(), keywords)
        val uid = firebaseUser.uid

        firestore.collection("users").document(uid).set(user)
    }

    private fun onFilterClicked() {
        // Show the dialog containing filter options
        filterDialog.show(supportFragmentManager, FilterDialogFragment.TAG)
    }

    private fun onClearFilterClicked() {
        filterDialog.resetFilters()

        onFilter(Filters.default)
    }

    override fun onRoomSelected(room: DocumentSnapshot) {
        // Go to the details page for the selected room
        val intent = Intent(this, MessagesActivity::class.java)
        intent.putExtra(MessagesActivity.KEY_ROOM_ID, room.reference.id)

        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left)
    }

    override fun onFilter(filters: Filters) {
        val firebaseUser = FirebaseAuth.getInstance().currentUser!!.uid
        val ref = firestore.collection("users").document(firebaseUser).collection("rooms")
        // Construct query basic query
        var query: Query = ref

        // Limit items
        query = query.limit(LIMIT.toLong())

        // Update the query
        adapter.setQuery(query)

        // Set header
        textCurrentSearch.text = Html.fromHtml(filters.getSearchDescription(this))

        // Save filters
        viewModel.filters = filters
    }

    private fun shouldStartSignIn(): Boolean {
        return !viewModel.isSigningIn && FirebaseAuth.getInstance().currentUser == null
    }

    private fun startSignIn() {
        // Sign in with FirebaseUI
        val intent = AuthUI.getInstance().createSignInIntentBuilder()
                .setAvailableProviders(listOf(
                        AuthUI.IdpConfig.EmailBuilder().build(),
                        AuthUI.IdpConfig.PhoneBuilder().build(),
                        AuthUI.IdpConfig.GoogleBuilder().build()
//                        AuthUI.IdpConfig.FacebookBuilder().build(),
//                        AuthUI.IdpConfig.TwitterBuilder().build()
                ))
                .setIsSmartLockEnabled(false)
                .build()

        startActivityForResult(intent, RC_SIGN_IN)
        viewModel.isSigningIn = true
    }

    private fun showSignInErrorDialog(@StringRes message: Int) {
        val dialog = AlertDialog.Builder(this)
                .setTitle(R.string.title_sign_in_error)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(R.string.option_retry) { _, _ -> startSignIn() }
                .setNegativeButton(R.string.option_exit) { _, _ -> finish() }.create()

        dialog.show()
    }

    companion object {

        private const val TAG = "MainActivity"

        private const val RC_SIGN_IN = 9001

        private const val LIMIT = 50
    }
}
