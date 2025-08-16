import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.*

class MessagingServiceTest {

    private lateinit var service: MessagingService
    private lateinit var user1: User
    private lateinit var user2: User

    @Before
    fun setUp() {
        service = MessagingService()
        user1 = User("u1", "Alice")
        user2 = User("u2", "Bob")
        service.addUser(user1)
        service.addUser(user2)
    }

    @Test
    fun `should create chat when sending first message`() {
        service.sendMessage(user1.id, user2.id, "Привет!")

        val chats = service.getChats()
        assertEquals(1, chats.size)
        assertEquals(user2.id, chats[0].partnerId)
    }

    @Test
    fun `should increment unread chats count when receiving message`() {
        service.sendMessage(user1.id, user2.id, "Привет!")
        assertEquals(1, service.getUnreadChatsCount())
    }

    @Test
    fun `should return last message texts including 'нет сообщений' for empty chat`() {
        service.sendMessage(user1.id, user2.id, "Привет!")
        service.deleteChat(user2.id)
        service.createChatIfNotExists("u3")

        val lastMessages = service.getLastMessages()
        assertEquals(listOf("нет сообщений"), lastMessages)
    }

    @Test
    fun `should return last N messages and mark them as read`() {
        service.sendMessage(user1.id, user2.id, "1")
        service.sendMessage(user1.id, user2.id, "2")
        service.sendMessage(user1.id, user2.id, "3")

        val messages = service.getMessagesFromChat(user2.id, 2)
        assertEquals(2, messages.size)
        assertEquals("2", messages[0].text)
        assertEquals("3", messages[1].text)

        // Проверим, что помечены как прочитанные
        val chat = service.getChats().first()
        assertEquals(0, chat.getUnreadCount())
    }

    @Test
    fun `should create chat on first message`() {
        service.sendMessage(user1.id, user2.id, "Первое сообщение")
        assertTrue(service.getChats().any { it.partnerId == user2.id })
    }

    @Test
    fun `should delete message from chat`() {
        val message = service.sendMessage(user1.id, user2.id, "Удалить меня")
        assertTrue(service.deleteMessage(user2.id, message.id))
        assertFalse(service.deleteMessage(user2.id, "invalid_id"))
    }

    @Test
    fun `should delete entire chat`() {
        service.sendMessage(user1.id, user2.id, "Сообщение")
        assertTrue(service.deleteChat(user2.id))
        assertEquals(0, service.getChats().size)
    }

    @Test
    fun `should edit message text`() {
        val message = service.sendMessage(user1.id, user2.id, "Старый текст")
        assertTrue(service.editMessage(user2.id, message.id, "Новый текст"))

        val messages = service.getMessagesFromChat(user2.id, 1)
        assertEquals("Новый текст", messages[0].text)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `should throw exception when sending message from non-existent user`() {
        service.sendMessage("unknown", user2.id, "Текст")
    }

    @Test(expected = NoSuchElementException::class)
    fun `should throw exception when getting messages from non-existent chat`() {
        service.getMessagesFromChat("unknown", 1)
    }
}