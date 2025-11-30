package com.github.thinkami.railroads.coroutines

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicReference
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ProjectCoroutineScopeProviderTest {

    private val testDispatchers = DispatcherSet(
        default = Dispatchers.Default,
        io = Dispatchers.IO,
        edt = Dispatchers.Unconfined
    )

    @Test
    fun `uses provided dispatcher set`() {
        val provider = ProjectCoroutineScopeProvider(testDispatchers)

        assertSame(testDispatchers, provider.dispatcherSet())
    }

    @Test
    fun `scope keeps siblings alive when one child fails`() = runBlocking {
        val provider = ProjectCoroutineScopeProvider(testDispatchers)

        val survivor = provider.scope.launch {
            suspendCancellableCoroutine<Unit> { }
        }
        val failure = AtomicReference<Throwable?>()
        val failing = provider.scope.launch {
            throw IllegalStateException("boom")
        }
        failing.invokeOnCompletion { failure.set(it) }

        failing.join()

        assertTrue(survivor.isActive)
        assertTrue(failure.get() is IllegalStateException)
        provider.dispose()
    }

    @Test
    fun `scope and children are cancelled on dispose`() = runBlocking {
        val provider = ProjectCoroutineScopeProvider(testDispatchers)
        val parentJob = provider.scope.coroutineContext[Job]!!
        val child = provider.scope.launch(start = CoroutineStart.UNDISPATCHED) {
            suspendCancellableCoroutine<Unit> { }
        }

        provider.dispose()

        assertTrue(child.isCancelled)
        assertTrue(parentJob.isCancelled)
        assertThrows<IllegalStateException> { provider.scope }
    }

    @Test
    fun `reset cancels old scope and swaps dispatcher set`() = runBlocking {
        val provider = ProjectCoroutineScopeProvider(testDispatchers)
        val child = provider.scope.launch(start = CoroutineStart.UNDISPATCHED) {
            suspendCancellableCoroutine<Unit> { }
        }
        val newDispatchers = DispatcherSet(
            default = Dispatchers.IO,
            io = Dispatchers.Default,
            edt = Dispatchers.Unconfined
        )

        provider.reset(newDispatchers)

        assertSame(newDispatchers, provider.dispatcherSet())
        assertTrue(child.isCancelled)
        val job = provider.scope.coroutineContext[Job]
        assertTrue(job is Job)
    }

    @Test
    fun `reset creates new scope after cancellation`() = runBlocking {
        val provider = ProjectCoroutineScopeProvider(testDispatchers)
        val originalJob = provider.scope.coroutineContext[Job]!!
        provider.reset(testDispatchers)

        val newJob = provider.scope.coroutineContext[Job]!!

        assertTrue(originalJob.isCancelled)
        assertTrue(newJob.isActive)
        assertTrue(originalJob !== newJob)
    }

    @Test
    fun `reset after dispose throws`() {
        val provider = ProjectCoroutineScopeProvider(testDispatchers)
        provider.dispose()

        var thrown = false
        try {
            provider.reset(testDispatchers)
        } catch (e: IllegalStateException) {
            thrown = true
        }

        assertTrue(thrown)
    }
}
