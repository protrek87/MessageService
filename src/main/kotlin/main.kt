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
    fun getMessages(): Sequence<Message> = messages.asSequence()

    fun addMessage(message: Message) {
        messages.add(message)
    }

    fun removeMessage(messageId: String): Boolean = messages.removeIf { it.id == messageId }

    fun updateMessage(messageId: String, newText: String): Boolean =
        messages.asSequence()
            .firstOrNull { it.id == messageId }
            ?.let {
                it.text = newText
                true
            } ?: false

    fun getUnreadCount(): Int = messages.asSequence().count { !it.isRead }

    fun markAllAsRead() {
        messages.asSequence().forEach { it.isRead = true }
    }

    fun getLastMessageText(): String = messages.asSequence().lastOrNull()?.text ?: "нет сообщений"

    fun isEmpty(): Boolean = messages.asSequence().none()
}

class MessagingService {
    private val users = mutableMapOf<String, User>()
    private val chats = mutableMapOf<String, Chat>()

    fun addUser(user: User) {
        users[user.id] = user
    }

    fun getUser(userId: String): User? = users[userId]

    fun getUnreadChatsCount(): Int = chats.values.asSequence().count { it.getUnreadCount() > 0 }

    fun getChats(): Sequence<Chat> = chats.values.asSequence()

    fun getLastMessages(): Sequence<String> = chats.values.asSequence().map { it.getLastMessageText() }

    fun getMessagesFromChat(partnerId: String, count: Int): List<Message> {
        val chat = chats[partnerId] ?: throw NoSuchElementException("Чат с $partnerId не найден")
        val messages = chat.getMessages().toList().let {
            if (count >= it.size) it else it.subList(it.size - count, it.size)
        }
        chat.markAllAsRead()
        return messages
    }

    fun sendMessage(senderId: String, recipientId: String, text: String): Message {
        if (users[senderId] == null || users[recipientId] == null) {
            throw IllegalArgumentException("Пользователь не найден")
        }

        val message = Message(senderId = senderId, text = text, isRead = false)

        val chat = chats.getOrPut(recipientId) { Chat(partnerId = recipientId) }
        chat.addMessage(message)

        return message
    }

    fun deleteMessage(partnerId: String, messageId: String): Boolean =
        chats[partnerId]?.removeMessage(messageId) ?: false

    fun createChatIfNotExists(partnerId: String) {
        chats.getOrPut(partnerId) { Chat(partnerId) }
    }

    fun deleteChat(partnerId: String): Boolean = chats.remove(partnerId) != null

    fun editMessage(partnerId: String, messageId: String, newText: String): Boolean =
        chats[partnerId]?.updateMessage(messageId, newText) ?: false
}

// Extension functions
fun Sequence<Chat>.filterUnread(): Sequence<Chat> = filter { it.getUnreadCount() > 0 }

fun Sequence<Chat>.mapToLastTexts(): Sequence<String> = map { it.getLastMessageText() }

fun Sequence<Message>.markAllAsRead(): Sequence<Message> = onEach { it.isRead = true }