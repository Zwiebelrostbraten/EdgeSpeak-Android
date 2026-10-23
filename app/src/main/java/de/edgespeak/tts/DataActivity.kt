package de.edgespeak.tts
import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.speech.tts.TextToSpeech
import de.edgespeak.core.Voices
import java.util.Locale
class DataActivity:Activity(){override fun onCreate(state:Bundle?){super.onCreate(state);when(intent.action){TextToSpeech.Engine.ACTION_CHECK_TTS_DATA->{val names=ArrayList(Voices.all.map{Locale.forLanguageTag(it.tag).let{l->"${l.isO3Language}-${l.isO3Country}"}}.distinct());setResult(TextToSpeech.Engine.CHECK_VOICE_DATA_PASS,Intent().putStringArrayListExtra(TextToSpeech.Engine.EXTRA_AVAILABLE_VOICES,names).putStringArrayListExtra(TextToSpeech.Engine.EXTRA_UNAVAILABLE_VOICES,arrayListOf()));finish()};TextToSpeech.Engine.ACTION_GET_SAMPLE_TEXT->{val english=intent.getStringExtra("language") in listOf("eng","en");setResult(TextToSpeech.LANG_AVAILABLE,Intent().putExtra("sampleText",if(english)"Hello. This is EdgeSpeak." else "Guten Tag. Das ist EdgeSpeak."));finish()};else->{startActivity(Intent(this,MainActivity::class.java));finish()}}}}
