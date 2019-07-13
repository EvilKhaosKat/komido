package dev.romangaranin.komido

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit


private const val KOMIDO_STATE_FILENAME = "./komido.json"

//TODO split class - separate commands to separate handlers
class Komido(var sshConnectionString: String,
             var statesDirPath: String = "./states",
             var minecraftServerFolder: String = "/home/minecraft",
             var latestBackupPath: String = "") {
    /**
     * Install required packages on server.
     */
    fun prepareServer() {
        val result = ("ssh -o BatchMode=yes -o StrictHostKeyChecking=no $sshConnectionString " +
                "apt -y update && apt -y install openjdk-11-jre").runCommand()
        println(result)
    }

    /**
     * Upload most recent minecraft state backup to server.
     */
    fun uploadState() {
        if (latestBackupPath == "") {
            println("Error: can't upload server since latestBackupPath is unknown. " +
                    "Please provide explicitly via init phase or make backup first.")
            return
        }

        //TODO unify minecraftServerFolder with the fact that archive contains 'minecraft' folder in it
        val targetFolder = minecraftServerFolder.substringBeforeLast("/")
        val targetArchiveName = "minecraft.zip"

        val uploadResult = uploadArchive(targetFolder, targetArchiveName)
        if (uploadResult != "") println("upload result = $uploadResult")

        println()

        val unzipResult = unzipArchive(targetFolder, targetArchiveName)
        if (unzipResult != "") println("unzip result = $unzipResult")

        println("Now you can connect using 'ssh $sshConnectionString', cd to '$minecraftServerFolder'")
    }

    private fun unzipArchive(targetFolder: String, targetArchiveName: String): String? {
        val unzipCommand = "ssh $sshConnectionString unzip /home/$targetArchiveName -d $targetFolder"
        println("unzip command = $unzipCommand")

        println("Unzipping server backup/state")
        return unzipCommand.runCommand()
    }

    private fun uploadArchive(targetFolder: String, targetArchiveName: String): String? {
        val uploadCommand = "scp -r $latestBackupPath $sshConnectionString:$targetFolder/$targetArchiveName"
        println("upload command = $uploadCommand")

        println("Uploading latest server backup/state")
        return uploadCommand.runCommand()
    }

    /**
     * Create backup of current server/minecraft state.
     */
    fun makeBackup() {
        val backupPath = recreateBackupDir(statesDirPath)
        println("backupPath = $backupPath")

        val command = "scp -r $sshConnectionString:$minecraftServerFolder $backupPath"
        println("command = $command")

        println("Copying files")
        val result = command.runCommand()
        println(result)

        val state = ServerState(backupPath.toString())
        latestBackupPath = state.packToZip(statesDirPath)
        saveAppConfig()

        deleteBackupDir(backupPath)
    }

    /**
     * Save current app config parameters.
     */
    fun saveAppConfig(stateFilePath: String = KOMIDO_STATE_FILENAME) {
        var path = Paths.get(stateFilePath)
        Files.deleteIfExists(path)
        path = Files.createFile(path)

        val gson = GsonBuilder().setPrettyPrinting().create()
        val stateJson = gson.toJson(this)
        File(path.toUri()).writeText(stateJson)
    }

    override fun toString(): String {
        return "Komido(sshConnectionString='$sshConnectionString', " +
                "statesDirPath='$statesDirPath', " +
                "minecraftServerFolder='$minecraftServerFolder', " +
                "latestBackupPath='$latestBackupPath')"
    }

    companion object {
        fun loadState(stateFilePath: String = KOMIDO_STATE_FILENAME): Komido {
            if (!stateExists(stateFilePath)) {
                throw RuntimeException("State file $stateFilePath does not exist")
            }

            val stateJson = File(stateFilePath).readText()

            val result = Gson().fromJson(stateJson, Komido::class.java)

            return result ?: throw RuntimeException("state wasn't parsed")
        }

        private fun stateExists(stateFilePath: String = KOMIDO_STATE_FILENAME): Boolean {
            return Files.exists(Paths.get(stateFilePath))
        }

        private fun getBackupPath(statesDirPath: String): String {
            return "$statesDirPath/backup"
        }

        private fun recreateBackupDir(statesDirPath: String): Path {
            var backupPath = Paths.get(getBackupPath(statesDirPath))

            backupPath.toFile().mkdirs()
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