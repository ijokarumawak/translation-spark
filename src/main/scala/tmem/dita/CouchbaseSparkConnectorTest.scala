package tmem.dita

import java.util.UUID

import com.couchbase.client.java.document.{JsonArrayDocument, JsonDocument}
import com.couchbase.client.java.document.json.{JsonArray, JsonObject}
import com.couchbase.spark._
import org.apache.spark.{SparkContext, SparkConf}
import play.api.libs.json.Json

/**
 * Created by koji on 3/19/15.
 */
object CouchbaseSparkConnectorTest {

  def main(args: Array[String]) {

    val conf = new SparkConf().setMaster("local[*]").setAppName("myapp")

    //
    conf.set("com.couchbase.nodes", "vm.sherlock")
    conf.set("com.couchbase.bucket.spark", "")
    conf.set("com.couchbase.bucket.translation", "")

    // Start your spark context
    val sc = new SparkContext(conf)

    val doc1 = JsonDocument.create("doc1", JsonObject.create().put("some", "content"))
    val doc2 = JsonArrayDocument.create("doc2", JsonArray.from("more", "content", "in", "here"))

    var data = sc.parallelize(Seq(doc1, doc2)).saveToCouchbase("spark")

    // data = sc.parallelize(Seq("document")).couchbaseGet[JsonDocument]("translation").foreach(d => println(d))
    // Copy document into different bucket.
    // data = sc.parallelize(Seq("document")).couchbaseGet[JsonDocument]("translation").saveToCouchbase("spark")

    // Copy document into different document.
//    sc.parallelize(Seq("document")).couchbaseGet[JsonDocument]("translation").map(
//      d => JsonDocument.create(UUID.randomUUID().toString, d.content()))
//    .saveToCouchbase("translation")

    // provide implicit Json format.
    implicit val sentenceFmt = Json.format[Sentence]
    implicit val documentFmt = Json.format[Document]


    // Document class is not serializable, so can't go through RDD.
    var docs : Array[JsonDocument] = sc.parallelize(Seq("77260c62-b1d3-46c6-99f4-ec2ec7cc6428"))
      .couchbaseGet[JsonDocument]("translation")
      .collect()

    // But Document can be de-serialized locally.
    docs.map(d => Json.fromJson[Document](Json.parse(d.content().toString)).get).foreach(d => println(d))


  }

}
