package com.sforca.callmanager.core.designsystem

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.sforca.callmanager.core.model.ScreeningReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScreeningReasonTextTest {
    private val context: Context = ApplicationProvider.getApplicationContext<Application>()

    private val allReasons =
        listOf(
            ScreeningReason.BlockList,
            ScreeningReason.AllowList,
            ScreeningReason.NamePattern,
            ScreeningReason.KnownContact,
            ScreeningReason.RecentlyAccepted(30),
            ScreeningReason.RecentlyCalled(30),
            ScreeningReason.UnknownNumber,
            ScreeningReason.NoMatchingBlockRule,
        )

    @Test
    fun `english text describes each reason`() {
        assertEquals("On your block list", ScreeningReason.BlockList.text(context))
        assertEquals("On your allow list", ScreeningReason.AllowList.text(context))
        assertEquals("Caller name matches an allow pattern", ScreeningReason.NamePattern.text(context))
        assertEquals("Known contact", ScreeningReason.KnownContact.text(context))
        assertEquals("Accepted within the last 30 days", ScreeningReason.RecentlyAccepted(30).text(context))
        assertEquals("Accepted within the last day", ScreeningReason.RecentlyAccepted(1).text(context))
        assertEquals("You called them within the last 7 days", ScreeningReason.RecentlyCalled(7).text(context))
        assertEquals("You called them within the last day", ScreeningReason.RecentlyCalled(1).text(context))
        assertEquals("Unknown number", ScreeningReason.UnknownNumber.text(context))
        assertEquals("No matching block rule", ScreeningReason.NoMatchingBlockRule.text(context))
    }

    @Test
    @Config(qualifiers = "es")
    fun `spanish text differs from english for every reason`() {
        assertEquals("En tu lista de bloqueados", ScreeningReason.BlockList.text(context))
        assertEquals("Atendida en los últimos 30 días", ScreeningReason.RecentlyAccepted(30).text(context))
        assertEquals("Lo llamaste en el último día", ScreeningReason.RecentlyCalled(1).text(context))
        allReasons.forEach { assertTrue(it.text(context).isNotBlank()) }
    }

    @Test
    @Config(qualifiers = "pt-rBR")
    fun `portuguese text differs from english for every reason`() {
        assertEquals("Na sua lista de bloqueados", ScreeningReason.BlockList.text(context))
        assertEquals("Atendida em até 30 dias", ScreeningReason.RecentlyAccepted(30).text(context))
        assertEquals("Você ligou para o contato em até 1 dia", ScreeningReason.RecentlyCalled(1).text(context))
        allReasons.forEach { assertTrue(it.text(context).isNotBlank()) }
    }

    @Test
    fun `codes are stable and unique`() {
        val codes = allReasons.map { it.code }
        assertEquals(codes.size, codes.toSet().size)
        assertEquals("block_list", ScreeningReason.BlockList.code)
        assertEquals("unknown_number", ScreeningReason.UnknownNumber.code)
    }
}
