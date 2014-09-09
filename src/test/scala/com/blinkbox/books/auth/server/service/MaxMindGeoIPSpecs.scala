package com.blinkbox.books.auth.server.service

import com.blinkbox.books.auth.server.services.MaxMindGeoIP
import org.scalatest.{Matchers, FlatSpec}
import spray.http.RemoteAddress

class MaxMindGeoIPSpecs extends FlatSpec with Matchers {

  val geoIp = new MaxMindGeoIP()

  "The MaxMind GeoIP service" should "identify as US the 8.8.8.8 server" in {
    geoIp.countryCode(RemoteAddress("8.8.8.8")) should equal(Some("US"))
  }

  it should "not identify a local address" in {
    geoIp.countryCode(RemoteAddress("127.0.0.1")) shouldBe empty
  }
}
