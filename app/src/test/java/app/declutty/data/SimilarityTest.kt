package app.declutty.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SimilarityTest {
    private fun photo(id: Long, taken: Long, mp: Int = 12, favorite: Boolean = false) =
        Photo(id, taken, 0, mp * 1000, 1000, mp * 100_000L, "Camera", favorite, false)

    private fun features(p: Long, d: Long, color: Int = 100, sharp: Float = 100f) =
        PhotoFeatures(p, d, ByteArray(48) { color.toByte() }, sharp, 0.8f)

    @Test fun identicalPhotosHaveFullSimilarity() {
        assertEquals(1f, similarity(features(0x1234, 0x5678), features(0x1234, 0x5678)), 1e-6f)
    }

    @Test fun groupsBurstAndKeepsSharpest() {
        val photos = listOf(
            AnalyzedPhoto(photo(1, 0), features(0xFF00FF, 0x0F0F, sharp = 50f)),
            AnalyzedPhoto(photo(2, 2_000), features(0xFF00FE, 0x0F0F, sharp = 400f)),
            // Diversa: tutti i bit invertiti e colore lontano.
            AnalyzedPhoto(photo(3, 4_000), features(0xFF00FF.inv(), 0x0F0F.inv(), color = 250)),
        )
        val groups = groupSimilar(photos, PhotoSettings())
        assertEquals(1, groups.size)
        assertEquals(setOf(1L, 2L), groups[0].members.map { it.photo.id }.toSet())
        assertEquals(2L, groups[0].bestId)
        assertTrue(!groups[0].isExactDuplicate)
    }

    @Test fun exactCopiesFoundAcrossTimeAndFavoriteProtected() {
        val day = 24 * 60 * 60_000L
        val photos = listOf(
            AnalyzedPhoto(photo(1, 0, mp = 12), features(42, 7)),
            AnalyzedPhoto(photo(2, 30 * day, mp = 2, favorite = true), features(42, 7)),
        )
        val groups = groupSimilar(photos, PhotoSettings())
        assertEquals(1, groups.size)
        assertTrue(groups[0].isExactDuplicate)
        assertEquals(2L, groups[0].bestId) // la preferita vince anche se più piccola
        assertEquals(0, groupSimilar(photos, PhotoSettings(exactEverywhere = false)).size)
    }
}
