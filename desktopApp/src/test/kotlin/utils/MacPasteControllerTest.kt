package utils

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MacPasteControllerTest {
    @Test
    fun waitsForFocusAndReleasedModifiersThenSendsOnceToCapturedProcess() = runTest {
        val platform = FakePlatform()
        platform.ready = false
        val job = launch { assertEquals(MacPasteResult.SENT, MacPasteController(platform).paste(42)) }
        advanceTimeBy(300)
        assertEquals(0, platform.sends)
        platform.ready = true
        advanceTimeBy(99)
        assertEquals(0, platform.sends)
        job.join()
        assertEquals(listOf(42), platform.activations)
        assertEquals(listOf(42), platform.recipients)
    }

    @Test
    fun focusChangeDuringSettlingDoesNotSendPaste() = runTest {
        val platform = FakePlatform()
        val job = launch { assertEquals(MacPasteResult.TIMED_OUT, MacPasteController(platform).paste(42)) }
        runCurrent()
        advanceTimeBy(50)
        platform.ready = false
        job.join()
        assertEquals(0, platform.sends)
    }

    @Test
    fun cancellationOnReopenPreventsDelayedPaste() = runTest {
        val platform = FakePlatform()
        val job = launch { MacPasteController(platform).paste(42) }
        runCurrent()
        advanceTimeBy(50)
        job.cancel()
        job.join()
        assertTrue(job.isCancelled)
        assertEquals(0, platform.sends)
    }

    @Test
    fun terminatedTargetAndMissingTargetNeverReceiveKeys() = runTest {
        val platform = FakePlatform()
        platform.activationAllowed = false
        assertEquals(MacPasteResult.NO_TARGET, MacPasteController(platform).paste(0))
        assertTrue(platform.activations.isEmpty())
        assertEquals(MacPasteResult.ACTIVATION_FAILED, MacPasteController(platform).paste(42))
        assertEquals(0, platform.sends)
    }

    @Test
    fun revokedPermissionStopsWithoutRetrying() = runTest {
        val platform = FakePlatform()
        platform.postResult = -1
        assertEquals(MacPasteResult.NOT_TRUSTED, MacPasteController(platform).paste(42))
        assertEquals(1, platform.attempts)
        assertEquals(0, platform.sends)
    }

    @Test
    fun transientFocusRaceRetriesOnlyUntilOneSend() = runTest {
        val platform = FakePlatform()
        platform.rejectedAttempts = 1
        assertEquals(MacPasteResult.SENT, MacPasteController(platform).paste(42))
        assertEquals(2, platform.attempts)
        assertEquals(1, platform.sends)
        assertEquals(listOf(42), platform.activations)
    }

    private class FakePlatform : MacPastePlatform {
        var activationAllowed = true
        var ready = true
        var postResult = 1
        var rejectedAttempts = 0
        var attempts = 0
        val activations = mutableListOf<Int>()
        val recipients = mutableListOf<Int>()
        val sends get() = recipients.size
        override fun activate(pid: Int): Boolean {
            activations.add(pid)
            return activationAllowed
        }
        override fun canPaste(pid: Int) = ready
        override fun postPaste(pid: Int): Int {
            attempts++
            if (!ready || attempts <= rejectedAttempts) return 0
            if (postResult == 1) recipients.add(pid)
            return postResult
        }
    }
}
