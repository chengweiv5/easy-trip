package com.yangchengwei.easytrip.settings

import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateControllerTest {
    private val release = OfficialRelease("v1.8.0", "notes", false, false, ReleaseAsset("easy-trip-v1.8.0-release.apk", "https://github.com/chengweiv5/easy-trip/releases/download/v1.8.0/easy-trip-v1.8.0-release.apk", 100, "a".repeat(64)))
    private class Fake(val release: OfficialRelease): UpdateService {
        var checks=0; var downloads=0; var discarded=0; var fail=false
        var gate: CompletableDeferred<Unit>?=null
        override suspend fun latest(): OfficialRelease { checks++;gate?.await();if(fail) error("offline");return release }
        override suspend fun download(release: OfficialRelease, progress:(Long,Long)->Unit):File { downloads++;progress(50,100);gate?.await();if(fail) error("offline");return File("fake.apk") }
        override fun discard(file:File) { discarded++ }
    }
    @Test fun requiresExplicitActionsAndReadyCanBeDiscarded()=runTest {
        val source=Fake(release);val c=UpdateController("1.7.0",source,this)
        runCurrent();assertEquals(0,source.checks);assertEquals(UpdateState.Idle,c.state.value)
        c.download();assertEquals(0,source.downloads)
        c.check();runCurrent();assertTrue(c.state.value is UpdateState.Available)
        c.download();runCurrent();assertTrue(c.state.value is UpdateState.Ready)
        c.close();assertEquals(1,source.discarded)
    }
    @Test fun checkCancellationAndDuplicateCheckAreSafe()=runTest {
        val source=Fake(release).apply { gate=CompletableDeferred() };val c=UpdateController("1.7.0",source,this)
        c.check();c.check();runCurrent();assertEquals(1,source.checks)
        c.cancel();source.gate!!.complete(Unit);runCurrent();assertEquals(UpdateState.Idle,c.state.value)
        c.check();runCurrent();assertTrue(c.state.value is UpdateState.Available)
    }
    @Test fun failuresRemainFailuresAndCanRetry()=runTest {
        val source=Fake(release).apply { fail=true };val c=UpdateController("1.7.0",source,this)
        c.check();runCurrent();assertTrue(c.state.value is UpdateState.Failed)
        source.fail=false;c.check();runCurrent();source.fail=true;c.download();runCurrent()
        assertEquals(release,(c.state.value as UpdateState.Failed).release)
        source.fail=false;c.download();runCurrent();assertTrue(c.state.value is UpdateState.Ready)
    }
    @Test fun cancellingDownloadRestoresAvailableAndNoLateReady()=runTest {
        val source=Fake(release);val c=UpdateController("1.7.0",source,this)
        c.check();runCurrent();source.gate=CompletableDeferred();c.download();runCurrent()
        assertEquals(50L,(c.state.value as UpdateState.Downloading).bytes)
        c.cancel();source.gate!!.complete(Unit);runCurrent();assertTrue(c.state.value is UpdateState.Available)
    }
    @Test fun lateNonCooperativeCheckCannotOverwriteCancellation()=runTest {
        val gate=CompletableDeferred<Unit>()
        val source=object:UpdateService {
            override suspend fun latest():OfficialRelease = kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) { gate.await();release }
            override suspend fun download(release:OfficialRelease,progress:(Long,Long)->Unit)=File("unused")
            override fun discard(file:File)=Unit
        }
        val c=UpdateController("1.7.0",source,this)
        c.check();runCurrent();c.cancel();gate.complete(Unit);runCurrent()
        assertEquals(UpdateState.Idle,c.state.value)
    }
    @Test fun lateNonCooperativeDownloadIsDiscardedAfterCancel()=runTest {
        val gate=CompletableDeferred<Unit>();var discards=0
        val source=object:UpdateService {
            override suspend fun latest()=release
            override suspend fun download(release:OfficialRelease,progress:(Long,Long)->Unit):File =
                kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) { gate.await();File("late.apk") }
            override fun discard(file:File) { discards++ }
        }
        val c=UpdateController("1.7.0",source,this)
        c.check();runCurrent();c.download();runCurrent();c.cancel();gate.complete(Unit);runCurrent()
        assertTrue(c.state.value is UpdateState.Available)
        assertEquals(1,discards)
    }
    @Test fun newerInstalledVersionDoesNotOfferDownload()=runTest {
        val source=Fake(release);val c=UpdateController("1.9.0",source,this)
        c.check();runCurrent();assertEquals(UpdateState.Ahead,c.state.value);c.download();assertEquals(0,source.downloads)
    }
}
