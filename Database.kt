import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

class Database {

    val USER_NAME: String = "your_postgres_user"
    val PASSWORD: String = "your_postgres_password"
    val URL: String = "jdbc:postgresql://localhost:5432/your_database_name"

    val connection: Connection by lazy {
        try {
            DriverManager.getConnection(URL, USER_NAME, PASSWORD).apply {
                println("Connection to PostgreSQL has been established.")
            }
        } catch (e: SQLException) {
            println("Error!")
            println(e.message)
            throw e
        }
    }

    fun createTables() {
        try {
            val userTable = """
                CREATE TABLE IF NOT EXISTS users (
                    id SERIAL PRIMARY KEY,
                    username VARCHAR(50) UNIQUE NOT NULL,
                    password VARCHAR(50) NOT NULL,
                    balance DOUBLE PRECISION DEFAULT 0
                );
            """
            val transactionTable = """
                CREATE TABLE IF NOT EXISTS transactions (
                    id SERIAL PRIMARY KEY,
                    sender_id INT NOT NULL,
                    recipient_id INT NOT NULL,
                    amount DOUBLE PRECISION NOT NULL,
                    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (sender_id) REFERENCES users(id),
                    FOREIGN KEY (recipient_id) REFERENCES users(id)
                );
            """
            connection.prepareStatement(userTable).executeUpdate()
            connection.prepareStatement(transactionTable).executeUpdate()
            println("Tables have been created or already exist.")
        } catch (e: SQLException) {
            println("Error creating tables.")
            println(e.message)
        }
    }

    fun registerUser(username: String, password: String): Boolean {
        return try {
            val sql = "INSERT INTO users (username, password) VALUES (?, ?);"
            val preparedStatement = connection.prepareStatement(sql)
            preparedStatement.setString(1, username)
            preparedStatement.setString(2, password)
            preparedStatement.executeUpdate()
            true
        } catch (e: SQLException) {
            println("Error registering user: ${e.message}")
            false
        }
    }

    fun authenticateUser(username: String, password: String): Boolean {
        return try {
            val sql = "SELECT * FROM users WHERE username = ? AND password = ?;"
            val preparedStatement = connection.prepareStatement(sql)
            preparedStatement.setString(1, username)
            preparedStatement.setString(2, password)
            val resultSet = preparedStatement.executeQuery()
            resultSet.next()
        } catch (e: SQLException) {
            println("Error authenticating user: ${e.message}")
            false
        }
    }

    fun getUserBalance(username: String): Double {
        return try {
            val sql = "SELECT balance FROM users WHERE username = ?;"
            val preparedStatement = connection.prepareStatement(sql)
            preparedStatement.setString(1, username)
            val resultSet = preparedStatement.executeQuery()
            if (resultSet.next()) resultSet.getDouble("balance") else 0.0
        } catch (e: SQLException) {
            println("Error fetching user balance: ${e.message}")
            0.0
        }
    }

    fun transferFunds(sender: String, recipient: String, amount: Double): Boolean {
        return try {
            connection.autoCommit = false

            val getSenderBalance = "SELECT balance FROM users WHERE username = ?;"
            val getRecipientBalance = "SELECT balance FROM users WHERE username = ?;"
            val updateSenderBalance = "UPDATE users SET balance = balance - ? WHERE username = ?;"
            val updateRecipientBalance = "UPDATE users SET balance = balance + ? WHERE username = ?;"
            val insertTransaction = "INSERT INTO transactions (sender_id, recipient_id, amount) VALUES ((SELECT id FROM users WHERE username = ?), (SELECT id FROM users WHERE username = ?), ?);"

            val senderBalanceStatement = connection.prepareStatement(getSenderBalance)
            senderBalanceStatement.setString(1, sender)
            val senderBalanceResult = senderBalanceStatement.executeQuery()
            if (!senderBalanceResult.next() || senderBalanceResult.getDouble("balance") < amount) {
                connection.rollback()
                return false
            }

            val recipientBalanceStatement = connection.prepareStatement(getRecipientBalance)
            recipientBalanceStatement.setString(1, recipient)
            val recipientBalanceResult = recipientBalanceStatement.executeQuery()
            if (!recipientBalanceResult.next()) {
                connection.rollback()
                return false
            }

            val updateSenderStatement = connection.prepareStatement(updateSenderBalance)
            updateSenderStatement.setDouble(1, amount)
            updateSenderStatement.setString(2, sender)
            updateSenderStatement.executeUpdate()

            val updateRecipientStatement = connection.prepareStatement(updateRecipientBalance)
            updateRecipientStatement.setDouble(1, amount)
            updateRecipientStatement.setString(2, recipient)
            updateRecipientStatement.executeUpdate()

            val insertTransactionStatement = connection.prepareStatement(insertTransaction)
            insertTransactionStatement.setString(1, sender)
            insertTransactionStatement.setString(2, recipient)
            insertTransactionStatement.setDouble(3, amount)
            insertTransactionStatement.executeUpdate()

            connection.commit()
            true
        } catch (e: SQLException) {
            println("Error transferring funds: ${e.message}")
            connection.rollback()
            false
        } finally {
            connection.autoCommit = true
        }
    }

    fun getTransactionHistory(username: String): List<String> {
        return try {
            val sql = """
                SELECT t.amount, u.username AS recipient, t.timestamp
                FROM transactions t
                JOIN users u ON t.recipient_id = u.id
                WHERE t.sender_id = (SELECT id FROM users WHERE username = ?)
                ORDER BY t.timestamp DESC;
            """
            val preparedStatement = connection.prepareStatement(sql)
            preparedStatement.setString(1, username)
            val resultSet = preparedStatement.executeQuery()
            val transactions = mutableListOf<String>()
            while (resultSet.next()) {
                val amount = resultSet.getDouble("amount")
                val recipient = resultSet.getString("recipient")
                val timestamp = resultSet.getTimestamp("timestamp")
                transactions.add("Sent $$amount to $recipient on $timestamp")
            }
            transactions
        } catch (e: SQLException) {
            println("Error fetching transaction history: ${e.message}")
            emptyList()
        }
    }
}
