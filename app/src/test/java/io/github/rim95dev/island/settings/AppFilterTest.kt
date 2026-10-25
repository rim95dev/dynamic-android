package io.github.rim95dev.island.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppFilterTest {

    @Test
    fun `default filter shows every app`() {
        val filter = AppFilter()

        assertEquals(AppFilterMode.ALL_APPS, filter.mode)
        assertTrue(filter.allows(KAKAO))
        assertTrue(filter.allows(MESSAGES))
    }

    @Test
    fun `all apps mode hides only excluded apps`() {
        val filter = AppFilter().withException(KAKAO, isException = true)

        assertFalse(filter.allows(KAKAO))
        assertTrue(filter.allows(MESSAGES))
    }

    @Test
    fun `no apps mode shows only included apps`() {
        val filter = AppFilter(mode = AppFilterMode.NO_APPS).withException(KAKAO, isException = true)

        assertTrue(filter.allows(KAKAO))
        assertFalse(filter.allows(MESSAGES))
    }

    @Test
    fun `removing an exception restores the mode default`() {
        val filter = AppFilter()
            .withException(KAKAO, isException = true)
            .withException(KAKAO, isException = false)

        assertTrue(filter.allows(KAKAO))
        assertTrue(filter.exceptions.isEmpty())
    }

    @Test
    fun `each mode keeps its own exception list`() {
        val hiddenKakao = AppFilter().withException(KAKAO, isException = true)
        val switched = hiddenKakao.copy(mode = AppFilterMode.NO_APPS)

        assertFalse(switched.allows(KAKAO))
        assertTrue(switched.exceptions.isEmpty())

        val switchedBack = switched.withException(MESSAGES, isException = true).copy(mode = AppFilterMode.ALL_APPS)

        assertEquals(setOf(KAKAO), switchedBack.exceptions)
        assertEquals(setOf(MESSAGES), switchedBack.includedPackages)
    }

    private companion object {
        const val KAKAO = "com.kakao.talk"
        const val MESSAGES = "com.samsung.android.messaging"
    }
}
