package de.edgespeak.tts
import android.media.AudioFormat
import android.speech.tts.*
import de.edgespeak.core.*
import java.io.File
import java.util.Locale
class EdgeTtsService:TextToSpeechService() {
 private val client=EdgeClient();private val stateLock=Any();private var active:Cancellation?=null
 @Volatile private var loaded=Voices.all.first()
 private val prefs get()=getSharedPreferences("settings",MODE_PRIVATE)
 private val consentListener=android.content.SharedPreferences.OnSharedPreferenceChangeListener { _,key->if(key=="consent"&&!prefs.getBoolean("consent",false))synchronized(stateLock){active?.revokeConsent()} }
 override fun onCreate(){super.onCreate();prefs.registerOnSharedPreferenceChangeListener(consentListener);cacheDir.listFiles()?.filter{it.name.startsWith("speech-")&&it.name.endsWith(".mp3")}?.forEach{it.delete()}}
 override fun onGetLanguage()=Locale.forLanguageTag(loaded.tag).let{arrayOf(it.isO3Language,it.isO3Country,"")}
 private fun resolve(lang:String,country:String):EdgeVoice?=Voices.all.find{val l=Locale.forLanguageTag(it.tag);(lang==l.language||lang==l.isO3Language)&&(country.isEmpty()||country==l.country||country==l.isO3Country)}
 override fun onIsLanguageAvailable(lang:String,country:String,variant:String):Int = when{resolve(lang,country)!=null->if(country.isEmpty())TextToSpeech.LANG_AVAILABLE else TextToSpeech.LANG_COUNTRY_AVAILABLE;resolve(lang,"")!=null->TextToSpeech.LANG_AVAILABLE;else->TextToSpeech.LANG_NOT_SUPPORTED}
 override fun onLoadLanguage(lang:String,country:String,variant:String):Int {val result=onIsLanguageAvailable(lang,country,variant);if(result>=0)loaded=resolve(lang,country)?:resolve(lang,"")!!;return result}
 override fun onGetVoices()=Voices.all.map{Voice(it.name,Locale.forLanguageTag(it.tag),Voice.QUALITY_NORMAL,Voice.LATENCY_HIGH,true,setOf(TextToSpeech.Engine.KEY_FEATURE_NETWORK_SYNTHESIS))}.toMutableList()
 override fun onGetFeaturesForLanguage(lang:String,country:String,variant:String)=if(onIsLanguageAvailable(lang,country,variant)>=0) mutableSetOf(TextToSpeech.Engine.KEY_FEATURE_NETWORK_SYNTHESIS) else mutableSetOf()
 override fun onIsValidVoiceName(name:String)=if(Voices.all.any{it.name==name})TextToSpeech.SUCCESS else TextToSpeech.ERROR
 override fun onLoadVoice(name:String):Int {val v=Voices.all.find{it.name==name}?:return TextToSpeech.ERROR;loaded=v;return TextToSpeech.SUCCESS}
 override fun onGetDefaultVoiceNameFor(lang:String,country:String,variant:String):String? {val preferred=Voices.all.find{it.name==prefs.getString("voice",null)};return preferred?.takeIf{val l=Locale.forLanguageTag(it.tag);(lang==l.language||lang==l.isO3Language)&&(country.isEmpty()||country==l.country||country==l.isO3Country)}?.name?:(resolve(lang,country)?:resolve(lang,""))?.name}
 override fun onStop(){synchronized(stateLock){active?.cancel()}}
 override fun onSynthesizeText(request:SynthesisRequest,callback:SynthesisCallback){
 val c=Cancellation();synchronized(stateLock){active?.cancel();active=c}
 val terminal=SynthesisTerminal({callback.error(it)},{callback.done();Unit})
 var file:File?=null
 try {
 if(!prefs.getBoolean("consent",false)){terminal.error(TextToSpeech.ERROR_NOT_INSTALLED_YET);return}
 val text=request.charSequenceText.toString();if(text.isBlank()||text.length>20000){terminal.error(TextToSpeech.ERROR_INVALID_REQUEST);return}
 val v=if(!request.voiceName.isNullOrEmpty())Voices.all.find{it.name==request.voiceName} else Voices.all.find{it.name==onGetDefaultVoiceNameFor(request.language,request.country,request.variant)}
 if(!validSynthesisInput(text,v?.name)){terminal.error(TextToSpeech.ERROR_INVALID_REQUEST);return}
 fun accepted(result:Int){if(result!=TextToSpeech.SUCCESS)throw CallbackRejected(result)}
 c.guarded{accepted(callback.start(24000,AudioFormat.ENCODING_PCM_16BIT,1))}
 val rate=(request.speechRate-100).coerceIn(-75,200);val pitch=((request.pitch-100)/2).coerceIn(-50,50)
 for(chunk in Protocol.chunks(text)){
 if(!prefs.getBoolean("consent",false))c.revokeConsent();c.check();val bytes=client.synthesize(chunk,v!!.name,rate,pitch,c);c.check();file=File.createTempFile("speech-",".mp3",cacheDir);file.writeBytes(bytes)
 PcmDecoder.decode(file,c){pcm->var offset=0;val max=callback.maxBufferSize.coerceAtMost(8192) and -2;check(max>0);while(offset<pcm.size){val n=minOf(max,pcm.size-offset);c.guarded{if(!prefs.getBoolean("consent",false))c.revokeConsent();c.check();accepted(callback.audioAvailable(pcm,offset,n))};offset+=n}}
 file.delete();file=null
 };c.guarded{if(!prefs.getBoolean("consent",false))c.revokeConsent();c.check();terminal.success()}
 }catch(e:CallbackRejected){if(e.result!=-2)terminal.failure(c,TextToSpeech.ERROR_OUTPUT,TextToSpeech.ERROR_NOT_INSTALLED_YET);c.cancel()}catch(_:Exception){terminal.failure(c,TextToSpeech.ERROR_NETWORK,TextToSpeech.ERROR_NOT_INSTALLED_YET)}finally{file?.delete();synchronized(stateLock){if(active===c)active=null}}
 }
 override fun onDestroy(){prefs.unregisterOnSharedPreferenceChangeListener(consentListener);onStop();client.close();super.onDestroy()}
 // AOSP callback STOPPED is -2; it is distinct from ERROR (-1).
 private class CallbackRejected(val result:Int):Exception()
}
