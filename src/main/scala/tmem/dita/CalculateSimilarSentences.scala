package tmem.dita

import com.couchbase.client.java.document.JsonDocument
import com.couchbase.client.java.document.json.{JsonArray, JsonObject}
import com.couchbase.client.java.query.Query
import com.couchbase.spark._
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.linalg.distributed.{MatrixEntry, RowMatrix}
import org.apache.spark.{SparkContext, SparkConf}
import play.api.libs.json.Json
import tmem.dita.Tokenizer
// Without this, you can't use reduceByKey.
import org.apache.spark.SparkContext._

/**
 * Inspired by:
 * - http://databricks.gitbooks.io/databricks-spark-knowledge-base/content/best_practices/prefer_reducebykey_over_groupbykey.html
 * - https://chimpler.wordpress.com/2014/06/11/classifiying-documents-using-naive-bayes-on-apache-spark-mllib/
 * Created by koji on 3/19/15.
 */
object CalculateSimilarSentences extends App {

  val conf = new SparkConf().setMaster("local[*]").setAppName("dictionary-creator")

  // Couchbase Server connection settings.
  conf.set("com.couchbase.nodes", "vm.sherlock")
  conf.set("com.couchbase.bucket.spark", "")
  conf.set("com.couchbase.bucket.translation", "")

  // Start your spark context
  val sc = new SparkContext(conf)

  // provide implicit Json format.
  implicit val sentenceFmt = Json.format[Sentence]
  implicit val documentFmt = Json.format[Document]

  // If a term doesn't appear at least in n documents, the term is discarded.
  val minimumOccurrence : Int = 1;
  // Similarity less than this is discarded.
  val simThreshold : Double = 0.3
  // How many similar sentences to store.
  val numOfSimEntries : Int = 5;

  def toDocument(d: JsonDocument) : Document = {
    Json.fromJson[Document](Json.parse(d.content().toString)).get
  }

  var termDocsRdd  = sc.couchbaseQuery(
    // Query document ids.
    Query.simple("SELECT meta(translation).id FROM translation WHERE _type = 'document'"), "translation")
    .map(row => row.value.get("id").asInstanceOf[String])
    // Document class is not serializable, so can't go through RDD.
    // So let's use JsonDocument here.
    .couchbaseGet[JsonDocument]("translation")
    .flatMap(
      // Transform each Document
      d => toDocument(d)
        // Break down into each Sentence with global sentence id.
        .sentences.map{
           s =>
             new TermDoc(String.valueOf(s.id), Tokenizer.tokenize(s.txt("en")))
        }

    )
    .persist()



  termDocsRdd.foreach(s => println(s))

  // create dictionary term => id
  // and id => term
  val terms = termDocsRdd.flatMap(_.terms).distinct().collect().sortBy(identity)
  val termDict = new Dictionary(terms)

  // Debug dictionary.
  var dictTerms = termDict.indexToTerm
  //    for(key <- (dictTerms.keySet(): ImmutableSet[Int]).toArray) {
  println("# dict")
  for(key <- dictTerms.keySet().toArray) {
    println(key, termDict.valueOf(key.asInstanceOf[Int]))
  }

  // compute TFIDF and generate vectors
  // for IDF
  val numDocs = termDocsRdd.count()
  val idfs = (termDocsRdd.flatMap(termDoc => termDoc.terms.map((termDoc.doc, _))).distinct().groupBy(_._2) collect {
    // mapValues not implemented :-(
    // if term is present in less than n documents then remove it
    case (term, docs) if docs.size > minimumOccurrence =>
      term -> (numDocs.toDouble / docs.size.toDouble)
  }).collect.toMap

  // Debug idfs
  println("# IDFs")
  for (key <- idfs.keys) {
    println(key, idfs.get(key))
  }

  // Calculate each sentence's TFIDF
  val tfidfsRdd = termDocsRdd.map{termDoc => (termDoc.doc, termDict.tfIdfs(termDoc.terms.toSeq, idfs))}
    .persist()
  println("tfidfs", tfidfsRdd)
  tfidfsRdd.foreach{case (s, tfidf) => println(s, tfidf)}

  // This is used as a vector size, so it should be + 1, otherwise ArrayIndexOutOfBoundsException.
  val vectorSize = termDocsRdd.map(t => t.doc.toInt).max() + 1
  println(s"vectorSize=$vectorSize")

  // Convert it to Vectors
  val sentenceTfIdfsByTermIdRdd = tfidfsRdd.flatMap{
    case (sentenceId, tfidfs) => tfidfs.map{
      case (termId, tfidf) => (termId, (Integer.parseInt(sentenceId), tfidf))
    }
  }.groupByKey().map{
    case(termId, sentences) =>
      // Sort the sentence by sid, because Vectors.sparse requires that.
      // However, the order of each Vector is not important,
      // because the RowMatrix only calculate column similarity.
      val sortedSentences = sentences.toArray.sortBy(_._1)
      val sids = sortedSentences.map(_._1)
      val tfidfs = sortedSentences.map(_._2)
      Vectors.sparse(vectorSize, sids, tfidfs)
  }.persist()



  println("sentenceTfIdfsByTermIdRdd", sentenceTfIdfsByTermIdRdd)
  sentenceTfIdfsByTermIdRdd.foreach{entry => println(entry)}

  // Calculate similarities
  val sims = new RowMatrix(sentenceTfIdfsByTermIdRdd).columnSimilarities()
  println("sims", sims)
  for(entry <- sims.entries){
    if(entry.value > simThreshold) println(entry)
  }

  // Select top N similar sentences
  // TODO: There must be better way to do this...
  val topN = sims.entries.filter(m => m.value > simThreshold).flatMap{
    // TODO: Need to emit src : target, and also target : src link.
    case entry => Seq(
      (entry.i, (entry.j, entry.value)),
      (entry.j, (entry.i, entry.value)))
  }.groupByKey().map{case(srcSid, targets) => (srcSid, targets.toSeq.sortBy(_._2).takeRight(numOfSimEntries))}
  for(entry <- topN){
    println(entry)
  }

  // Store the similar pairs as JSON documents.
  topN.map{
    entry =>
      // Use sentence id so that it can be retrieved with KV access.
      val doc = JsonDocument.create("sim::" + entry._1, JsonObject.create())
      doc.content().put("_type", "sim")
      val sims = JsonArray.create()
      doc.content().put("sims", sims)
      entry._2.map{case (tid, sim) =>
        val s = JsonObject.create()
        s.put("id", String.valueOf(tid))
        s.put("sim", sim)
        sims.add(s)}
      doc
  }.saveToCouchbase("translation")


}
