package dev.romangaranin.komido

import com.google.gson.Gson
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

data class Komido(val sshConnectString: String) {
    fun prepareServer() {
        val result = "ssh $sshConnectString apt -y update && apt -y install openjdk-11-jre".runCommand()
        println(result)
    }

    fun saveState(stateFilePath: String = "./komido.json") {
        var path = Paths.get(stateFilePath)
        Files.deleteIfExists(path)
        path = Files.createFile(path)

        val stateJson = Gson().toJson(this)
        File(path.toUri()).writeText(stateJson)
    }

    companion object {
        fun loadState(stateFilePath: String = "./komido.json"): Komido {
            if (Files.notExists(Paths.get(stateFilePath))) {
                throw RuntimeException("State file $stateFilePath not exists")
            }

            val stateJson = File(stateFilePath).readText()

            val result = Gson().fromJson(stateJson, Komido::class.java)

            return result ?: throw RuntimeException("state wasn't parsed")
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