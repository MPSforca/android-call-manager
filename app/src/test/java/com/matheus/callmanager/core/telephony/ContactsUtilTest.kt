package com.matheus.callmanager.core.telephony

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.ContactsContract
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class ContactsUtilTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private lateinit var provider: FakeContactsProvider

    @Before
    fun setUp() {
        provider = Robolectric.setupContentProvider(FakeContactsProvider::class.java, ContactsContract.AUTHORITY)
    }

    @Test
    fun `picked phone resolves to its number and name`() {
        provider.rows = listOf(arrayOf("+15551234567", "Alice"))

        val result = ContactsUtil.resolvePickedPhoneNumber(context, pickedUri())

        assertEquals("+15551234567" to "Alice", result)
    }

    @Test
    fun `picked phone without a name keeps the number`() {
        provider.rows = listOf(arrayOf("+15551234567", null))

        assertEquals("+15551234567" to null, ContactsUtil.resolvePickedPhoneNumber(context, pickedUri()))
    }

    @Test
    fun `picked phone without a number resolves to null`() {
        provider.rows = listOf(arrayOf(null, "Alice"))

        assertNull(ContactsUtil.resolvePickedPhoneNumber(context, pickedUri()))
    }

    @Test
    fun `empty pick result resolves to null`() {
        assertNull(ContactsUtil.resolvePickedPhoneNumber(context, pickedUri()))
    }

    @Test
    fun `pick resolves to null when the provider returns no cursor`() {
        provider.returnNullCursor = true

        assertNull(ContactsUtil.resolvePickedPhoneNumber(context, pickedUri()))
    }

    @Test
    fun `number with a phone lookup match is a contact`() {
        provider.rows = listOf(arrayOf(1L))

        assertTrue(ContactsUtil.isContact(context, "+15551234567"))
        assertEquals("+15551234567", provider.lastUri?.lastPathSegment)
    }

    @Test
    fun `number without a phone lookup match is not a contact`() {
        assertFalse(ContactsUtil.isContact(context, "+15551234567"))
    }

    @Test
    fun `number is not a contact when the provider returns no cursor`() {
        provider.returnNullCursor = true

        assertFalse(ContactsUtil.isContact(context, "+15551234567"))
    }

    @Test
    fun `display name comes from the phone lookup`() {
        provider.rows = listOf(arrayOf("Alice"))

        assertEquals("Alice", ContactsUtil.displayName(context, "+15551234567"))
    }

    @Test
    fun `blank display name is treated as missing`() {
        provider.rows = listOf(arrayOf("  "))

        assertNull(ContactsUtil.displayName(context, "+15551234567"))
    }

    @Test
    fun `display name is null for an unknown number`() {
        assertNull(ContactsUtil.displayName(context, "+15551234567"))
    }

    @Test
    fun `display name is null when the provider returns no cursor`() {
        provider.returnNullCursor = true

        assertNull(ContactsUtil.displayName(context, "+15551234567"))
    }

    private fun pickedUri(): Uri = Uri.withAppendedPath(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, "1")

    /** Answers every query with [rows] under the requested projection, or no cursor at all. */
    class FakeContactsProvider : ContentProvider() {
        var rows: List<Array<Any?>> = emptyList()
        var returnNullCursor = false
        var lastUri: Uri? = null

        override fun onCreate() = true

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?,
        ): Cursor? {
            lastUri = uri
            if (returnNullCursor) return null
            return MatrixCursor(projection).apply { rows.forEach { addRow(it) } }
        }

        override fun getType(uri: Uri): String? = null

        override fun insert(
            uri: Uri,
            values: ContentValues?,
        ): Uri? = null

        override fun delete(
            uri: Uri,
            selection: String?,
            selectionArgs: Array<out String>?,
        ) = 0

        override fun update(
            uri: Uri,
            values: ContentValues?,
            selection: String?,
            selectionArgs: Array<out String>?,
        ) = 0
    }
}
