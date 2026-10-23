package de.edgespeak.core
import org.junit.Assert.*
import org.junit.Test
class ReviewTest {
 @Test fun terminalErrorBeforeStartCompletesOnce(){val events=mutableListOf<String>();val t=SynthesisTerminal({events.add("error:$it")},{events.add("done")});t.error(-8);t.error(-6);t.success();assertEquals(listOf("error:-8","done"),events)}
 @Test fun delayedSinkDoesNotConsumeCodecBudget(){var now=0L;val timer=ProgressTimeout(30){now};timer.emitting{now+=46};timer.check();now+=29;timer.check();now+=2;try{timer.check();fail("stalled codec accepted")}catch(_:IllegalStateException){}}
 @Test fun codecProgressResetsBudget(){var now=0L;val timer=ProgressTimeout(30){now};repeat(10){now+=29;timer.check();timer.progress()}}
 @Test fun revokeIsDistinctFromFrameworkStop(){val c=Cancellation();c.revokeConsent();c.cancel();assertTrue(c.consentRevoked);try{c.guarded{fail("PCM after revoke")};fail()}catch(_:java.io.IOException){}}
 @Test fun redirectTargetsNeverReceiveHandshakeOrText(){
  for(code in listOf(302,307)) okhttp3.mockwebserver.MockWebServer().use{origin->okhttp3.mockwebserver.MockWebServer().use{target->
   origin.start();target.start();origin.enqueue(okhttp3.mockwebserver.MockResponse().setResponseCode(code).addHeader("Location",target.url("/")))
   EdgeClient().use{client->val ended=java.util.concurrent.CountDownLatch(1)
    client.transport.newWebSocket(okhttp3.Request.Builder().url(origin.url("/")).build(),object:okhttp3.WebSocketListener(){
     override fun onOpen(w:okhttp3.WebSocket,r:okhttp3.Response){w.send("fixture public text")}
     override fun onFailure(w:okhttp3.WebSocket,t:Throwable,r:okhttp3.Response?){ended.countDown()}
    });assertTrue(ended.await(5,java.util.concurrent.TimeUnit.SECONDS));assertEquals(1,origin.requestCount);assertEquals(0,target.requestCount)
   }
  }}
 }
 @Test fun frameworkStopThenRevokeRemainsStop(){val c=Cancellation();c.cancel();c.revokeConsent();assertFalse(c.consentRevoked)}
 @Test fun invalidRequestsAreRejected(){assertFalse(validSynthesisInput("", "en-US-AriaNeural"));assertFalse(validSynthesisInput(" ", "en-US-AriaNeural"));assertFalse(validSynthesisInput("x".repeat(20001), "en-US-AriaNeural"));assertFalse(validSynthesisInput("Hello", "unknown"));assertTrue(validSynthesisInput("Hello", "en-US-AriaNeural"))}
 @Test fun revocationCompletesWithErrorWhileFrameworkStopDoesNot(){
  for(revoke in listOf(true,false)){val c=Cancellation();val events=mutableListOf<Int>();val t=SynthesisTerminal({events.add(it)},{events.add(0)})
   if(revoke)c.revokeConsent() else c.cancel()
   t.failure(c,-6,-9);assertEquals(if(revoke)listOf(-9,0) else emptyList<Int>(),events)
  }
 }
 @Test fun redirectsDisabled(){EdgeClient().use{assertFalse(it.transport.followRedirects);assertFalse(it.transport.followSslRedirects)}}
}
