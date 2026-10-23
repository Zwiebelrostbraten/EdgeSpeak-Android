package de.edgespeak.tts

import android.media.AudioFormat
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** Explicit opt-in: this one public sentence is transmitted to Microsoft. */
@RunWith(AndroidJUnit4::class)
class LiveFrameworkTest {
 @Test fun germanFrameworkFileIsPcmWav() {
  assumeTrue("Requires explicit liveEdge=true", InstrumentationRegistry.getArguments().getString("liveEdge") == "true")
  val instrumentation = InstrumentationRegistry.getInstrumentation()
  val ctx = instrumentation.targetContext
  val prefs = ctx.getSharedPreferences("settings", 0)
  val previous = prefs.all.toMap()
  val file = File(ctx.cacheDir, "live-framework.wav")
  var tts: TextToSpeech? = null
  val initialized = CountDownLatch(1)
  val finished = CountDownLatch(1)
  val initCode = AtomicInteger(999)
  val terminal = AtomicInteger(999)
  val format = AtomicInteger()
  val rate = AtomicInteger()
  val channels = AtomicInteger()
  try {
   // Never use user text or saved settings as synthesis input.
   prefs.edit().putBoolean("consent", true).commit()
   instrumentation.runOnMainSync {
    tts = TextToSpeech(ctx, { initCode.set(it); initialized.countDown() }, "de.edgespeak.tts")
   }
   assertTrue("TTS initialization timed out", initialized.await(20, TimeUnit.SECONDS))
   assertEquals(TextToSpeech.SUCCESS, initCode.get())
   assertTrue(tts!!.engines.any { it.name == "de.edgespeak.tts" })
   val voice = tts!!.voices.first { it.name == "de-DE-KatjaNeural" && it.locale.language == "de" }
   assertEquals(TextToSpeech.SUCCESS, tts!!.setVoice(voice))
   tts!!.setSpeechRate(1f)
   tts!!.setPitch(1f)
   tts!!.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
    override fun onStart(id: String) {}
    override fun onBeginSynthesis(id: String, sampleRateInHz: Int, audioFormat: Int, channelCount: Int) {
     rate.set(sampleRateInHz); format.set(audioFormat); channels.set(channelCount)
    }
    override fun onDone(id: String) { terminal.set(TextToSpeech.SUCCESS); finished.countDown() }
    @Deprecated("platform") override fun onError(id: String) { terminal.set(TextToSpeech.ERROR); finished.countDown() }
    override fun onError(id: String, errorCode: Int) { terminal.set(errorCode); finished.countDown() }
   })
   assertEquals(TextToSpeech.SUCCESS, tts!!.synthesizeToFile(
    "Guten Tag! Dies ist ein öffentlicher deutscher Testsatz für EdgeSpeak.", Bundle(), file, "live-german"))
   assertTrue("Framework synthesis timed out", finished.await(180, TimeUnit.SECONDS))
   assertEquals("Framework synthesis failed", TextToSpeech.SUCCESS, terminal.get())
   assertEquals(24000, rate.get()); assertEquals(AudioFormat.ENCODING_PCM_16BIT, format.get()); assertEquals(1, channels.get())
   assertPcmWav(file.readBytes())
  } finally {
   tts?.stop(); tts?.shutdown(); file.delete()
   val editor = prefs.edit().clear()
   previous.forEach { (key, value) -> when(value) {
    is Boolean -> editor.putBoolean(key, value)
    is String -> editor.putString(key, value)
    is Int -> editor.putInt(key, value)
    is Long -> editor.putLong(key, value)
    is Float -> editor.putFloat(key, value)
    is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
   } }
   check(editor.commit())
  }
 }
 @Test fun longFrameworkPlaybackCompletes()=playback(false)
 @Test fun consentRevokedAtFrameworkStartErrors()=playback(true)
 private fun playback(revoke:Boolean){
  assumeTrue(InstrumentationRegistry.getArguments().getString("liveEdge")=="true")
  val i=InstrumentationRegistry.getInstrumentation();val ctx=i.targetContext
  val prefs=ctx.getSharedPreferences("settings",0);val previous=prefs.getBoolean("consent",false)
  val init=CountDownLatch(1);val finished=CountDownLatch(1);val started=CountDownLatch(1)
  val result=AtomicInteger(999);val terminals=AtomicInteger();var tts:TextToSpeech?=null
  val revokedFile=File(ctx.cacheDir,"revocation-test.wav")
  try{
   prefs.edit().putBoolean("consent",true).commit()
   i.runOnMainSync{tts=TextToSpeech(ctx,{result.set(it);init.countDown()},ctx.packageName)}
   assertTrue(init.await(20,TimeUnit.SECONDS));assertEquals(0,result.get())
   tts!!.voice=tts!!.voices.first{it.name=="en-US-AriaNeural"};tts!!.setSpeechRate(1f)
   tts!!.setOnUtteranceProgressListener(object:UtteranceProgressListener(){
    override fun onStart(id:String){started.countDown();if(revoke)check(prefs.edit().putBoolean("consent",false).commit())}
    override fun onDone(id:String){terminals.incrementAndGet();result.set(0);finished.countDown()}
    @Deprecated("platform") override fun onError(id:String){onError(id,TextToSpeech.ERROR)}
    override fun onError(id:String,code:Int){terminals.incrementAndGet();result.set(code);finished.countDown()}
   })
   val paragraph="The public garden opens in the morning. A quiet path follows the river past trees and flowers. Visitors can sit on a bench and watch the water. This is a fixed public test paragraph about an ordinary peaceful day. "
   val begin=System.nanoTime()
   assertEquals(0,if(revoke)tts!!.synthesizeToFile(paragraph,Bundle(),revokedFile,"revoke-public") else tts!!.speak(paragraph.repeat(6),TextToSpeech.QUEUE_FLUSH,Bundle(),"long-public"))
   assertTrue("Playback never started",started.await(60,TimeUnit.SECONDS))
   assertTrue("Playback terminal timeout",finished.await(240,TimeUnit.SECONDS))
   assertEquals(1,terminals.get())
   if(revoke)assertEquals(TextToSpeech.ERROR_NOT_INSTALLED_YET,result.get()) else {
    assertEquals(0,result.get());assertTrue("Fixture must exercise over 40 seconds of playback",System.nanoTime()-begin>45_000_000_000L)
   }
  }finally{tts?.stop();tts?.shutdown();revokedFile.delete();prefs.edit().putBoolean("consent",previous).commit()}
 }
 private fun assertPcmWav(bytes: ByteArray) {
  fun tag(offset: Int) = String(bytes, offset, 4, Charsets.US_ASCII)
  val b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
  assertTrue("Missing WAV header (raw MP3 is invalid)", bytes.size >= 44)
  assertEquals("RIFF", tag(0)); assertEquals("WAVE", tag(8))
  var offset = 12; var foundFormat = false; var foundAudio = false
  while (offset + 8 <= bytes.size) {
   val size = b.getInt(offset + 4)
   assertTrue("Invalid WAV chunk", size >= 0 && size.toLong() + offset + 8 <= bytes.size)
   val start = offset + 8
   when (tag(offset)) {
    "fmt " -> {
     assertTrue(size >= 16)
     assertEquals(1, b.getShort(start).toInt()) // PCM, not compressed audio
     assertEquals(1, b.getShort(start + 2).toInt())
     assertEquals(24000, b.getInt(start + 4))
     assertEquals(48000, b.getInt(start + 8))
     assertEquals(2, b.getShort(start + 12).toInt())
     assertEquals(16, b.getShort(start + 14).toInt()); foundFormat = true
    }
    "data" -> {
     assertTrue("Empty or unaligned PCM", size > 0 && size % 2 == 0)
     assertTrue("Silent PCM", (start until start + size step 2).any { b.getShort(it).toInt() != 0 })
     foundAudio = true
    }
   }
   offset = start + size + size % 2
  }
  assertTrue("Missing PCM format/data chunks", foundFormat && foundAudio)
 }
}
