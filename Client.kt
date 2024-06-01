import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.singleWindowApplication
import java.io.BufferedReader
import java.io.PrintWriter
import java.net.Socket

fun main() = singleWindowApplication {
    var showLoginDialog by remember { mutableStateOf(true) }
    var showRegisterDialog by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var balance by remember { mutableStateOf(0.0) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var transferAmount by remember { mutableStateOf("") }
    var transferRecipient by remember { mutableStateOf("") }
    var transactions by remember { mutableStateOf(listOf<String>()) }

    if (showLoginDialog) {
        LoginDialog(
            username = username,
            password = password,
            onUsernameChange = { username = it },
            onPasswordChange = { password = it },
            onLogin = {
                // Implement your authentication logic here and update balance
                Socket("localhost", 9999).use { socket ->
                    val writer = PrintWriter(socket.getOutputStream(), true)
                    val reader = BufferedReader(socket.getInputStream().reader())
                    writer.println("LOGIN")
                    writer.println(username)
                    writer.println(password)
                    val response = reader.readLine()
                    if (response == "SUCCESS") {
                        showLoginDialog = false
                        balance = reader.readLine().toDouble()
                        val transactionCount = reader.readLine().toInt()
                        transactions = (1..transactionCount).map { reader.readLine() }
                    } else {
                        println("Login failed")
                    }
                }
            },
            onShowRegister = {
                showLoginDialog = false
                showRegisterDialog = true
            }
        )
    } else if (showRegisterDialog) {
        RegisterDialog(
            username = username,
            password = password,
            onUsernameChange = { username = it },
            onPasswordChange = { password = it },
            onRegister = {
                // Implement your registration logic here
                Socket("localhost", 9999).use { socket ->
                    val writer = PrintWriter(socket.getOutputStream(), true)
                    val reader = BufferedReader(socket.getInputStream().reader())
                    writer.println("REGISTER")
                    writer.println(username)
                    writer.println(password)
                    val response = reader.readLine()
                    if (response == "SUCCESS") {
                        showRegisterDialog = false
                        showLoginDialog = true
                    } else {
                        println("Registration failed")
                    }
                }
            },
            onDismiss = {
                showRegisterDialog = false
                showLoginDialog = true
            }
        )
    } else {
        Scaffold(
            topBar = { TopAppBar(title = { Text("Banking App") }) },
            content = {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Welcome, $username")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Current Balance: $$balance")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { showTransferDialog = true }) {
                        Text("Transfer Funds")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Transaction History")
                    transactions.forEach { transaction ->
                        Text(transaction)
                    }
                }
            }
        )
    }

    if (showTransferDialog) {
        TransferDialog(
            amount = transferAmount,
            recipient = transferRecipient,
            onAmountChange = { transferAmount = it },
            onRecipientChange = { transferRecipient = it },
            onTransfer = {
                // Implement your fund transfer logic here
                Socket("localhost", 9999).use { socket ->
                    val writer = PrintWriter(socket.getOutputStream(), true)
                    val reader = BufferedReader(socket.getInputStream().reader())
                    writer.println("TRANSFER")
                    writer.println(username)
                    writer.println(transferRecipient)
                    writer.println(transferAmount)
                    val response = reader.readLine()
                    if (response == "SUCCESS") {
                        balance = reader.readLine().toDouble()
                        val transactionCount = reader.readLine().toInt()
                        transactions = (1..transactionCount).map { reader.readLine() }
                        showTransferDialog = false
                    } else {
                        println("Transfer failed")
                    }
                }
            },
            onDismiss = { showTransferDialog = false }
        )
    }
}

@Composable
fun LoginDialog(
    username: String,
    password: String,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLogin: () -> Unit,
    onShowRegister: () -> Unit
) {
    Dialog(onCloseRequest = {}) {
        Surface(shape = MaterialTheme.shapes.medium, elevation = 8.dp) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Login", style = MaterialTheme.typography.h6)
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    value = username,
                    onValueChange = onUsernameChange,
                    label = { Text("Username") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row {
                    Button(onClick = onLogin) {
                        Text("Login")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onShowRegister) {
                        Text("Register")
                    }
                }
            }
        }
    }
}

@Composable
fun RegisterDialog(
    username: String,
    password: String,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onRegister: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onCloseRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.medium, elevation = 8.dp) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Register", style = MaterialTheme.typography.h6)
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    value = username,
                    onValueChange = onUsernameChange,
                    label = { Text("Username") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row {
                    Button(onClick = onRegister) {
                        Text("Register")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
fun TransferDialog(
    amount: String,
    recipient: String,
    onAmountChange: (String) -> Unit,
    onRecipientChange: (String) -> Unit,
    onTransfer: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onCloseRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.medium, elevation = 8.dp) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Transfer Funds", style = MaterialTheme.typography.h6)
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    value = amount,
                    onValueChange = onAmountChange,
                    label = { Text("Amount") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = recipient,
                    onValueChange = onRecipientChange,
                    label = { Text("Recipient") }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row {
                    Button(onClick = onTransfer) {
                        Text("Transfer")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}
