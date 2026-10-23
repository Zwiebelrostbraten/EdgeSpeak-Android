package de.edgespeak.core
import java.io.File
fun main(args:Array<String>){require(args.size==1){"Output directory required; only fixed public test sentences are sent"};val dir=File(args[0]);dir.mkdirs();EdgeClient().use{client->listOf(Triple("de-DE-KatjaNeural","Guten Tag. Dies ist ein öffentlicher Sprachtest.",0),Triple("en-US-AriaNeural","Hello. This is a public speech test.",25),Triple("de-DE-ConradNeural","Ein kurzer Test mit Umlauten: grün und schön. ".repeat(4),-10)).forEachIndexed{i,t->Protocol.chunks(t.second,120).forEachIndexed{j,c->val start=System.nanoTime();val b=client.synthesize(c,t.first,t.third,0,Cancellation());File(dir,"$i-$j.mp3").writeBytes(b);println("sample=$i chunk=$j bytes=${b.size} elapsedMs=${(System.nanoTime()-start)/1000000}")}}}}

// Optional entry point kept deterministic; cancellation is also covered by JVM tests.
