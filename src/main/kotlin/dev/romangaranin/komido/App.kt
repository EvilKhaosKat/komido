package dev.romangaranin.komido

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt

fun main(args: Array<String>) {
    KomidoCommand()
            .subcommands(Init(), UpdateSshConnection(), PrepareServer(), UploadState(), MakeBackup())
            .main(args)
}

class KomidoCommand : CliktCommand(name = "komido") {
    override fun run() = Unit
}

class Init : CliktCommand(help = "Init application by providing SSH connection string (alias or username@hostname) " +
        "and optionally folder for backups, and latest actual backup") {
    private val sshConnectionString: String
            by option(help = "SSH connection string").prompt("SSH connection string")
    private val backupDirPath: String?
            by option(help = "Directory for keeping server backups")
    private val latestBackupPath: String?
            by option(help = "Path to latest backup zip file")

    override fun run() {
        val komido = Komido(sshConnectionString)

        if (backupDirPath != null) {
            komido.backupDirPath = backupDirPath as String
        }
        if (latestBackupPath != null) {
            komido.latestBackupPath = latestBackupPath as String
        }

        komido.saveAppConfig()
        echo("Komido state saved")
    }
}

class UpdateSshConnection : CliktCommand(help = "Updates SSH connection string (alias or username@hostname)",
        name = "updateSshConnection") {
    private val sshConnectionString: String
            by option(help = "SSH connection string").prompt("SSH connection string")

    override fun run() {
        val komido = Komido.loadState()
        komido.sshConnectionString = sshConnectionString

        komido.saveAppConfig()
        echo("Komido state saved")
    }
}

class PrepareServer : CliktCommand(help = "Prepare server by installing necessary packages", name = "prepareServer") {
    override fun run() {
        val komido = Komido.loadState()

        echo("Prepare server ${komido.sshConnectionString}")
        komido.prepareServer()
        echo("Done")
    }
}

class UploadState : CliktCommand(help = "Upload latest backup/state to server", name = "uploadState") {
    override fun run() {
        val komido = Komido.loadState()

        echo("Upload minecraft server to ${komido.sshConnectionString}")
        komido.uploadState()
        echo("Done")
    }
}

class MakeBackup : CliktCommand(help = "Make backup of current server state", name = "backup") {
    override fun run() {
        val komido = Komido.loadState()

        echo("Making backup of server ${komido.sshConnectionString}")
        komido.makeBackup()
        echo("Done")
    }
}
