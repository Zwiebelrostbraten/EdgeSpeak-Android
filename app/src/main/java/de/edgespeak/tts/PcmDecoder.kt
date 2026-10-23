package de.edgespeak.tts
import android.media.*
import de.edgespeak.core.Cancellation
import de.edgespeak.core.ProgressTimeout
import java.io.File
/** Decodes MPEG frames into signed little-endian PCM. Never passes MP3 to the framework. */
object PcmDecoder {
 fun decode(file:File,cancel:Cancellation,emit:(ByteArray)->Unit) {
 require(file.length() in 1..2_000_000L)
 val extractor=MediaExtractor();var codec:MediaCodec?=null
 try {
 extractor.setDataSource(file.path);require(extractor.trackCount>0);val format=extractor.getTrackFormat(0)
 require(format.getString(MediaFormat.KEY_MIME)?.startsWith("audio/")==true)
 extractor.selectTrack(0);format.setInteger(MediaFormat.KEY_PCM_ENCODING,AudioFormat.ENCODING_PCM_16BIT)
 val decoder=MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!);codec=decoder;decoder.configure(format,null,null,0);decoder.start()
 val info=MediaCodec.BufferInfo();var inputEnd=false;var end=false;val timeout=ProgressTimeout();var total=0
 while(!end){cancel.check();timeout.check()
 if(!inputEnd){val index=decoder.dequeueInputBuffer(10000);if(index>=0){timeout.progress();val buffer=decoder.getInputBuffer(index)!!;val n=extractor.readSampleData(buffer,0);if(n<0){decoder.queueInputBuffer(index,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);inputEnd=true}else{decoder.queueInputBuffer(index,0,n,extractor.sampleTime,0);extractor.advance()}}}
 val index=decoder.dequeueOutputBuffer(info,10000)
 if(index==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){timeout.progress();val f=decoder.outputFormat;require(f.getInteger(MediaFormat.KEY_SAMPLE_RATE)==24000&&f.getInteger(MediaFormat.KEY_CHANNEL_COUNT)==1);require(!f.containsKey(MediaFormat.KEY_PCM_ENCODING)||f.getInteger(MediaFormat.KEY_PCM_ENCODING)==AudioFormat.ENCODING_PCM_16BIT)}
 if(index>=0){timeout.progress();try{if(info.size>0){require(info.size%2==0);total+=info.size;require(total<=16_000_000);val b=decoder.getOutputBuffer(index)!!;b.position(info.offset);b.limit(info.offset+info.size);val bytes=ByteArray(info.size);b.get(bytes);cancel.check();timeout.emitting{emit(bytes)}};end=info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM!=0}finally{decoder.releaseOutputBuffer(index,false)}}
 };require(total>0)
 }finally{try{codec?.stop()}finally{codec?.release();extractor.release()}}
 }
}
