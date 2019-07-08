package dev.romangaranin.komido

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt

class KomidoCommand : CliktCommand(name = "komido") {
    override fun run() = Unit
}

class Init : CliktCommand(help = "Init application by providing SSH connection string (alias or username@hostname)") {
    val sshConnectionString: String by option(help = "SSH connection string").prompt("SSH connection string")

    override fun run() {
        val komido = Komido(sshConnectionString)
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

fun main(args: Array<String>) {
    KomidoCommand()
            .subcommands(Init(), PrepareServer())
            .main(args)
}