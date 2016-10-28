package org.eiennohito.grpc.stream.impl

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}
import java.nio.ByteBuffer
import java.util.UUID

import akka.stream.Attributes.Attribute
import io.grpc.Metadata.Key
import io.grpc.{Metadata, Status, StatusException, StatusRuntimeException}

/**
  * @author eiennohito
  * @since 2016/10/28
  */
object ScalaMetadata {

  def get[T](meta: Metadata, key: Key[T]): Option[T] = {
    if (meta.containsKey(key)) {
      Some(meta.get(key))
    } else None
  }

  def forException(t: Throwable): Metadata = {
    val base = t match {
      case _: StatusException | _: StatusRuntimeException =>
        Status.trailersFromThrowable(t)
      case _ =>
        new Metadata()
    }
    base.put(ScalaException, t)
    base
  }


  def make(id: UUID): Metadata = {
    val obj = new Metadata()
    obj.put(ReqId, id)
    obj
  }

  case class InitialRequestId(id: UUID) extends Attribute

  val ReqId: Key[UUID] = Metadata.Key.of("reqid-bin", UUIDMarshaller)
  val ScalaException = Metadata.Key.of("jvm-exception-bin", ThrowableMarshaller)

  object UUIDMarshaller extends Metadata.BinaryMarshaller[UUID] {
    override def toBytes(value: UUID) = {
      val buf = ByteBuffer.allocate(16)
      buf.putLong(value.getMostSignificantBits)
      buf.putLong(value.getLeastSignificantBits)
      buf.array()
    }

    override def parseBytes(serialized: Array[Byte]) = {
      val buf = ByteBuffer.wrap(serialized)
      val mostSig = buf.getLong
      val leastSig = buf.getLong
      new UUID(mostSig, leastSig)
    }
  }

  object ThrowableMarshaller extends Metadata.BinaryMarshaller[Throwable] {
    override def toBytes(value: Throwable) = {
      val baos = new ByteArrayOutputStream(4096)
      val oos = new ObjectOutputStream(baos)
      oos.writeObject(value)
      oos.flush()
      baos.toByteArray
    }

    override def parseBytes(serialized: Array[Byte]) = {
      val bais = new ByteArrayInputStream(serialized)
      val ois = new ObjectInputStream(bais)
      ois.readObject().asInstanceOf[Throwable]
    }
  }
}
