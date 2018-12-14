package com.craiovadata.android.messenger

import android.content.Context
import android.text.TextUtils
import com.google.firebase.firestore.Query

/**
 * Object for passing filters around.
 */
class Filters {

    var category: String? = null
    var city: String? = null
    var price = -1
    var sortBy: String? = null
    var sortDirection: Query.Direction = Query.Direction.DESCENDING

    fun hasCategory(): Boolean {
        return !TextUtils.isEmpty(category)
    }

    fun hasCity(): Boolean {
        return !TextUtils.isEmpty(city)
    }

    fun hasPrice(): Boolean {
        return price > 0
    }

    fun hasSortBy(): Boolean {
        return !TextUtils.isEmpty(sortBy)
    }

    fun getSearchDescription(context: Context): String {
        val desc = StringBuilder()

        if (category == null && city == null) {
            desc.append("<b>")
            desc.append(context.getString(R.string.all_restaurants))
            desc.append("</b>")
        }

        if (category != null) {
            desc.append("<b>")
            desc.append(category)
            desc.append("</b>")
        }

        if (category != null && city != null) {
            desc.append(" in ")
        }

        if (city != null) {
            desc.append("<b>")
            desc.append(city)
            desc.append("</b>")
        }

        if (price > 0) {
            desc.append(" for ")
            desc.append("<b>")
            desc.append("</b>")
        }

        return desc.toString()
    }


    companion object {

        val default: Filters
            get() {
                val filters = Filters()
                filters.sortDirection = Query.Direction.DESCENDING

                return filters
            }
    }
}
