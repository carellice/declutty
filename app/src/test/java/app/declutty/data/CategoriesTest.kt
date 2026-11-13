package app.declutty.data

import org.junit.Assert.assertEquals
import org.junit.Test

class CategoriesTest {
    @Test fun keywords() {
        assertEquals(Category.COMUNICAZIONE, categorize("com.whatsapp", "WhatsApp", 4))
        assertEquals(Category.FINANZA, categorize("com.revolut.revolut", "Revolut", -1))
        assertEquals(Category.VIDEO, categorize("com.netflix.mediaclient", "Netflix", -1))
        assertEquals(Category.SOCIAL, categorize("com.instagram.android", "Instagram", 4))
        assertEquals(Category.VIAGGI, categorize("com.waze", "Waze", -1))
        assertEquals(Category.FOTO, categorize("com.google.android.apps.photos", "Foto", -1))
    }

    @Test fun declaredGameWins() = assertEquals(Category.GIOCHI, categorize("com.word.chums", "Word Chums", 0))

    @Test fun fallbackToDeclared() {
        assertEquals(Category.NOTIZIE, categorize("com.example.xyz", "Xyz", 5))
        assertEquals(Category.ALTRO, categorize("com.example.xyz", "Xyz", -1))
    }

    // Le parole corte non devono combaciare dentro parole più lunghe ("tv" in "atvremote", "pay" in "payload").
    @Test fun shortWordsExact() = assertEquals(Category.ALTRO, categorize("com.example.payload", "Atvx", -1))
}
