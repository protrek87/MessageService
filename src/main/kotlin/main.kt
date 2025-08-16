import java.util.*

data class User(
    val id: String,
    val name: String
)

data class Message(
    val id: String = UUID.randomUUID().toString(),
    val senderId: String,
    var text: String,
    var isRead: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class Chat(
    val partnerId: String,
    private val messages: MutableList<Message> = mutableListOf()
) {
    fun getMessages(): List<Message> = messages.toList()

    fun addMessage(message: Message) {
        messages.add(message)
    }

    fun removeMessage(messageId: String): Boolean {
        return messages.removeIf { it.id == messageId }
    }

    fun updateMessage(messageId: String, newText: String): Boolean {
        val message = messages.find { it.id == messageId } ?: return false
        message.text = newText
        return true
    }

    fun getUnreadCount(): Int = messages.count { !it.isRead }

    fun markAllAsRead() {
        messages.forEach { it.isRead = true }
    }

    fun getLastMessageText(): String = messages.lastOrNull()?.text ?: "нет сообщений"

    fun isEmpty(): Boolean = messages.isEmpty()
}

class MessagingService {
    private val users = mutableMapOf<String, User>()
    private val chats = mutableMapOf<String, Chat>() // ключ — partnerId

    fun addUser(user: User) {
        users[user.id] = user
    }

    fun getUser(userId: String): User? = users[userId]

    // 1. Количество чатов с непрочитанными сообщениями
    fun getUnreadChatsCount(): Int = chats.values.count { it.getUnreadCount() > 0 }

    // 2. Получить список чатов
    fun getChats(): List<Chat> = chats.values.toList()

    // 3. Получить последние сообщения из чатов (в виде строк)
    fun getLastMessages(): List<String> = chats.values.map { it.getLastMessageText() }

    // 4. Получить последние N сообщений из чата с partnerId
    // После получения — все сообщения помечаются как прочитанные
    fun getMessagesFromChat(partnerId: String, count: Int): List<Message> {
        val chat = chats[partnerId] ?: throw NoSuchElementException("Чат с $partnerId не найден")
        val messages = chat.getMessages().takeLast(count)
        chat.markAllAsRead()
        return messages
    }

    // 5. Создать новое сообщение
    // Чат создаётся при первом сообщении
    fun sendMessage(senderId: String, recipientId: String, text: String): Message {
        if (users[senderId] == null || users[recipientId] == null) {
            throw IllegalArgumentException("Пользователь не найден")
        }

        val message = Message(senderId = senderId, text = text, isRead = false)

        val chat = chats.getOrPut(recipientId) { Chat(partnerId = recipientId) }
        chat.addMessage(message)

        return message
    }

    // 6. Удалить сообщение (любое, можно чужое)
    fun deleteMessage(partnerId: String, messageId: String): Boolean {
        val chat = chats[partnerId] ?: return false
        return chat.removeMessage(messageId)
    }

    // 7. Создать чат — происходит автоматически при sendMessage
    // Эта функция просто проверяет или инициирует чат
    fun createChatIfNotExists(partnerId: String) {
        chats.getOrPut(partnerId) { Chat(partnerId) }
    }

    // 8. Удалить чат целиком
    fun deleteChat(partnerId: String): Boolean {
        return chats.remove(partnerId) != null
    }

    // Дополнительно: редактировать сообщение
    fun editMessage(partnerId: String, messageId: String, newText: String): Boolean {
        val chat = chats[partnerId] ?: return false
        return chat.updateMessage(messageId, newText)
    }
}

fun List<Chat>.filterUnread(): List<Chat> = this.filter { it.getUnreadCount() > 0 }

fun List<Chat>.mapToLastTexts(): List<String> = this.map { it.getLastMessageText() }

fun List<Message>.markAllAsRead(): List<Message> {
    this.forEach { it.isRead = true }
    return this
}