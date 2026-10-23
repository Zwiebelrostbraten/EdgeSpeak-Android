package de.edgespeak.core
/** Used only on the synthesis thread. AOSP requires done even after error before start. */
class SynthesisTerminal(private val reportError:(Int)->Unit, private val done:()->Unit) {
 private var finished=false
 fun error(code:Int){if(!finished){finished=true;reportError(code);done()}}
 fun failure(cancel:Cancellation,code:Int,consentCode:Int){if(cancel.consentRevoked)error(consentCode) else if(!cancel.cancelled)error(code)}
 fun success(){if(!finished){finished=true;done()}}
}
/** Codec no-progress budget, excluding time spent in a blocking framework sink. */
class ProgressTimeout(private val budget:Long=30_000_000_000L,private val clock:()->Long=System::nanoTime){
 private var last=clock()
 fun check(){check(clock()-last<budget){"Decoder stalled"}}
 fun progress(){last=clock()}
 fun <T> emitting(block:()->T):T {val start=clock();try{return block()}finally{last+=clock()-start}}
}

fun validSynthesisInput(text:String,voice:String?)=text.isNotBlank()&&text.length<=20000&&Voices.all.any{it.name==voice}
