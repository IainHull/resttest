package org.iainhull.resttest

/**
 * The test driver defines the [[Api.HttpClient]] and initial [[Api.RequestBuilder]]
 * used to execute Rest Tests.
 * 
 * Users mix-in their preferred TestDriver implementation, to execute their tests.
 * 
 * {{{
 * class PersonSpec extends FlatSpec {
 *   this: TestDriver =>
 *
 *   val EmptyList = Seq[Person]()
 *  
 *   "/person (collection)" should "be empty" in {    
 *     GET / "person" asserting (StatusCode === Status.OK, BodyAsPersonList === EmptyList)
 *   }
 * }
 *
 * @RunWith(classOf[JUnitRunner])
 * class PersonUnitSpec extends PersonSpec with SprayUnitTestDriver with MyService
 * 
 * @RunWith(classOf[JUnitRunner])
 * class PersonSystemSpec extends PersonSpec with JerseySystemTestDriver {
 *   override val baseUrl = "http://localhost:9000"
 * }
 * }}}
 * 
 * Subclasses implement the interface supplying a httpClient to execute the 
 * Requests and a defBuilder which provides the base configuration for all 
 * tests.  All tests should support relative paths, this enables the same test
 * code to be executed as a unit test and a system test.  To support this
 * the defBuilder for system test drivers should supply the baseUrl some how.   
 */
trait TestDriver {
  import Api._
  
  /**
   * The httpClient to execute Requests
   */
  implicit def httpClient: HttpClient
  
  /**
   * The default RequestBuilder, common to all tests
   */
  implicit def defBuilder: RequestBuilder
}