package dev.qwidget.automation.skip

import org.junit.Assert.*
import org.junit.Test

class SkipRuleTest {
    private val id = "spotify:track:0123456789ABCDEFGHIJKL"
    private fun rule(count: Int? = 2, revision: String = "r1") = SkipRule(id, "İsim\n=özel", "Sanatçı: &", count, revision)

    @Test fun `only canonical music track identifiers are accepted`() {
        assertEquals(id, TrackIdentity.parse(id))
        listOf(null, "", "Song - Artist", "spotify:track:bad", "spotify:episode:0123456789ABCDEFGHIJKL", "spotify:track:0123456789ABCDEFGHIJK!").forEach {
            assertNull(TrackIdentity.parse(it))
        }
    }

    @Test fun `finite and permanent rules round trip unicode and special characters`() {
        assertEquals(listOf(rule()), RuleCodec.decode(RuleCodec.encode(listOf(rule()))))
        assertEquals(listOf(rule(null)), RuleCodec.decode(RuleCodec.encode(listOf(rule(null)))))
    }

    @Test fun `corrupt or unsupported storage is ignored safely`() {
        assertTrue(RuleCodec.decode("broken").isEmpty())
        assertTrue(RuleCodec.decode("version=999\n").isEmpty())
        assertTrue(RuleCodec.decode(null).isEmpty())
    }

    @Test fun `put replaces same track rather than duplicating and rejects invalid counts`() {
        val book = SkipRuleBook()
        assertTrue(book.put(rule()))
        assertTrue(book.put(rule(null, "r2")))
        assertEquals(1, book.all().size)
        assertNull(book.find(id)!!.remaining)
        assertFalse(book.put(rule(0)))
        assertFalse(book.put(rule(1000)))
        assertNull(book.find(id)!!.remaining)
    }

    @Test fun `only matching rule revision can consume and final count removes rule`() {
        val book = SkipRuleBook(listOf(rule(1)))
        assertFalse(book.consume(rule(1, "old")))
        assertTrue(book.consume(rule(1)))
        assertTrue(book.all().isEmpty())
    }

    @Test fun `failed persistence does not mutate rules`() {
        val book = SkipRuleBook(listOf(rule()), persist = { false })
        assertFalse(book.consume(rule()))
        assertEquals(2, book.find(id)!!.remaining)
        assertFalse(book.remove(id))
        assertEquals(1, book.all().size)
    }
}
