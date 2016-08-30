package me.labe.labo

// Source:
// https://gist.github.com/ahjohannessen/ce43cf45607b9dd9050b

import java.lang.reflect.Method
import java.util.concurrent.atomic.AtomicReference
import akka.actor.ExtendedActorSystem
import akka.serialization.Serializer
import com.trueaccord.scalapb.GeneratedMessage
import scala.annotation.tailrec

/**
  * This Serializer serializes `com.trueaccord.scalapb.GeneratedMessage`s
  */

class ProtobufSerializer(val system: ExtendedActorSystem) extends Serializer {

  import ProtobufSerializer._

  private val parsingMethodBindingRef = new AtomicReference[Map[Class[_], Method]](Map.empty)

  def identifier      = 200
  def includeManifest = true

  def fromBinary(bytes: Array[Byte], clazz: Option[Class[_]]): AnyRef = clazz match {
    case Some(clz) ⇒

      @tailrec
      def parsingMethod(method: Method = null): Method = {
        val parsingMethodBinding = parsingMethodBindingRef.get()
        parsingMethodBinding.get(clz) match {
          case Some(cachedParsingMethod) ⇒ cachedParsingMethod
          case None ⇒
            val unCachedParsingMethod =
              if (method eq null) clz.getDeclaredMethod("parseFrom", ARRAY_OF_BYTE_ARRAY: _*)
              else method
            if (parsingMethodBindingRef.compareAndSet(parsingMethodBinding, parsingMethodBinding.updated(clz, unCachedParsingMethod)))
              unCachedParsingMethod
            else
              parsingMethod(unCachedParsingMethod)
        }
      }

      parsingMethod().invoke(null, bytes).asInstanceOf[GeneratedMessage]

    case None ⇒ throw new IllegalArgumentException(
      "Need a protobuf message class to be able to serialize bytes using protobuf"
    )
  }


  def toBinary(obj: AnyRef): Array[Byte] = obj match {
    case message: GeneratedMessage ⇒ message.toByteArray
    case _ ⇒ throw new IllegalArgumentException(
      s"Can't serialize a non-protobuf message using protobuf [$obj]"
    )
  }

}

object ProtobufSerializer {
  private val ARRAY_OF_BYTE_ARRAY = Array[Class[_]](classOf[Array[Byte]])
}
