package dev.qwidget.automation.skip

import org.junit.Assert.*
import org.junit.Test

class SkipEngineTest {
    private val a = "spotify:track:0123456789ABCDEFGHIJKL"
    private val b = "spotify:track:abcdefghijklmnopqrstuv"
    private fun rule(id: String = a, count: Int? = 2, revision: String = "r1") = SkipRule(id, "Song", "Artist", count, revision)

    @Test fun `count decreases only after a different track confirms a sent skip`() {
        val book = SkipRuleBook(listOf(rule()))
        val engine = SkipEngine(book)
        assertTrue(engine.observe(a, "s", true, true, 0))
        assertEquals(2, book.find(a)!!.remaining)
        assertFalse(engine.observe(a, "s", true, true, 100))
        assertFalse(engine.observe(b, "s", true, true, 200))
        assertEquals(1, book.find(a)!!.remaining)
    }

    @Test fun `pause resume and repeated metadata never issue duplicate skip`() {
        val engine = SkipEngine(SkipRuleBook(listOf(rule(count = null))))
        assertTrue(engine.observe(a, "s", true, true, 0))
        assertFalse(engine.observe(a, "s", false, true, 20))
        assertFalse(engine.observe(a, "s", true, true, 30))
        assertFalse(engine.observe(a, "s", true, true, 9000))
    }

    @Test fun `paused missing identity or unsupported skip cannot trigger`() {
        val engine = SkipEngine(SkipRuleBook(listOf(rule())))
        assertFalse(engine.observe(a, "s", false, true, 0))
        assertFalse(engine.observe(null, "s", true, true, 1))
        assertFalse(engine.observe(a, "s", true, false, 2))
        assertTrue(engine.observe(a, "s", true, true, 3))
    }

    @Test fun `failed command never consumes or automatically retries`() {
        val book = SkipRuleBook(listOf(rule()))
        val engine = SkipEngine(book)
        assertTrue(engine.observe(a, "s", true, true, 0))
        engine.commandFailed()
        assertFalse(engine.observe(a, "s", true, true, 1))
        engine.observe(b, "s", true, true, 2)
        assertEquals(2, book.find(a)!!.remaining)
    }

    @Test fun `late confirmation or changed session never consumes`() {
        listOf("s" to 8001L, "new" to 10L).forEach { (session, time) ->
            val book = SkipRuleBook(listOf(rule()))
            val engine = SkipEngine(book)
            assertTrue(engine.observe(a, "s", true, true, 0))
            engine.observe(b, session, true, true, time)
            assertEquals(2, book.find(a)!!.remaining)
        }
    }

    @Test fun `temporary unknown metadata cannot confirm or rearm a skip`() {
        val book = SkipRuleBook(listOf(rule()))
        val engine = SkipEngine(book)
        assertTrue(engine.observe(a, "s", true, true, 0))
        engine.observe(null, "s", true, true, 1)
        assertFalse(engine.observe(a, "s", true, true, 2))
        assertEquals(2, book.find(a)!!.remaining)
    }

    @Test fun `edited rule is not decremented by an old pending confirmation`() {
        val book = SkipRuleBook(listOf(rule()))
        val engine = SkipEngine(book)
        assertTrue(engine.observe(a, "s", true, true, 0))
        book.put(rule(count = 5, revision = "new"))
        engine.observe(b, "s", true, true, 10)
        assertEquals(5, book.find(a)!!.remaining)
    }

    @Test fun `returning to a different previously seen track can skip again`() {
        val engine = SkipEngine(SkipRuleBook(listOf(rule(count = null))))
        assertTrue(engine.observe(a, "s", true, true, 0))
        engine.observe(b, "s", true, true, 1)
        assertTrue(engine.observe(a, "s", true, true, 2))
    }

    @Test fun `restored attempt prevents duplicate after process restart`() {
        val engine = SkipEngine(SkipRuleBook(listOf(rule())), SkipAttempt(a, "r1"))
        assertFalse(engine.observe(a, "new-process", true, true, 0))
        engine.observe(b, "new-process", true, true, 1)
        assertTrue(engine.observe(a, "new-process", true, true, 2))
    }

    @Test fun `failed attempt journal write prevents command`() {
        val engine = SkipEngine(SkipRuleBook(listOf(rule())), recordAttempt = { false })
        assertFalse(engine.observe(a, "s", true, true, 0))
    }

    @Test fun `rule loop is bounded to five automatic skips per thirty seconds`() {
        val engine = SkipEngine(SkipRuleBook(listOf(rule(count = null), rule(b, null))))
        repeat(5) { assertTrue(engine.observe(if (it % 2 == 0) a else b, "s", true, true, it.toLong())) }
        assertFalse(engine.observe(b, "s", true, true, 6))
        engine.observe(a, "s", true, true, 31000)
        assertTrue(engine.observe(b, "s", true, true, 31001))
    }

    @Test fun `restart cooldown cannot reset the automatic skip rate budget`() {
        val engine = SkipEngine(SkipRuleBook(listOf(rule())), restoredCooldownUntilMs = 30000)
        assertFalse(engine.observe(a, "s", true, true, 100))
        assertTrue(engine.observe(a, "s", true, true, 30000))
    }

    @Test fun `dispatch must use the observed session and still playing track`() {
        assertTrue(AutomaticSkipTarget.matches(a, "s1", a, "s1", true))
        assertFalse(AutomaticSkipTarget.matches(a, "s1", a, "s2", true))
        assertFalse(AutomaticSkipTarget.matches(a, "s1", b, "s1", true))
        assertFalse(AutomaticSkipTarget.matches(a, "s1", a, "s1", false))
    }

}
