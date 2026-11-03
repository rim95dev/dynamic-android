package io.github.rim95dev.island.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Real-world message shapes. Add a sample here whenever a code is missed or a number is taken by mistake. */
class OneTimeCodeDetectorTest {

    private val codes = listOf(
        "[Web발신]\n[네이버] 인증번호 [482913]를 입력해주세요." to "482913",
        "[카카오] 인증번호 [3829]를 입력하세요." to "3829",
        "[토스] 인증번호는 192837 입니다. 타인에게 절대 알려주지 마세요." to "192837",
        "인증번호(482913)를 입력해 주세요. 유효시간 03:00" to "482913",
        "[KB국민카드] 본인확인 인증번호 [739201] 입니다. 2026/11/03 22:10" to "739201",
        "[신한은행] 인증번호 [123456] 타인 노출 금지. 문의 1577-8000" to "123456",
        "인증번호 482913 을 입력하세요. 고객센터 1588-1234" to "482913",
        "인증번호 482913 일회용이니 3분 안에 입력하세요" to "482913",
        "인증번호 [123 456]" to "123456",
        "G-482913 is your Google verification code." to "482913",
        "Your Apple Account code is: 482913. Don't share it with anyone.\n\n@apple.com #482913 %apple.com" to "482913",
        "Your verification code is 123-456" to "123456",
        "Use 8423 to verify your account" to "8423",
        "WhatsApp code 123-456. Don't share this code with others" to "123456",
        "Your Microsoft account security code is 1234567" to "1234567",
        "<#> 482913 is your verification code. FA+9qCX9VSu" to "482913",
        "Your verification code: A7K9Q2" to "A7K9Q2",
        "验证码：482913，5分钟内有效" to "482913",
        "認証コード: 482913" to "482913",
        "Your code for example.com\n\n@example.com #ABC123" to "ABC123",
        "Your PIN is 4821" to "4821",
    )

    private val notCodes = listOf(
        "[CJ대한통운] 고객님의 상품이 배송되었습니다. 운송장번호 123456789012",
        "[신한카드] 승인 홍길동 12,500원 일시불 11/03 22:10 스타벅스",
        "[현대카드] 승인번호 12345678 12,500원 일시불",
        "엄마: 내일 1234번 버스 타고 와",
        "Your order #482913 has shipped",
        "쿠폰 코드 SALE2026을 입력하세요",
        "Promo code SAVE2026: 20% off everything",
        "[네이버] 인증번호 요청이 있었습니다. 본인이 아니면 고객센터 1588-3820으로 연락주세요.",
        "Your code expires at 12:30 on 2026-11-03",
        "본인확인 요청 카드 ****1234",
        "인증번호 문의는 010 1234 5678로 연락주세요",
        "2026년 인증서 갱신 안내",
        "Shopping list: 4 apples, 2000 grams of flour",
    )

    @Test
    fun `finds the code in common formats`() {
        codes.forEach { (text, code) -> assertEquals(text, code, OneTimeCodeDetector.find(text)) }
    }

    @Test
    fun `leaves numbers that are not codes alone`() {
        notCodes.forEach { assertNull(it, OneTimeCodeDetector.find(it)) }
    }

    @Test
    fun `uses code words the user added`() {
        val text = "[우리동네앱] 입장번호 5821"

        assertNull(OneTimeCodeDetector.find(text))
        assertEquals("5821", OneTimeCodeDetector.find(text, extraKeywords = listOf("입장번호")))
    }

    @Test
    fun `prefers the number nearest to the code word`() {
        assertEquals("482913", OneTimeCodeDetector.find("주문 7731 관련 인증번호 482913"))
    }
}
