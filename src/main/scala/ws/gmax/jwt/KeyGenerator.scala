package ws.gmax.jwt

import java.security._

import com.google.common.base.Splitter
import org.bouncycastle.util.encoders.Base64

object Pem {

  val pair: KeyPair = generateKeyPair()

  def getPem(publicKey: Boolean = true): String = {

    def encodePem(key: Key, beg: String, end: String) = {
      val pem = new StringBuffer(beg)
      val base64encoded = Base64.toBase64String(key.getEncoded)
      Splitter.fixedLength(64).split(base64encoded).forEach(t => pem.append(s"$t\n"))
      pem.append(end)
      pem.toString
    }

    if (publicKey)
      encodePem(pair.getPublic, begPublic, endPublic)
    else
      encodePem(pair.getPrivate, begPrivate, endPrivate)
  }

//  def main(args: Array[String]): Unit = {
//    println(getPem(true))
//    println(getPem(false))
//  }
}
