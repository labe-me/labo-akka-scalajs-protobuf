package me.labe.labo
import java.nio.ByteBuffer
import protocol._
import scalajs.js.annotation.JSExport
import scalajs.js
import org.scalajs.dom
import dom.ext.Ajax

@JSExport object Client {

  @JSExport def main() = {
    println("OK")
    sendHello()
    sendPerson()
  }

  def sendHello() = {
    val p = Hello()
    val data = Ajax.InputData.byteBuffer2ajax(ByteBuffer.wrap(p.toByteArray))
    Ajax.post("/hello", data)
  }

  def sendPerson() = {
    val p = new Person(
      id = 1,
      name = "Someone",
      email = Some("someone@foo.com"),
      cdate = System.currentTimeMillis
    )
    val data = Ajax.InputData.byteBuffer2ajax(ByteBuffer.wrap(p.toByteArray))
    Ajax.post("/person", data)
  }
}
