import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.*

class MessagingServiceTest {
    private lateinit var service: MessagingService
    private val user1 = User("1", "Alice")
    private val user2 = User("2", "Bob")
    private val user3 = User("3", "Charlie")

    @Before
    fun setUp() {
        service = MessagingService()
        service.addUser(user1)
        service.addUser(user2)
        service.addUser(user3)
    }

    @Test
    fun `addUser should add user correctly`() {
        assertEquals(user1, service.getUser("1"))
        assertEquals(user2, service.getUser("2"))
        assertNull(service.getUser("999"))
    }

    @Test
    fun `sendMessage should create message and chat`() {
        val message = service.sendMessage("1", "2", "Hello Bob")

        assertEquals("Hello Bob", message.text)
        assertEquals("1", message.senderId)
        assertFalse(message.isRead)

        val chats = service.getChats().toList()
        assertEquals(1, chats.size)
        assertEquals("2", chats[0].partnerId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `sendMessage should throw when user not found`() {
        service.sendMessage("1", "999", "Invalid")
    }

    @Test
    fun `getMessagesFromChat should return messages and mark as read`() {
        service.sendMessage("1", "2", "Message 1")
        service.sendMessage("1", "2", "Message 2")

        val messages = service.getMessagesFromChat("2", 10)
        assertEquals(2, messages.size)
        assertTrue(messages.all { it.isRead })

        assertEquals(0, service.getUnreadChatsCount())
    }

    @Test(expected = NoSuchElementException::class)
    fun `getMessagesFromChat should throw when chat not found`() {
        service.getMessagesFromChat("999", 10)
    }

    @Test
    fun `deleteMessage should remove message`() {
        val message = service.sendMessage("1", "2", "To be deleted")
        assertTrue(service.deleteMessage("2", message.id))
        assertFalse(service.deleteMessage("2", "nonexistent"))
        assertFalse(service.deleteMessage("999", message.id))
    }

    @Test
    fun `editMessage should update message text`() {
        val message = service.sendMessage("1", "2", "Original")
        assertTrue(service.editMessage("2", message.id, "Updated"))

        val messages = service.getMessagesFromChat("2", 1)
        assertEquals("Updated", messages[0].text)

        assertFalse(service.editMessage("2", "nonexistent", "New"))
        assertFalse(service.editMessage("999", message.id, "New"))
    }

    @Test
    fun `getUnreadChatsCount should return correct count`() {
        assertEquals(0, service.getUnreadChatsCount())

        service.sendMessage("1", "2", "Hi")
        assertEquals(1, service.getUnreadChatsCount())

        service.getMessagesFromChat("2", 1)
        assertEquals(0, service.getUnreadChatsCount())
    }

    @Test
    fun `getLastMessages should return last messages`() {
        service.sendMessage("1", "2", "First to Bob")
        service.sendMessage("1", "3", "First to Charlie")
        service.sendMessage("1", "2", "Second to Bob")

        val lastMessages = service.getLastMessages().toList()
        assertEquals(2, lastMessages.size)
        assertTrue(lastMessages.contains("Second to Bob"))
        assertTrue(lastMessages.contains("First to Charlie"))
    }

    @Test
    fun `deleteChat should remove chat`() {
        service.sendMessage("1", "2", "Test")
        assertTrue(service.deleteChat("2"))
        assertFalse(service.deleteChat("999"))
    }

    @Test
    fun `createChatIfNotExists should create empty chat`() {
        service.createChatIfNotExists("2")
        assertTrue(service.getChats().any { it.partnerId == "2" && it.isEmpty() })
    }

    @Test
    fun `extension function filterUnread should work`() {
        service.sendMessage("1", "2", "Unread")
        service.sendMessage("1", "3", "Unread")
        service.getMessagesFromChat("2", 1) // marks as read

        val unreadChats = service.getChats().filterUnread().toList()
        assertEquals(1, unreadChats.size)
        assertEquals("3", unreadChats[0].partnerId)
    }

    @Test
    fun `extension function mapToLastTexts should work`() {
        service.sendMessage("1", "2", "First")
        service.sendMessage("1", "2", "Last")

        val lastTexts = service.getChats().mapToLastTexts().toList()
        assertEquals(1, lastTexts.size)
        assertEquals("Last", lastTexts[0])
    }
}