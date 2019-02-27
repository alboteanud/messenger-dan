package com.craiovadata.android.messenger

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.craiovadata.android.messenger.adapter.PeopleAdapter
import com.craiovadata.android.messenger.model.Person
import com.craiovadata.android.messenger.model.Person.Companion.STATUS_TXT
import com.craiovadata.android.messenger.util.KEY_USER_ID
import com.craiovadata.android.messenger.util.KEY_USER_NAME
import com.craiovadata.android.messenger.util.KEY_USER_PHOTO_URL
import com.craiovadata.android.messenger.util.USERS
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_messages.*
import kotlinx.android.synthetic.main.people_list.*
import java.util.*


class AllPeopleActivity : AppCompatActivity(), PeopleAdapter.OnItemSelectedListener {
    private var adapter: PeopleAdapter? = null
    private lateinit var firestore: FirebaseFirestore
    private lateinit var menu: Menu

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_people)
        setSupportActionBar(toolbar)
        firestore = FirebaseFirestore.getInstance()
        setUpRecyclerView()
        updateVisitTime()
    }

    private fun updateVisitTime() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val data = HashMap<String, Any?>()

        data["timestamp"] = FieldValue.serverTimestamp()
        firestore.collection(USERS).document(user.uid).update(data)
    }

    private fun setUpRecyclerView() {

        val query = firestore.collection(USERS)
                .whereEqualTo("visible", true)
//                    .orderBy(LAST_MODIF, Query.Direction.DESCENDING)

        val options = FirestoreRecyclerOptions.Builder<Person>()
                .setQuery(query, Person::class.java)
                .build()

        adapter = PeopleAdapter(options, this@AllPeopleActivity)
        recyclerPeople.adapter = adapter
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    // onBackPressed
    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_from_left, R.anim.slide_out_to_right)
    }

    public override fun onStart() {
        super.onStart()
        adapter?.startListening()
    }

    public override fun onStop() {
        super.onStop()
        adapter?.stopListening()
    }

    override fun onItemSelected(person: Person) {
        val intent = Intent(this, DetailsActivity::class.java)
                .putExtra(KEY_USER_ID, person.uid)
                .putExtra(KEY_USER_NAME, person.name)
                .putExtra(KEY_USER_PHOTO_URL, person.photoUrl)

        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_people, menu)
        this.menu = menu
        checkVisibleState()
        return super.onCreateOptionsMenu(menu)
    }

    private fun checkVisibleState() {
        val user = FirebaseAuth.getInstance().currentUser ?: return

        val docRef = firestore.collection(USERS).document(user.uid)

        docRef.get().addOnSuccessListener { document ->
            if (document != null) {
                Log.d(TAG, "DocumentSnapshot data: " + document.data)
                val userData = document.data

                val iAmVisible = userData?.get("visible")
                if (iAmVisible is Boolean) setVisibleUI(iAmVisible)

            } else {
                Log.d(TAG, "No such document")
            }
        }
    }

    private fun setVisibleUI(iAmVisible: Boolean) {
        menu.findItem(R.id.menu_hide_me).isVisible = iAmVisible
        menu.findItem(R.id.menu_show_me).isVisible = !iAmVisible
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_show_me -> {
                val user = FirebaseAuth.getInstance().currentUser ?: return true

                val data = HashMap<String, Any>()
                data["visible"] = true
                firestore.collection(USERS).document(user.uid).update(data)
                setVisibleUI(true)
            }
            R.id.menu_hide_me -> {
                val user = FirebaseAuth.getInstance().currentUser ?: return true
                val data = HashMap<String, Any>()
                data["visible"] = false
                firestore.collection(USERS).document(user.uid).update(data)
                setVisibleUI(false)
            }
            R.id.menu_change_status -> {
                builtDialogSetStatus()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun builtDialogSetStatus() {
        val alert = AlertDialog.Builder(this)

        alert.setTitle(R.string.dialogTitleEnterStatus)
//        alert.setMessage("Enter Your status Message")

        val li = LayoutInflater.from(this)
        val myView = li.inflate(R.layout.dialog_status, null)
//        alert.setView(R.layout.dialog_status)
        alert.setView(myView)

        alert.setPositiveButton(android.R.string.yes) { dialog, whichButton ->

            val user = FirebaseAuth.getInstance().currentUser ?: return@setPositiveButton
            val ref = firestore.collection(USERS).document(user.uid)
            val data = HashMap<String, Any>()

            val edit = myView.findViewById(R.id.editTextDialogStatus) as EditText?
            val txt = edit?.text.toString()
            data[STATUS_TXT] = txt
            ref.update(data)
        }

        alert.setNegativeButton(android.R.string.cancel) { dialog, whichButton ->
            // what ever you want to do with No option.
        }

        alert.show()
    }

    companion object {
        const val TAG = "AllPeopleActivity"
    }

}

