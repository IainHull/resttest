# RestTest

A lightweight Scala DSL for system testing REST web services

## Example

```scala
val Jason: Person = ???
val personJson = Json.stringify(Jason)
val EmptyList = List[Person]()

using(_ url "http://api.rest.org/person") { implicit rb =>
  GET asserting (statusCode is Status.OK, jsonBodyAsList[Person] is EmptyList)
  val id = POST body personJson asserting (statusCode is Status.Created) returning (header("X-Person-Id"))
  GET / id asserting (statusCode is Status.OK, jsonBodyAs[Person] is Jason)
  GET asserting (statusCode is Status.OK, jsonBodyAsList[Person] is Seq(Jason))
  DELETE / id asserting (statusCode is Status.OK)
  GET / id asserting (statusCode is Status.NotFound)
  GET asserting (statusCode is Status.OK, jsonBodyAsList[Person] is EmptyList)
}
```

## The Plan

I plan for this to be a useful resource for writing REST web service system tests.  However my initial focus is learning and documenting the creation of a Scala DSL.  The progress on the implementation is slow because I am documenting my understanding of DSLs as I go.

You can follow the [progress on my blog](http://iainhull.github.io/tags.html#resttest-ref):

* [The Builder Pattern](http://iainhull.github.io/2013/07/01/a-simple-rest-dsl-part-1/)
* [The Builder as the basis for a DSL](http://iainhull.github.io/2013/07/02/a-simple-rest-dsl-part-2/)
* [Extracting and asserting on response values](http://iainhull.github.io/2013/07/14/a-simple-rest-dsl-part-3/)
* Integrating RestTest with ScalaTest (planned)
* How to document a DLS (planned)
* How to structure DLS projects (planned)
* Summary of Scala techniques and resources for creating DSLs (planned)

## How to build

This project is built with [gradle](http://www.gradle.org/).  It includes the gradle wrapper which will download gradle and build the project for you (the only prereq is Java).

To download and build RestTest just:

```
git clone git@github.com:IainHull/resttest.git
cd resttest
./gradlew build
```

To create a fully configured eclipse project just:

```
./gradlew eclipse
```

## License

RestTest is licensed under the permissive [Apache 2 Open Source License](http://www.apache.org/licenses/LICENSE-2.0.txt).
