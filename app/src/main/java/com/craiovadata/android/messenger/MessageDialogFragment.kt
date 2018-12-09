package com.craiovadata.android.messenger


import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.craiovadata.android.messenger.model.Message
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.dialog_rating.*
import kotlinx.android.synthetic.main.dialog_rating.view.*

/**
 * Dialog Fragment containing rating form.
 */
class MessageDialogFragment : DialogFragment() {

    private var msgListener: MsgListener? = null

    internal interface MsgListener {

        fun onMessage(message: Message)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.dialog_rating, container, false)

        v.restaurantFormButton.setOnClickListener { onSubmitClicked() }
        v.restaurantFormCancel.setOnClickListener { onCancelClicked() }

        return v
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        if (context is MsgListener) {
            msgListener = context
        }
    }

    override fun onResume() {
        super.onResume()
        dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun onSubmitClicked() {
        val user = FirebaseAuth.getInstance().currentUser
        user?.let {
            val message = Message(
                    user,
                    restaurantFormText.text.toString())

            msgListener?.onMessage(message)
        }

        dismiss()
    }

    private fun onCancelClicked() {
        dismiss()
    }

    companion object {

        const val TAG = "MsgDialog"
    }
}
