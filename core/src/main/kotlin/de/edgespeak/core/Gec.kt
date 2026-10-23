package de.edgespeak.core
// Derived from rany2/edge-tts drm.py, LGPL-3.0. See THIRD_PARTY_NOTICES.md.
import java.nio.ByteBuffer
import java.nio.ByteOrder
object Gec {
private val SHA256_INITIAL = intArrayOf(0x6a09e667.toInt(),0xbb67ae85.toInt(),0x3c6ef372.toInt(),0xa54ff53a.toInt(),0x510e527f.toInt(),0x9b05688c.toInt(),0x1f83d9ab.toInt(),0x5be0cd19.toInt())
private val SHA256_STATE = intArrayOf(0xfd9fbea2.toInt(),0x91243ce7.toInt(),0xbe499094.toInt(),0xcd2e1e0a.toInt(),0x6c006f49.toInt(),0x4bd35ab.toInt(),0xfeb0a7e4.toInt(),0x3effbd2.toInt())
private val SHA256_SCHEDULE = intArrayOf(0xeb24bf9e.toInt(),0x1558691f.toInt(),0xa0c09ee2.toInt(),0xcfa32ba.toInt(),0xa622ee8c.toInt(),0xd1dd9491.toInt(),0xc0dd25a0.toInt(),0xf9e70ab6.toInt(),0xf0c31573.toInt(),0x7058ce4e.toInt(),0x7188e606.toInt(),0xe5ab33.toInt(),0xd258b060.toInt(),0x72766564.toInt())
private val SHA256_ROUND_CONSTANTS = intArrayOf(0x428a2f98.toInt(),0x71374491.toInt(),0xb5c0fbcf.toInt(),0xe9b5dba5.toInt(),0x3956c25b.toInt(),0x59f111f1.toInt(),0x923f82a4.toInt(),0xab1c5ed5.toInt(),0xd807aa98.toInt(),0x12835b01.toInt(),0x243185be.toInt(),0x550c7dc3.toInt(),0x72be5d74.toInt(),0x80deb1fe.toInt(),0x9bdc06a7.toInt(),0xc19bf174.toInt(),0xe49b69c1.toInt(),0xefbe4786.toInt(),0xfc19dc6.toInt(),0x240ca1cc.toInt(),0x2de92c6f.toInt(),0x4a7484aa.toInt(),0x5cb0a9dc.toInt(),0x76f988da.toInt(),0x983e5152.toInt(),0xa831c66d.toInt(),0xb00327c8.toInt(),0xbf597fc7.toInt(),0xc6e00bf3.toInt(),0xd5a79147.toInt(),0x6ca6351.toInt(),0x14292967.toInt(),0x27b70a85.toInt(),0x2e1b2138.toInt(),0x4d2c6dfc.toInt(),0x53380d13.toInt(),0x650a7354.toInt(),0x766a0abb.toInt(),0x81c2c92e.toInt(),0x92722c85.toInt(),0xa2bfe8a1.toInt(),0xa81a664b.toInt(),0xc24b8b70.toInt(),0xc76c51a3.toInt(),0xd192e819.toInt(),0xd6990624.toInt(),0xf40e3585.toInt(),0x106aa070.toInt(),0x19a4c116.toInt(),0x1e376c08.toInt(),0x2748774c.toInt(),0x34b0bcb5.toInt(),0x391c0cb3.toInt(),0x4ed8aa4a.toInt(),0x5b9cca4f.toInt(),0x682e6ff3.toInt(),0x748f82ee.toInt(),0x78a5636f.toInt(),0x84c87814.toInt(),0x8cc70208.toInt(),0x90befffa.toInt(),0xa4506ceb.toInt(),0xbef9a3f7.toInt(),0xc67178f2.toInt())
private val coefficients = arrayOf(longArrayOf(3895366065L,3005677776L),longArrayOf(1155409094L,2296378653L),longArrayOf(1005282748L,693190712L),longArrayOf(3365689651L,3005795295L),longArrayOf(2367603217L,3557143765L),longArrayOf(827258325L,652566332L),longArrayOf(3911487921L,1618477510L),longArrayOf(4037392853L,2192753147L),longArrayOf(101076211L,4199654362L),longArrayOf(3665427040L,545047652L),longArrayOf(3201434976L,1010595059L),longArrayOf(2314848760L,3536931681L),longArrayOf(905590982L,3137643201L),longArrayOf(3410617286L,3311559015L))
private fun r(x:Int,n:Int)=Integer.rotateRight(x,n)
private fun s0(x:Int)=r(x,7) xor r(x,18) xor (x ushr 3)
private fun s1(x:Int)=r(x,17) xor r(x,19) xor (x ushr 10)
private fun expand(w:IntArray,from:Int) { for(i in from..63) w[i]=s1(w[i-2])+w[i-7]+s0(w[i-15])+w[i-16] }
private fun rounds(st:IntArray,w:IntArray,first:Int):IntArray {
 var a=st[0];var b=st[1];var c=st[2];var d=st[3];var e=st[4];var f=st[5];var g=st[6];var h=st[7]
 for(i in first..63) {
 val k=if(first==14 && i==14) 0xC9DC06A7.toInt() else SHA256_ROUND_CONSTANTS[i]
 val t=h+(r(e,6) xor r(e,11) xor r(e,25))+(g xor (e and (f xor g)))+k+w[i]
 val u=(r(a,2) xor r(a,13) xor r(a,22))+((a and b) or (c and (a or b)))
 h=g;g=f;f=e;e=d+t;d=c;c=b;b=a;a=t+u
 }; return intArrayOf(a,b,c,d,e,f,g,h)
}
private fun sha(data:ByteArray):ByteArray {
 val w=IntArray(64);w[14]=ByteBuffer.wrap(byteArrayOf(0)+data.copyOfRange(0,3)).int;w[15]=ByteBuffer.wrap(data,3,4).int
 for(i in 16..29) {
 var v=SHA256_SCHEDULE[i-16]
 if(i-2>=14) v+=s1(w[i-2]) xor (if(i==16) 0x000B92C0 else 0)
 if(i-7>=14) v+=w[i-7]
 if(i-15>=14) v+=s0(w[i-15]) xor (if(i==29) 0x059C0B80 else 0)
 w[i]=v
 }
 w[30]=s1(w[28])+w[23]+s0(w[15])+w[14]+0xC9DC06A7.toInt()-SHA256_ROUND_CONSTANTS[14]
 expand(w,31)
 var st=rounds(SHA256_STATE,w,14).mapIndexed{i,v->v+SHA256_INITIAL[i]}.toIntArray()
 val length=57+data.size
 val rest=data.copyOfRange(7,data.size)+byteArrayOf(0x80.toByte())+ByteArray((55-length).mod(64))+ByteBuffer.allocate(8).putLong(length*8L).array()
 for(o in rest.indices step 64) { val b=ByteBuffer.wrap(rest,o,64);for(i in 0..15)w[i]=b.int;expand(w,16);st=rounds(st,w,0).mapIndexed{i,v->v+st[i]}.toIntArray() }
 return ByteBuffer.allocate(32).also{b->st.forEach{b.putInt(it)}}.array()
}
private fun u(v:Long)=v and 0xffffffffL
private fun rol(v:Long)=u((v shl 16) or (v ushr 16))
private fun cs(data:ByteArray,digest:ByteArray):ByteArray {
 val b=ByteBuffer.wrap(digest).order(ByteOrder.LITTLE_ENDIAN);val ka=u(b.int.toLong()) or 1;val kb=u(b.int.toLong()) or 1
 val factors=longArrayOf(u(ka*0x82893933L),u(kb*0x82893933L))
 val terms=coefficients.mapIndexed{i,c->u(c[0]*factors[i%2])}.toMutableList();val offsets=coefficients.map{it[1]}.toMutableList()
 val bytes=byteArrayOf(0)+data;val padded=bytes+ByteArray((-bytes.size).mod(8));val words=ByteBuffer.wrap(padded).order(ByteOrder.LITTLE_ENDIAN)
 var i=0
 while(words.remaining()>0){val word=u(words.int.toLong());var term=word*(if(i%2==0)ka else kb);var off=rol(word)*(if(i%2==0)0xEF0569FBL else 0xC31713DBL)
 if(i==0){term+=0xF2F90B1AL*factors[0];off+=0x0B1A0000L};terms.add(u(term));offsets.add(u(off));i++}
 var swap=0L;var last=0L;var state=0L;var acc=0L
 for(j in terms.indices step 2){val x=terms[j];val y=terms[j+1]
 var q=u(rol(u(offsets[j]+x))*0x76F50F34L);swap+=u(rol(q)*0xC3EFEA97L+q)
 q=u(rol(u(offsets[j+1]+y))*0x1881CE3CL);last=u(rol(q)*0x35BD1EC9L+q);swap=u(swap+last)
 state=u(state*ka+x);for(k in longArrayOf(0xCF98B111L,0x87085B9FL,0x12CEB96DL,0x257E1D83L))state=u(rol(state)*k)
 acc+=state;var z=u(state*kb+y);z=u(rol(z)*0xA27416F5L);z=u(rol(z)*0xD38396FFL);state=u(rol(u(rol(z)*0x7C932B89L))*0xBFA49F69L);acc=u(acc+state)
 }
 return ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putInt((last xor state).toInt()).putInt((swap xor acc).toInt()).array()
}
fun token(seconds:Long=System.currentTimeMillis()/1000):String {
 val t=(seconds+11644473600L)/300*300*10000000
 val data=(t.toString()+Protocol.TOKEN).toByteArray(Charsets.US_ASCII);val d=sha(data)
 fun hex(b:ByteArray)=b.joinToString(""){"%02X".format(it)}
 return hex(cs(data,d))+"-"+hex(d)
}
}
