package dev.qwidget.probe

enum class StartSession { MISSING, WAITING, READY, PLAYING }
enum class StartResult { ALREADY_PLAYING, COMMAND_SENT, UNAVAILABLE }

/** One wake request and at most eight seconds for a usable Spotify session. */
class SpotifyStartEngine(
    private val session: () -> StartSession,
    private val start: () -> Boolean,
    private val play: () -> Boolean,
    private val waitForSession: suspend () -> Unit,
    private val canResume: () -> Boolean = { true },
) {
    suspend fun resume(): StartResult {
        if (!canResume()) return StartResult.UNAVAILABLE
        when (session()) {
            StartSession.PLAYING -> return StartResult.ALREADY_PLAYING
            StartSession.READY -> return sendPlay()
            StartSession.WAITING -> Unit
            StartSession.MISSING -> if (!start()) return StartResult.UNAVAILABLE
        }
        repeat(16) {
            waitForSession()
            if (!canResume()) return StartResult.UNAVAILABLE
            when (session()) {
                StartSession.PLAYING -> return StartResult.ALREADY_PLAYING
                StartSession.READY -> return sendPlay()
                else -> Unit
            }
        }
        return StartResult.UNAVAILABLE
    }

    private fun sendPlay() = if (canResume() && play()) StartResult.COMMAND_SENT else StartResult.UNAVAILABLE
}
