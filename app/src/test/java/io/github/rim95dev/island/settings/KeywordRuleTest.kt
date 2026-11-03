package io.github.rim95dev.island.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KeywordRuleTest {

    @Test
    fun `a rule without keywords allows everything`() {
        assertTrue(KeywordRule(KeywordRuleMode.HIDE_MATCHING).allows(listOf("광고")))
        assertTrue(KeywordRule(KeywordRuleMode.SHOW_MATCHING).allows(listOf("광고")))
    }

    @Test
    fun `hide mode hides notifications that contain a keyword`() {
        val rule = KeywordRule(KeywordRuleMode.HIDE_MATCHING, listOf("광고", "event"))

        assertFalse(rule.allows(listOf("쇼핑몰", "(광고) 오늘만 할인")))
        assertFalse(rule.allows(listOf("New EVENT for you")))
        assertTrue(rule.allows(listOf("엄마", "밥 먹었어?")))
    }

    @Test
    fun `show mode shows only notifications that contain a keyword`() {
        val rule = KeywordRule(KeywordRuleMode.SHOW_MATCHING, listOf("배송"))

        assertTrue(rule.allows(listOf("쿠팡", "오늘 배송 예정입니다")))
        assertFalse(rule.allows(listOf("쿠팡", "추천 상품을 확인하세요")))
    }

    @Test
    fun `any of the texts can match`() {
        val rule = KeywordRule(KeywordRuleMode.HIDE_MATCHING, listOf("회사 단톡"))

        assertFalse(rule.allows(listOf("김철수", "내일 봐요", "회사 단톡방")))
    }

    @Test
    fun `adding keeps keywords trimmed and unique, ignoring case`() {
        val rule = KeywordRule()
            .withKeyword("  Sale ")
            .withKeyword("sale")
            .withKeyword("   ")
            .withKeyword("line\nbreak")

        assertEquals(listOf("Sale", "line break"), rule.keywords)
    }

    @Test
    fun `removing a keyword drops only that keyword`() {
        val rule = KeywordRule(keywords = listOf("a", "b")).withoutKeyword("a")

        assertEquals(listOf("b"), rule.keywords)
    }

    @Test
    fun `encoding round trips keywords with spaces and commas`() {
        val rule = KeywordRule(KeywordRuleMode.SHOW_MATCHING, listOf("인증번호", "a, b", "two words"))

        assertEquals(rule, KeywordRule.decode(rule.encode()))
    }

    @Test
    fun `decoding rejects missing or unknown values`() {
        assertNull(KeywordRule.decode(null))
        assertNull(KeywordRule.decode("SOMETHING_ELSE\nword"))
        assertEquals(KeywordRule(KeywordRuleMode.HIDE_MATCHING), KeywordRule.decode("HIDE_MATCHING"))
    }
}
