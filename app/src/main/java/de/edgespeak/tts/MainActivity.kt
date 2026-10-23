package de.edgespeak.tts
import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.graphics.Color
import android.speech.tts.*
import android.view.View
import android.widget.*
import de.edgespeak.core.Voices
class MainActivity:Activity(){
 private var tts:TextToSpeech?=null;private var ready=false;private lateinit var status:TextView
 private val prefs get()=getSharedPreferences("settings",MODE_PRIVATE)
 override fun onCreate(state:Bundle?){setTheme(if(resources.configuration.uiMode and 0x30==0x20) android.R.style.Theme_Material_NoActionBar else android.R.style.Theme_Material_Light_NoActionBar);super.onCreate(state)
 val dark=resources.configuration.uiMode and 0x30==0x20;val bg=if(dark)Color.rgb(29,33,32) else Color.rgb(239,245,239);val fg=if(dark)Color.WHITE else Color.rgb(24,48,35)
 val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(24,32,24,32);setBackgroundColor(bg)}
 fun label(s:String,size:Float=16f)=TextView(this).apply{text=s;textSize=size;setTextColor(fg);setPadding(0,12,0,12);root.addView(this)}
 fun button(s:String,action:()->Unit)=Button(this).apply{text=s;setOnClickListener{action()};root.addView(this)}
 label("EdgeSpeak",32f);label("Deine Android-Stimme. Online.",20f)
 label("EdgeSpeak sendet vorzulesenden Text über HTTPS an Microsoft. Internet ist für jede Hörprobe und jede andere App nötig. Keine Offline-Stimmen, kein API-Schlüssel. Keine Text- oder Audiochronik.")
 label("Unabhängiges Projekt, nicht von Microsoft unterstützt. Der undokumentierte Edge-Read-Aloud-Dienst kann sich ändern oder ausfallen.")
 val consent=CheckBox(this).apply{text="Ich stimme der Textübertragung an Microsoft zu";setTextColor(fg);isChecked=prefs.getBoolean("consent",false);root.addView(this)}
 consent.setOnCheckedChangeListener{_,checked->prefs.edit().putBoolean("consent",checked).apply()}
 label("Stimme für Hörprobe und Standardauswahl")
 val spinner=Spinner(this);spinner.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,Voices.all.map{it.name});spinner.setSelection(Voices.all.indexOfFirst{it.name==prefs.getString("voice",Voices.all.first().name)}.coerceAtLeast(0));root.addView(spinner)
 spinner.onItemSelectedListener=object:android.widget.AdapterView.OnItemSelectedListener{override fun onNothingSelected(p:android.widget.AdapterView<*>?){};override fun onItemSelected(p:android.widget.AdapterView<*>?,v:View?,pos:Int,id:Long){prefs.edit().putString("voice",Voices.all[pos].name).apply()}}
 fun slider(key:String,title:String):SeekBar{val caption=label(title);return SeekBar(this).apply{max=150;progress=prefs.getInt(key,50);root.addView(this);fun update(){caption.text="$title: ${progress+50}%"};update();setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{override fun onStartTrackingTouch(s:SeekBar){};override fun onStopTrackingTouch(s:SeekBar){};override fun onProgressChanged(s:SeekBar,n:Int,user:Boolean){prefs.edit().putInt(key,n).apply();update()}})}}
 val rate=slider("rate","Tempo");val pitch=slider("pitch","Tonhöhe")
 label("Tempo und Tonhöhe gelten für diese Hörprobe. Andere Apps geben ihre eigenen Werte vor. Eine ausdrücklich angeforderte Stimme hat Vorrang vor der Standardauswahl.")
 val input=EditText(this).apply{setTextColor(fg);setText("Guten Tag! Willkommen bei EdgeSpeak.");hint="Text für die Hörprobe";maxLines=4;filters=arrayOf(android.text.InputFilter.LengthFilter(2000));root.addView(this)}
 status=label("Engine wird verbunden …")
 button("Hörprobe starten"){if(!consent.isChecked){status.text="Bitte zuerst der Textübertragung zustimmen."}else if(!ready){status.text="TTS-Engine ist noch nicht bereit."}else{val voice=tts?.voices?.find{it.name==Voices.all[spinner.selectedItemPosition].name};if(voice==null){status.text="Stimme nicht verfügbar."}else{tts?.voice=voice;tts?.setSpeechRate((rate.progress+50)/100f);tts?.setPitch((pitch.progress+50)/100f);if(tts?.speak(input.text.toString(),TextToSpeech.QUEUE_FLUSH,Bundle(),"preview")!=TextToSpeech.SUCCESS)status.text="Hörprobe konnte nicht gestartet werden."}}}
 button("Stopp"){tts?.stop();status.text="Gestoppt"}
 button("Als Android-Standard auswählen"){try{startActivity(Intent("com.android.settings.TTS_SETTINGS"))}catch(_:Exception){status.text="Bitte Einstellungen → Sprache und Eingabe → Text-in-Sprache öffnen."}}
 button("Open-Source-Lizenzen"){val names=assets.list("licenses")!!.sorted();val text=names.joinToString("\n\n"){name->"=== $name ===\n"+assets.open("licenses/$name").bufferedReader().use{it.readText()}};val view=TextView(this).apply{this.text=text;setPadding(24,24,24,24);setTextIsSelectable(true)};android.app.AlertDialog.Builder(this).setTitle("Open-Source-Lizenzen").setView(ScrollView(this).apply{addView(view)}).setPositiveButton("Schließen",null).show()}
 label("Wähle dort EdgeSpeak als bevorzugte Engine. Kindle: bei geeigneten Büchern unter Aa → Mehr → Assistive Reader. Verfügbarkeit und Nutzung der Systemstimme hängen von Kindle-Version und Buch ab.")
 setContentView(ScrollView(this).apply{addView(root);setOnApplyWindowInsetsListener{v,insets->v.setPadding(insets.systemWindowInsetLeft,insets.systemWindowInsetTop,insets.systemWindowInsetRight,insets.systemWindowInsetBottom);insets}})
 tts=TextToSpeech(this,{result->runOnUiThread{ready=result==TextToSpeech.SUCCESS;status.text=if(ready)"Bereit · Online-Engine" else "Engine-Verbindung fehlgeschlagen"}},packageName)
 tts?.setOnUtteranceProgressListener(object:UtteranceProgressListener(){override fun onStart(id:String){show("Sprache wird geladen …")};override fun onDone(id:String){show("Hörprobe beendet")};@Deprecated("platform") override fun onError(id:String){show("Sprachausgabe fehlgeschlagen. Zustimmung, Internet und Dienstverfügbarkeit prüfen.")};override fun onError(id:String,code:Int){show("Sprachausgabe fehlgeschlagen ($code). Zustimmung, Internet oder Dienst prüfen.")}})
 }
 private fun show(s:String){runOnUiThread{status.text=s}}
 override fun onDestroy(){tts?.stop();tts?.shutdown();super.onDestroy()}
}
