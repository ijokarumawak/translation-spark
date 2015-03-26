package tmem.dita

import java.util.UUID

import org.reactivecouchbase.client.OpResult

import scala.concurrent.ExecutionContext.Implicits.global
import org.reactivecouchbase.ReactiveCouchbaseDriver
import play.api.libs.json.Json

import scala.util.{Failure, Success, Try}

/**
 * Created by koji on 3/19/15.
 */
object DocLoader extends App {

  // get a driver instance driver
  val driver = ReactiveCouchbaseDriver()

  // get the bucket
  val bucket = driver.bucket("translation")

  bucket.add("sentenceId", 0).collect{case status => println(s"sentenceId status : ${status.getMessage}")}

  // provide implicit Json format.
  implicit val sentenceFmt = Json.format[Sentence]
  implicit val documentFmt = Json.format[Document]

  // Parse document.
//  val document = DitaParser.parse("/Users/koji/dev/Couchbase/projects/docs-ja/en/learn/admin/REST/rest-user-create.dita")
//  val document = DitaParser.parse("/Users/koji/dev/Couchbase/projects/docs-ja/en/learn/admin/Install/linux-startup-shutdown.dita")
  val document = DitaParser.parse("/Users/koji/dev/Couchbase/projects/docs-ja/en/learn/admin/REST/rest-user-getname.dita")


  // Save as a brand new document.
  // TODO: check MD5 and insert only if it's new
  // select meta(translation).id from translation where uri = "???" order by updatedAt desc;
  val docId : String = UUID.randomUUID().toString

  // Publish global sequence id for each sentence.
  for(s <- document.sentences){
    bucket.incr("sentenceId", 1).onSuccess{
      case i => i.msg match {
        case Some(x) => s.id = x.toInt
      }
    }
  }

  bucket.set[Document](docId, document).onSuccess {
    case status => println(s"Operation status : ${status.getMessage}")

    // shutdown the driver (only at app shutdown)
    driver.shutdown()
  }





}
