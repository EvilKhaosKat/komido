package dev.romangaranin.komido

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ServerState(val sourceDirPath: String) {
    fun packToZip(statesDirPath: String) : String {
        val statesPath = Paths.get(statesDirPath)
        if (Files.notExists(statesPath)) {
            Files.createDirectory(statesPath)
        }

        val zipFilePath = getZipFilePath(statesDirPath)
        println("zip file path = ${zipFilePath}")
        File(zipFilePath).let { if (it.exists()) it.delete() }

        val zipFile = Files.createFile(Paths.get(zipFilePath))

        ZipOutputStream(Files.newOutputStream(zipFile)).use { stream ->
            val sourceDir = Paths.get(sourceDirPath)
            Files.walk(sourceDir).filter { path -> !Files.isDirectory(path) }
                    .forEach { path ->
                        val substring = path.toString().substringAfterLast("$sourceDir")
                        val zipEntry = ZipEntry(substring)

                        stream.putNextEntry(zipEntry)
                        stream.write(Files.readAllBytes(path))
                        stream.closeEntry()
                    }
        }

        return zipFilePath
    }

    companion object {
        protected fun getZipFilePath(statesDirPath: String): String {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd|HH-mm-ss")
            return "$statesDirPath/${LocalDateTime.now().format(formatter)}.zip"
        }
    }
}
