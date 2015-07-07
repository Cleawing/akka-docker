package com.cleawing.docker

import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.cleawing.docker.api.Data
import org.scalatest.concurrent.ScalaFutures
import org.scalatest._
import org.scalatest.time.{Seconds, Span, Millis}

class PingerSpec(_system: ActorSystem) extends TestKit(_system)
  with FeatureSpecLike with GivenWhenThen with ShouldMatchers
  with ScalaFutures with EitherValues with BeforeAndAfterAll {

  def this() = this(ActorSystem("ApiSpec"))

  implicit val defaultPatience = PatienceConfig(timeout = Span(1, Seconds), interval = Span(20, Millis))

  val api = Remote()

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  feature("Pinger") {
    scenario("Success") {
      Given("API connection from config")
      When("ping()")
      whenReady(api.ping()) { res =>
        Then("Data.Pong(OK)")
        res.right.value shouldBe a [Data.Pong]
        res.right.value.msg shouldBe "OK"
      }
    }

    scenario("Connection failed") {
      Given("API connection with missed host and port")
      val missedApi = Remote("127.0.0.1", 22375)
      When("ping()")
      Then("Data.ConnectionFailed")
      whenReady(missedApi.ping()) { _.left.value shouldBe a [Data.ConnectionFailed] }
    }

    scenario("Pickup TLS-port without tls = on") {
      Given("API connection with TLS-port")
      val missedApi = Remote(2376)
      When("ping()")
      Then("Data.ConnectionFailed")
      whenReady(missedApi.ping()) { _.left.value shouldBe a [Data.ConnectionFailed] }
    }

    scenario("Establish TLS-connection") {
      Given("API connection with tls = on")
      val securedApi = Remote(tls_on = true)
      When("ping()")
      whenReady(securedApi.ping()) { res =>
        Then("Data.Pong(OK)")
        res.right.value shouldBe a [Data.Pong]
        res.right.value.msg shouldBe "OK"
      }
    }
  }
}
