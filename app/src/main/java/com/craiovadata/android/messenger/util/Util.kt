package com.craiovadata.android.messenger.util

object Util {

    fun buildKeywords(email: String, displayName: String): MutableList<String> {
        val names = displayName.toLowerCase().split(" ")    // ["dan" "alboteanu" "cristinel"]

        val keywords = ArrayList<String>()

        keywords.addAll(names)
        keywords.add(email.toLowerCase())
        keywords.add(displayName.toLowerCase())

        // add name and parts of the name:  alb, albo, albot, albote
        for (word in keywords) {
            if (word.length >= 4) {
                if (!keywords.contains(word))
                    keywords.add(word)
                val endIndex = if (word.length > 7) 7 else word.length - 1
                for (i in 4..endIndex) {
                    val substr = word.substring(0, i)
                    if (!keywords.contains(substr))
                        keywords.add(substr)
                }
            }
        }

        val username = usernameFromEmail(email)
        keywords.add(username)   //  add "alboteanud"

        return keywords as MutableList<String>
    }

    private fun usernameFromEmail(email: String): String {
        return if (email.contains("@")) {
            email.split("@".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
        } else {
            email
        }
    }



}
