package tmem.dita

// first import the implicit execution context

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import org.reactivecouchbase.ReactiveCouchbaseDriver
import play.api.libs.json.{Json, JsObject}

/**
 * Created by koji on 3/17/15.
 */

case class Sample(click: String, memo: String)

object CouchbaseConnectionTest extends App {



  // get a driver instance driver
  val driver = ReactiveCouchbaseDriver()

  // get the bucket
  val bucket = driver.bucket("translation")

  // provide implicit Json format.
  implicit val sampleFmt = Json.format[Sample]
  implicit val sentenceFmt = Json.format[Sentence]
//  implicit val sentencesFmt = Json.format[mutable.MutableList[Sentence]]
  implicit val documentFmt = Json.format[Document]

  // do something here with the default bucket
  val doc = bucket.get[Sample]("sample").map { opt =>
    println(opt.map(sample => s"Found John : ${sample}").getOrElse("Cannot find object with key 'john-doe'"))
  }

  println("doc=", doc)

  val document = DitaParser.parse("/Users/koji/dev/Couchbase/projects/docs-ja/en/learn/admin/REST/rest-user-create.dita")

  bucket.set[Document]("document", document).onSuccess{
    case status => println(s"Operation status : ${status.getMessage}")
  }


  // shutdown the driver (only at app shutdown)
  // driver.shutdown()
}
