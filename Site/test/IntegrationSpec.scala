import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

import play.api.test._
//import com.websudos.phantom.

/**
 * add your integration spec here.
 * An integration test will fire up a whole play application in a real (or headless) browser
 */

@RunWith(classOf[JUnitRunner])
class IntegrationSpec extends Specification {

  "Application" should {

    "work from within a browser" in new WithBrowser {

      browser.goTo("http://localhost:9000")
      println(browser.pageSource())
      browser.pageSource must contain("html")
    }



  }
}
