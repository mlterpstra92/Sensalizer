import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

import play.api.test._
import play.api.test.Helpers._

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends Specification {

  "Application" should {

    "send 404 on a bad request" in new WithApplication{
      route(FakeRequest(GET, "/boum")) must beNone
    }

    "render the index page" in new WithApplication{
      val home = route(FakeRequest(GET, "/")).get

      status(home) must equalTo(OK)
      contentType(home) must beSome.which(_ == "text/html")
      contentAsString(home) must contain ("Sensalizer")
    }

    "render profile page" in new WithApplication{
      val profile = route(FakeRequest(GET, "/profile/0")).get

      status(profile) must equalTo(OK)
      contentType(profile) must beSome.which(_ == "text/html")
      contentAsString(profile) must contain ("ChaM9GovFEW6Q2dFuQTAVucAl4bJvJKsQNVS3pars6QDVirs")

    }

    "render feed overview" in new WithApplication{
      val profile = route(FakeRequest(GET, "/feeds/0")).get

      status(profile) must equalTo(OK)
      contentType(profile) must beSome.which(_ == "text/html")
      contentAsString(profile) must contain ("Feed Management")
    }

    "render feed" in new WithApplication{
      val profile = route(FakeRequest(GET, "/feed/923879237")).get

      status(profile) must equalTo(OK)
      contentType(profile) must beSome.which(_ == "text/plain")
      contentAsString(profile) must contain ("Feed 923879237:")
    }
  }
}
