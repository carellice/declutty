package app.declutty.data

import org.junit.Assert.assertEquals
import org.junit.Test

class DuplicatesTest {
    private fun c(id: Long, name: String, phones: List<String> = emptyList(), emails: List<String> = emptyList(), unnamed: Boolean = false) =
        ContactItem(id, name, null, phones, emails, false, unnamed)

    @Test fun groups() {
        val list = listOf(
            c(1, "Mario Rossi", listOf("+39 333 123 4567")),
            c(2, "Rossi Mario"),
            c(3, "Mamma", listOf("3331234567")),
            c(4, "Luca", emails = listOf("Luca@Mail.it")),
            c(5, "Luca Bianchi", emails = listOf("luca@mail.it")),
            c(6, "Giulia"),
            c(7, "+39 06 1234", unnamed = true),
            c(8, "+39 06 1234", unnamed = true),
        )
        val groups = duplicateGroups(list).map { g -> g.map { it.id }.sorted() }.toSet()
        // 1-2 per nome, 1-3 per numero; 4-5 per email; 7-8 non hanno nome né numeri validi in comune (06 1234 è troppo corto).
        assertEquals(setOf(listOf(1L, 2L, 3L), listOf(4L, 5L)), groups)
    }

    @Test fun keys() {
        assertEquals("333123456".takeLast(9), phoneKey("+39 333 123 456"))
        assertEquals("mario rossi", nameKey("Rossi  Mário"))
    }
}
