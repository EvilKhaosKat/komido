package dev.romangaranin.komido

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt

class KomidoCommand : CliktCommand(name = "komido") {
    override fun run() = Unit
}

class Init : CliktCommand(help = "Init application by providing SSH connection string (alias or username@hostname) " +
        "and folder for world states") {
    val sshConnectionString: String
            by option(help = "SSH connection string").prompt("SSH connection string")
    val statesDirPath: String
            by option(help = "Directory for keeping world states").default("./states")

    override fun run() {
        val komido = Komido(sshConnectionString, statesDirPath)
        komido.saveState()
        echo("Komido state saved")
    }
}

class PrepareServer : CliktCommand(help = "Prepare server by installing necessary packages", name = "prepareServer") {
    override fun run() {
        val komido = Komido.loadState()
        echo("Komido state loaded")

        echo("Prepare server ${komido.sshConnectionString}")
        komido.prepareServer()
        echo("Done")
    }
}

class MakeBackup : CliktCommand(help = "Make backup of current server", name = "backup") {
    override fun run() {
        val komido = Komido.loadState()
        echo("Komido state loaded")

        echo("Making backup of server ${komido.sshConnectionString}")
        komido.makeBackup()
        echo("Done")
    }
}

fun main(args: Array<String>) {
    KomidoCommand()
            .subcommands(Init(), PrepareServer(), MakeBackup())
            .main(args)
}