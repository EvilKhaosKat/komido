package dev.romangaranin.komido

import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class Komido(private val sshConnectString: String) {
    fun prepareServer() {
        val result = "ssh $sshConnectString apt -y update && apt -y install openjdk-11-jre".runCommand()
        println(result)
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