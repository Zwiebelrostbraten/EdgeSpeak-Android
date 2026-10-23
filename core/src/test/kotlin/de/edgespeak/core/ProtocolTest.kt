package de.edgespeak.core
import org.junit.Test
import org.junit.Assert.*
class ProtocolTest {
 @Test fun gecVector() { assertEquals("2CD23A2CA0B2595A-F3C846C370E05F9CC83ACC5F995C5F5D87A4D0F14CE8936A0F902E64099E38A2", Gec.token(1700000000)) }
 @Test fun voiceSelection() { assertEquals("de-DE-ConradNeural",Voices.resolve("de-DE-ConradNeural","en-US")!!.name);assertNull(Voices.resolve(null,"ja-JP")) }
 @Test fun binaryFrame() { val h="Path:audio\r\nContent-Type:audio/mpeg\r\n".toByteArray();assertArrayEquals(byteArrayOf(1,2),Protocol.audio(byteArrayOf(0,h.size.toByte())+h+byteArrayOf(1,2))) }
 @Test fun ssmlControls() { val s=Protocol.ssml("hello","de-DE-ConradNeural",25,-5);assertTrue(s.contains("rate='+25%'"));assertTrue(s.contains("pitch='-5Hz'"));assertTrue(s.contains("de-DE-ConradNeural")) }
 @Test fun cancelledAttach() {val c=Cancellation();c.cancel();assertThrows(java.io.IOException::class.java){c.guarded { fail("must not call callback") }} }
 @Test fun invalidXmlCharacters() {assertEquals("   😀",Protocol.escape("\u0000\uD800\uFFFF😀"))}
 @Test fun escaping() { assertEquals("&lt;&amp;&gt;&quot;&apos;", Protocol.escape("<&>\"'")) }
 @Test fun chunks() { val s="😀 &ä".repeat(2000); val c=Protocol.chunks(s); assertEquals(Protocol.escape(s),c.joinToString("")); assertTrue(c.all{it.toByteArray().size<=3000}); assertTrue(c.none{it.endsWith("&")}) }
 @Test fun malformedFrame() { assertThrows(IllegalArgumentException::class.java){Protocol.audio(byteArrayOf(0,50))} }
 @Test fun cancellation() { val c=Cancellation(); c.cancel(); assertThrows(java.io.IOException::class.java){c.check()} }
}
