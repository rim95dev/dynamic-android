package io.github.rim95dev.island.notification

import io.github.rim95dev.island.settings.AppFilter
import io.github.rim95dev.island.settings.AppFilterMode
import io.github.rim95dev.island.settings.KeywordRule
import io.github.rim95dev.island.settings.KeywordRuleMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationGateTest {

    private val alerting = NotificationFacts(
        packageName = KAKAO,
        isOngoing = false,
        isGroupSummary = false,
        isSilent = false,
        isBlockedByDnd = false,
        isQuietUpdate = false,
        isScreenOn = true,
        isHiddenOnLockScreen = false,
        content = listOf("엄마", "(광고) 오늘만 할인"),
    )

    private fun shows(
        facts: NotificationFacts,
        filter: AppFilter = AppFilter(),
        keywordRule: KeywordRule? = null,
    ) = NotificationGate.shouldShow(facts, filter, keywordRule, OWN_PACKAGE)

    @Test
    fun `shows a new alerting notification while the screen is on`() {
        assertTrue(shows(alerting))
    }

    @Test
    fun `hides while the screen is off`() {
        assertFalse(shows(alerting.copy(isScreenOn = false)))
    }

    @Test
    fun `hides ongoing, group summary, silent, dnd and quiet updates`() {
        assertFalse(shows(alerting.copy(isOngoing = true)))
        assertFalse(shows(alerting.copy(isGroupSummary = true)))
        assertFalse(shows(alerting.copy(isSilent = true)))
        assertFalse(shows(alerting.copy(isBlockedByDnd = true)))
        assertFalse(shows(alerting.copy(isQuietUpdate = true)))
    }

    @Test
    fun `hides notifications that are secret on the lock screen`() {
        assertFalse(shows(alerting.copy(isHiddenOnLockScreen = true)))
    }

    @Test
    fun `hides its own notifications`() {
        assertFalse(shows(alerting.copy(packageName = OWN_PACKAGE)))
    }

    @Test
    fun `follows the app filter`() {
        assertFalse(shows(alerting, AppFilter().withException(KAKAO, isException = true)))
        assertFalse(shows(alerting, AppFilter(mode = AppFilterMode.NO_APPS)))
        assertTrue(shows(alerting, AppFilter(mode = AppFilterMode.NO_APPS).withException(KAKAO, isException = true)))
    }

    @Test
    fun `follows the app's keyword rule`() {
        assertFalse(shows(alerting, keywordRule = KeywordRule(KeywordRuleMode.HIDE_MATCHING, listOf("광고"))))
        assertTrue(shows(alerting, keywordRule = KeywordRule(KeywordRuleMode.HIDE_MATCHING, listOf("배송"))))
        assertTrue(shows(alerting, keywordRule = KeywordRule(KeywordRuleMode.SHOW_MATCHING, listOf("할인"))))
        assertFalse(shows(alerting, keywordRule = KeywordRule(KeywordRuleMode.SHOW_MATCHING, listOf("배송"))))
    }

    @Test
    fun `a keyword rule never shows what the app filter hides`() {
        val hiddenKakao = AppFilter().withException(KAKAO, isException = true)

        assertFalse(shows(alerting, hiddenKakao, KeywordRule(KeywordRuleMode.SHOW_MATCHING, listOf("할인"))))
    }

    private companion object {
        const val KAKAO = "com.kakao.talk"
        const val OWN_PACKAGE = "io.github.rim95dev.island"
    }
}
