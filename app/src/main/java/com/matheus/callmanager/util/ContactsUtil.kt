package com.matheus.callmanager.util

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract

object ContactsUtil {
    /** Resolves a URI returned by ACTION_PICK on Phone.CONTENT_TYPE into a (number, name) pair. */
    fun resolvePickedPhoneNumber(
        context: Context,
        uri: Uri,
    ): Pair<String, String?>? {
        context.contentResolver
            .query(
                uri,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ),
                null,
                null,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val number = cursor.getString(0) ?: return null
                    val name = cursor.getString(1)
                    return number to name
                }
            }
        return null
    }

    /** True if [number] matches an entry in the user's contacts. */
    fun isContact(
        context: Context,
        number: String,
    ): Boolean {
        val uri =
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI
                .buildUpon()
                .appendPath(number)
                .build()
        context.contentResolver
            .query(
                uri,
                arrayOf(ContactsContract.PhoneLookup._ID),
                null,
                null,
                null,
            )?.use { cursor ->
                return cursor.moveToFirst()
            }
        return false
    }

    /** Best-effort display name for [number], or null if it isn't in contacts. */
    fun displayName(
        context: Context,
        number: String,
    ): String? {
        val uri =
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI
                .buildUpon()
                .appendPath(number)
                .build()
        context.contentResolver
            .query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null,
                null,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return cursor.getString(0)?.takeIf { it.isNotBlank() }
                }
            }
        return null
    }
}
