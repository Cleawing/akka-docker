package com.cleawing.docker

import akka.actor.ActorSystem
import akka.http.scaladsl.model.IllegalResponseException
import akka.http.scaladsl.model.StatusCodes._
import akka.stream.StreamTcpException
import akka.testkit.TestKit
import com.cleawing.docker.api.Data
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}

class RemoteSpec(_system: ActorSystem) extends TestKit(_system)
  with FunSpecLike with ShouldMatchers with ScalaFutures
  with EitherValues with BeforeAndAfterAll {

  def this() = this(ActorSystem("ApiSpec"))

  implicit val defaultPatience = PatienceConfig(timeout = Span(2, Seconds), interval = Span(20, Millis))

  val api = Remote()

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  describe("Misc") {
    it("should return Data.Version") {
      whenReady(api.version()) { _.right.value shouldBe a [Data.Version]}
    }

    it("should return Data.Info") {
      whenReady(api.info()) {_.right.value shouldBe a [Data.Info]}
    }
  }

  describe("Images") {
    it("should return Data.Images") {
      whenReady(api.images()) {_.right.value shouldBe a [Data.Images]}
    }

    ignore("should return Data.ImageHistory") {
      whenReady(api.images()) {_.right.value shouldBe a [Data.ImageHistory]}
    }
  }

  describe("Containers") {
    it("should return Data.Containers") {
      whenReady(api.containers()) {_.right.value shouldBe a [Data.Containers]}
    }
  }

}
