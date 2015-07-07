package com.cleawing.docker

import org.scalatest.{BeforeAndAfterEach, FeatureSpec, GivenWhenThen, ShouldMatchers}
import scala.sys.process._

class TLSSupportSpec extends FeatureSpec with ShouldMatchers with GivenWhenThen with BeforeAndAfterEach {

  override def beforeEach(): Unit = {
    // Ensure that we do not have tmp mock files
    Seq("rm", "-f", "/tmp/key.pem").!
    Seq("rm", "-f", "/tmp/cert.pem").!
  }

  feature("Path verification") {
    scenario("Empty path") {
      Given("''")
      When("thrown by IllegalArgumentException")
      the [IllegalArgumentException] thrownBy TLS("") should have message "Path '' is not exist"
      info("With Path '' is not exist")
    }

    scenario("Missed path") {
      Given("/missed")
      When("thrown by IllegalArgumentException")
      the [IllegalArgumentException] thrownBy TLS("/missed") should have message "Path '/missed' is not exist"
      info("With Path '/missed' is not exist")
    }

    scenario("Path to file") {
      Given("/etc/hosts")
      When("thrown by IllegalArgumentException")
      the [IllegalArgumentException] thrownBy TLS("/etc/hosts") should have message "Path '/etc/hosts' is not a directory"
      info("With Path '/etc/hosts' is not a directory")
    }

    scenario("Path without certs") {
      Given("/tmp")
      When("thrown by IllegalArgumentException")
      the [IllegalArgumentException] thrownBy TLS("/tmp") should have message "Path '/tmp' does not contain any certs"
      info("With Path '/tmp' does not contain any certs")
    }

    scenario("Path only with key.pm") {
      Seq("touch", "/tmp/key.pem").! shouldBe 0
      Given("/tmp")
      When("thrown by IllegalArgumentException")
      the [IllegalArgumentException] thrownBy TLS("/tmp") should have message "Path '/tmp/cert.pem' is not a file"
      info("With Path '/tmp/cert.pem' is not a file")
      Seq("rm", "/tmp/key.pem").! shouldBe 0
    }

    scenario("Path only with cert.pm") {
      Seq("touch",  "/tmp/cert.pem").! shouldBe 0
      Given("/tmp")
      When("thrown by IllegalArgumentException")
      the [IllegalArgumentException] thrownBy TLS("/tmp") should have message "Path '/tmp/key.pem' is not a file"
      info("With Path '/tmp/key.pem' is not a file")
      Seq("rm", "/tmp/cert.pem").! shouldBe 0
    }

    scenario("Directly use empty cert paths") {
      Given("'', '', None")
      When("thrown by IllegalArgumentException")
      the [IllegalArgumentException] thrownBy TLS("", "", None) should have message "Path '' is not readable or not a file"
      info("With Path '' is not readable")
    }

    scenario("Provide a not readable caPath") {
      Seq("touch", "/tmp/key.pem").! shouldBe 0
      Seq("touch", "/tmp/cert.pem").! shouldBe 0
      Given("'/tmp/key.pem', '/tmp/cert.pem', Some('/tmp/ca.pem')")
      When("thrown by IllegalArgumentException")
      the [IllegalArgumentException] thrownBy TLS("/tmp/key.pem", "/tmp/cert.pem", Some("/tmp/ca.pem")) should have message "Path '/tmp/ca.pem' is not readable or not a file"
      info("Path '/tmp/ca.pem' is not readable or not a file")
    }

    // FIXME
    ignore("Provide exist, but not readable paths to certs") {

    }
  }

  feature("TLS creation") {
    scenario("Path from config") {
      Given(s"'${config.getString("docker.cert_path")}'")
      And("No exception")
      noException should be thrownBy TLS()
    }
  }

}
