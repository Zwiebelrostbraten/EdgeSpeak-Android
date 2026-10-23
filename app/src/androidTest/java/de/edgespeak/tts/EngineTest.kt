package de.edgespeak.tts
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import de.edgespeak.core.Cancellation
import java.io.File
@RunWith(AndroidJUnit4::class)
class EngineTest {
 @Test fun frameworkConsentGateReturnsErrorWithoutAudio()=consentGate(false)
 @Test fun frameworkSpeakConsentGateReturnsErrorWithoutAudio()=consentGate(true)
 private fun consentGate(speak:Boolean){
 val i=InstrumentationRegistry.getInstrumentation();val ctx=i.targetContext;val prefs=ctx.getSharedPreferences("settings",0);val old=prefs.getBoolean("consent",false);prefs.edit().putBoolean("consent",false).commit()
 val init=java.util.concurrent.CountDownLatch(1);val done=java.util.concurrent.CountDownLatch(1);val result=java.util.concurrent.atomic.AtomicInteger(999);var tts:android.speech.tts.TextToSpeech?=null;val terminals=java.util.concurrent.atomic.AtomicInteger();val audio=java.util.concurrent.atomic.AtomicInteger();val f=File(ctx.cacheDir,"consent-test.wav")
 try{i.runOnMainSync{tts=android.speech.tts.TextToSpeech(ctx,{code->result.set(code);init.countDown()},ctx.packageName)};assertTrue(init.await(15,java.util.concurrent.TimeUnit.SECONDS));assertEquals(0,result.get())
 tts!!.setOnUtteranceProgressListener(object:android.speech.tts.UtteranceProgressListener(){override fun onStart(id:String){};override fun onDone(id:String){terminals.incrementAndGet();result.set(0);done.countDown()};@Deprecated("platform") override fun onError(id:String){terminals.incrementAndGet();result.set(-9);done.countDown()};override fun onError(id:String,code:Int){terminals.incrementAndGet();result.set(code);done.countDown()};override fun onAudioAvailable(id:String,b:ByteArray){audio.addAndGet(b.size)}})
 assertEquals(0,if(speak)tts!!.speak("Public consent gate test.",0,android.os.Bundle(),"consent") else tts!!.synthesizeToFile("Public consent gate test.",android.os.Bundle(),f,"consent"));assertTrue(done.await(15,java.util.concurrent.TimeUnit.SECONDS));assertEquals(android.speech.tts.TextToSpeech.ERROR_NOT_INSTALLED_YET,result.get());assertEquals(0,audio.get());assertEquals(1,terminals.get())
 }finally{tts?.shutdown();f.delete();prefs.edit().putBoolean("consent",old).commit()}
 }
 @Test fun realMediaCodecProducesPcm(){val i=InstrumentationRegistry.getInstrumentation();val f=File(i.targetContext.cacheDir,"decoder-test.mp3");try{i.context.assets.open("tone.mp3").use{input->f.outputStream().use{input.copyTo(it)}};var count=0;PcmDecoder.decode(f,Cancellation()){assertEquals(0,it.size%2);count+=it.size};assertTrue(count>8000)}finally{f.delete()}}
 @Test fun cancelledDecoderEmitsNothing(){val i=InstrumentationRegistry.getInstrumentation();val f=File(i.targetContext.cacheDir,"cancel-test.mp3");try{i.context.assets.open("tone.mp3").use{input->f.outputStream().use{input.copyTo(it)}};val c=Cancellation();c.cancel();try{PcmDecoder.decode(f,c){fail("Audio after stop")};fail("Expected cancellation")}catch(_:java.io.IOException){}}finally{f.delete()}}
 @Test fun registeredNetworkEngine(){val ctx=InstrumentationRegistry.getInstrumentation().targetContext;val info=ctx.packageManager.getServiceInfo(android.content.ComponentName(ctx,EdgeTtsService::class.java),android.content.pm.PackageManager.GET_META_DATA);assertTrue(info.exported);assertTrue(info.metaData.containsKey("android.speech.tts"));val service=EdgeTtsService();assertTrue(service.onGetVoices().all{it.isNetworkConnectionRequired});assertEquals(android.speech.tts.TextToSpeech.ERROR,service.onIsValidVoiceName("unknown"))}
}
