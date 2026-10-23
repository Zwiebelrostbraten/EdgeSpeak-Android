package de.edgespeak.core
import okhttp3.*
import okio.ByteString
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
class Cancellation {
 private val lock=Any()
 @Volatile var cancelled=false; private set
 @Volatile var consentRevoked=false; private set
 private var socket:WebSocket?=null
 fun revokeConsent()=synchronized(lock){if(!cancelled){consentRevoked=true;cancel()}}
 fun attach(s:WebSocket)=synchronized(lock){if(cancelled)s.cancel() else socket=s}
 fun cancel()=synchronized(lock){cancelled=true;socket?.cancel()}
 fun check(){if(cancelled)throw IOException("Cancelled")}
 fun <T> guarded(block:()->T):T=synchronized(lock){check();block()}
}
object Protocol {
 const val TOKEN="6A5AA1D4EAFF4E9FB37E23D68491D6F4"
 fun escape(s:String)=buildString { s.codePoints().forEach { c->append(when(c){38->"&amp;";60->"&lt;";62->"&gt;";34->"&quot;";39->"&apos;";else->if(c==9||c==10||c==13||c in 32..0xD7FF||c in 0xE000..0xFFFD||c in 0x10000..0x10FFFF)String(Character.toChars(c)) else " "}) } }
 fun chunks(s:String,limit:Int=3000):List<String> {require(s.length<=20000);require(limit>=12);val out=mutableListOf<String>();var b=StringBuilder();var size=0
 s.codePoints().forEach{c->val e=escape(String(Character.toChars(c)));val n=e.toByteArray().size;if(size+n>limit){out.add(b.toString());b=StringBuilder();size=0};b.append(e);size+=n};if(b.isNotEmpty())out.add(b.toString());return out }
 fun audio(b:ByteArray):ByteArray {require(b.size>=2);val n=((b[0].toInt() and 255) shl 8)+(b[1].toInt() and 255);require(n+2<=b.size);val h=String(b,2,n,Charsets.UTF_8);val headers=h.split("\r\n").filter{it.contains(':')}.associate{it.substringBefore(':') to it.substringAfter(':').trim()};require(headers["Path"]=="audio");require(headers["Content-Type"].isNullOrEmpty()||headers["Content-Type"]=="audio/mpeg");return b.copyOfRange(n+2,b.size)}
 fun ssml(escaped:String,voice:String,rate:Int,pitch:Int):String {require(Voices.all.any{it.name==voice});return "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='en-US'><voice name='$voice'><prosody pitch='${signed(pitch.coerceIn(-50,50))}Hz' rate='${signed(rate.coerceIn(-75,200))}%' volume='+0%'>$escaped</prosody></voice></speak>"}
 private fun signed(n:Int)=if(n>=0)"+$n" else "$n"
}
data class EdgeVoice(val name:String,val tag:String)
object Voices {
 val all=listOf(EdgeVoice("de-DE-KatjaNeural","de-DE"),EdgeVoice("de-DE-ConradNeural","de-DE"),EdgeVoice("de-DE-AmalaNeural","de-DE"),EdgeVoice("de-DE-KillianNeural","de-DE"),EdgeVoice("en-US-AriaNeural","en-US"),EdgeVoice("en-US-GuyNeural","en-US"),EdgeVoice("en-GB-SoniaNeural","en-GB"))
 fun resolve(name:String?,tag:String)=all.find{it.name==name}?:all.find{it.tag.equals(tag,true)}?:all.find{it.tag.take(2)==tag.take(2)}
}
class EdgeClient:java.io.Closeable {
 internal val transport=OkHttpClient.Builder().followRedirects(false).followSslRedirects(false).connectTimeout(10,TimeUnit.SECONDS).readTimeout(20,TimeUnit.SECONDS).build()
 fun synthesize(escaped:String,voice:String,rate:Int,pitch:Int,cancel:Cancellation):ByteArray {
 cancel.check();val done=CountDownLatch(1);val output=ByteArrayOutputStream();var failure:IOException?=null;var completed=false
 val id=UUID.randomUUID().toString().replace("-","");val url="https://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1?TrustedClientToken=${Protocol.TOKEN}&ConnectionId=$id&Sec-MS-GEC=${Gec.token()}&Sec-MS-GEC-Version=2-143.0.3650.75"
 val req=Request.Builder().url(url).header("Origin","chrome-extension://jdiccldimpdaibmpdkjnbmckianbfold").header("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0").header("Cookie","muid=${UUID.randomUUID().toString().replace("-","").uppercase()};").build()
 val ws=transport.newWebSocket(req,object:WebSocketListener(){
 override fun onOpen(w:WebSocket,r:Response){if(cancel.cancelled){w.cancel();done.countDown();return};try{cancel.guarded{w.send("Content-Type:application/json; charset=utf-8\r\nPath:speech.config\r\n\r\n{\"context\":{\"synthesis\":{\"audio\":{\"metadataoptions\":{\"sentenceBoundaryEnabled\":\"false\",\"wordBoundaryEnabled\":\"false\"},\"outputFormat\":\"audio-24khz-48kbitrate-mono-mp3\"}}}}\r\n");w.send("X-RequestId:$id\r\nContent-Type:application/ssml+xml\r\nPath:ssml\r\n\r\n"+Protocol.ssml(escaped,voice,rate,pitch))}}catch(_:IOException){w.cancel();done.countDown()}}
 override fun onMessage(w:WebSocket,b:ByteString){try{cancel.check();val a=Protocol.audio(b.toByteArray());if(output.size()+a.size>2_000_000)throw IOException("Audio limit");output.write(a)}catch(e:Exception){failure=IOException("Invalid audio frame",e);w.cancel();done.countDown()}}
 override fun onMessage(w:WebSocket,s:String){if(s.substringBefore("\r\n\r\n").contains("Path:turn.end")){completed=true;done.countDown();w.close(1000,null)}}
 override fun onFailure(w:WebSocket,t:Throwable,r:Response?){failure=IOException("Network error HTTP ${r?.code ?: 0}: ${t.javaClass.simpleName}");done.countDown()}
 override fun onClosed(w:WebSocket,code:Int,reason:String){done.countDown()}
 });cancel.attach(ws)
 try {if(!done.await(35,TimeUnit.SECONDS))throw IOException("Network timeout");cancel.check();failure?.let{throw it};if(!completed||output.size()==0)throw IOException("Incomplete audio turn");return output.toByteArray()}finally{ws.cancel()}
 }
 override fun close(){transport.dispatcher.cancelAll();transport.dispatcher.executorService.shutdown();transport.connectionPool.evictAll()}
}
