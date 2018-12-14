package com.craiovadata.android.messenger.util

object Util {

    fun buildKeywords(email: String, displayName: String): MutableList<String> {
        val emailLC = email.toLowerCase()
        val displayNameLC = displayName.toLowerCase()

        val keywords: ArrayList<String> = arrayListOf()

        val username = usernameFromEmail(emailLC)               // alboteanud
        keywords.add(username)                                  // add directly to list (derivatives are added from email)

        addWordDerivToList(emailLC, keywords)                   // alboteanud@gmail.com
        addWordDerivToList(displayNameLC, keywords)

        val displayNameS = wordsFromString(displayNameLC)       // alb, albo, albot, albote
        for (word in displayNameS) addWordDerivToList(word, keywords)

        return keywords
    }

    // add word and derivatives to list
    private fun addWordDerivToList(word: String, keywords: ArrayList<String>) {
        if (word.length >= 4) {
            if (!keywords.contains(word))
                keywords.add(word)
            val endLettersIndex = if (word.length > 7) 7 else word.length - 1
            for (i in 4..endLettersIndex) {
                val substr = word.substring(0, i)
                if (!keywords.contains(substr))
                    keywords.add(substr)
            }
        }

    }

    private fun usernameFromEmail(email: String): String {
        return if (email.contains("@")) {
            email.split("@".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
        } else {
            email
        }
    }

    private fun wordsFromString(s: String): Array<String> {
        return if (s.contains(" ")) {
            s.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        } else {
            arrayOf(s)
        }
    }


}
