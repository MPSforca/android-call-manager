package com.matheus.callmanager.data

import android.content.Context
import com.matheus.callmanager.util.ContactsUtil

/** Abstraction over contacts lookup so rule evaluation can be unit tested with a fake. */
interface ContactsLookup {
    fun isContact(number: String): Boolean

    fun displayName(number: String): String?
}

class AndroidContactsLookup(
    private val context: Context,
) : ContactsLookup {
    override fun isContact(number: String): Boolean = ContactsUtil.isContact(context, number)

    override fun displayName(number: String): String? = ContactsUtil.displayName(context, number)
}
