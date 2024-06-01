import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import kotlin.concurrent.thread

fun main() {
    val server = ServerSocket(9999)
    val database = Database()
    database.createTables()

    println("Server is running on port 9999")

    while (true) {
        val client = server.accept()
        thread {
            val reader = BufferedReader(InputStreamReader(client.getInputStream()))
            val writer = PrintWriter(client.getOutputStream(), true)

            val command = reader.readLine()
            when (command) {
                "REGISTER" -> {
                    val username = reader.readLine()
                    val password = reader.readLine()
                    val success = database.registerUser(username, password)
                    writer.println(if (success) "SUCCESS" else "FAILURE")
                }
                "LOGIN" -> {
                    val username = reader.readLine()
                    val password = reader.readLine()
                    val authenticated = database.authenticateUser(username, password)
                    if (authenticated) {
                        writer.println("SUCCESS")
                        writer.println(database.getUserBalance(username))
                        val transactions = database.getTransactionHistory(username)
                        writer.println(transactions.size)
                        transactions.forEach { writer.println(it) }
                    } else {
                        writer.println("FAILURE")
                    }
                }
                "TRANSFER" -> {
                    val sender = reader.readLine()
                    val recipient = reader.readLine()
                    val amount = reader.readLine().toDouble()
                    val success = database.transferFunds(sender, recipient, amount)
                    if (success) {
                        writer.println("SUCCESS")
                        writer.println(database.getUserBalance(sender))
                        val transactions = database.getTransactionHistory(sender)
                        writer.println(transactions.size)
                        transactions.forEach { writer.println(it) }
                    } else {
                        writer.println("FAILURE")
                    }
                }
                else -> writer.println("UNKNOWN_COMMAND")
            }
        }
    }
}
