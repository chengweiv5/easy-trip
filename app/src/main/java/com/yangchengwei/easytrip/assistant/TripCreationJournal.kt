package com.yangchengwei.easytrip.assistant

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.io.FileNotFoundException

/** One unresolved operation, in private non-backed-up storage; never contains credentials or images. */
interface PendingTripCreation {
    fun read(): TripCreationCommand?
    fun write(command: TripCreationCommand)
    fun clear(command: TripCreationCommand)
}

class TripCreationJournal(context: Context) : PendingTripCreation {
    private val file = AtomicFile(File(context.noBackupFilesDir, "trip-creation-pending.json"))
    @Synchronized override fun read(): TripCreationCommand? {
        val bytes = try { file.openRead().use { stream ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (output.size() <= 256 * 1024) {
                val count = stream.read(buffer)
                if (count < 0) break
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        } }
        catch (_: FileNotFoundException) { return null }
        require(bytes.size <= 256 * 1024) { "创建恢复记录无效，请保留记录并重试" }
        return TripCreationCommand.decode(bytes.toString(Charsets.UTF_8))
    }
    @Synchronized override fun write(command: TripCreationCommand) {
        command.validated()
        val old = read()
        require(old == null || old == command) { "请先处理上次未完成的创建" }
        val bytes = command.encode().toByteArray(Charsets.UTF_8)
        require(bytes.size <= 256 * 1024)
        val output = file.startWrite()
        try { output.write(bytes); file.finishWrite(output) }
        catch (e: Exception) { file.failWrite(output); throw e }
    }
    @Synchronized override fun clear(command: TripCreationCommand) {
        require(read() == command) { "创建恢复记录已变化" }
        file.delete()
        check(read() == null) { "创建恢复记录尚未清理" }
    }
}
