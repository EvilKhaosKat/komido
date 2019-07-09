package dev.romangaranin.komido

import com.google.gson.Gson
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

private const val COMIDO_STATE_FILENAME = "./komido.json"

class Komido(var sshConnectionString: String,
             var statesDirPath: String = "./states",
             var minecraftFolder: String = "/home/minecraft") {
    fun prepareServer() {
        val result = "ssh $sshConnectionString apt -y update && apt -y install openjdk-11-jre".runCommand()
        println(result)
    }

    fun makeBackup() {
        val backupPath = recreateBackupDir(statesDirPath)
        println("backupPath = ${backupPath}")

        val command = "scp -r $sshConnectionString:$minecraftFolder $backupPath"
        println("command = ${command}")

        println("Copying files")
        val result = command.runCommand()
        println(result)

        val state = ServerState(backupPath.toString())
        state.packToZip(statesDirPath)

        println("Deleting backup dir")
        deleteBackupDir(backupPath)
    }

    fun saveState(stateFilePath: String = COMIDO_STATE_FILENAME) {
        var path = Paths.get(stateFilePath)
        Files.deleteIfExists(path)
        path = Files.createFile(path)

        val stateJson = Gson().toJson(this)
        File(path.toUri()).writeText(stateJson)
    }

    override fun toString(): String {
        return "Komido(sshConnectionString='$sshConnectionString')"
    }

    companion object {
        fun loadState(stateFilePath: String = COMIDO_STATE_FILENAME): Komido {
            if (!stateExists(stateFilePath)) {
                throw RuntimeException("State file $stateFilePath does not exist")
            }

            val stateJson = File(stateFilePath).readText()

            val result = Gson().fromJson(stateJson, Komido::class.java)

            return result ?: throw RuntimeException("state wasn't parsed")
        }

        fun stateExists(stateFilePath: String = COMIDO_STATE_FILENAME): Boolean {
            return Files.exists(Paths.get(stateFilePath))
        }

        private fun getBackupPath(statesDirPath: String): String {
            return "$statesDirPath/backup"
        }

        private fun recreateBackupDir(statesDirPath: String): Path {
            var backupPath = Paths.get(getBackupPath(statesDirPath))
            deleteBackupDir(backupPath)

            backupPath = Files.createDirectory(backupPath)
            return backupPath
        }

        private fun deleteBackupDir(backupPath: Path): Path? {
            if (Files.exists(backupPath)) {
                File(backupPath.toUri()).deleteRecursively()
            }
            return backupPath
        }
    }
}

fun String.runCommand(workingDir: File = File("."),
                      timeoutAmount: Long = 60,
                      timeoutUnit: TimeUnit = TimeUnit.SECONDS): String? {
    return try {
        ProcessBuilder(*this.split("\\s".toRegex()).toTypedArray())
                .directory(workingDir)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .redirectErrorStream(true)
                .start().apply {
                    waitFor(timeoutAmount, timeoutUnit)
                }.inputStream.bufferedReader().readText()
    } catch (e: IOException) {
        e.printStackTrace()
        return e.toString()
    }
}