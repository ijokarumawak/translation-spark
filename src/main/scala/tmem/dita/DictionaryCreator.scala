package tmem.dita

import com.couchbase.client.java.document.JsonDocument
import com.couchbase.client.java.document.json.JsonObject
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
 * Created by koji on 3/19/15.
 */
object DictionaryCreator extends App {

  val conf = new SparkConf().setMaster("local[*]").setAppName("dictionary-creator")

  //
  conf.set("com.couchbase.nodes", "vm.sherlock")
  conf.set("com.couchbase.bucket.spark", "")
  conf.set("com.couchbase.bucket.translation", "")

  // Start your spark context
  val sc = new SparkContext(conf)

  // provide implicit Json format.
  implicit val sentenceFmt = Json.format[Sentence]
  implicit val documentFmt = Json.format[Document]

  // Similarity less than this is discarded.
  val simThreshold : Double = 0.7
  // How many similar sentences to store.
  val numOfSimEntries : Int = 3;

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
        // Break down into each Sentence with index in the array
        /*
        .sentences.zipWithIndex.map{
          // Tokenize each sentence
          case(s, i) => (d.id() + "-" + i, Tokenizer.tokenize(s.txt("en")))
        }
        */
        // Use global Id index instead of array index.
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
    // if term is present in less than 2 documents then remove it
    case (term, docs) if docs.size > 2 =>
      term -> (numDocs.toDouble / docs.size.toDouble)
  }).collect.toMap

  // Debug idfs
  println("# IDFs")
  for (key <- idfs.keys) {
    println(key, idfs.get(key))
  }

  // Calculate each sentence's TFIDF
  val tfidfsRdd = termDocsRdd.map{termDoc => (termDoc.doc, termDict.tfIdfs(termDoc.terms.toSeq, idfs))}.persist()
  println("tfidfs", tfidfsRdd)
  tfidfsRdd.foreach{case (s, tfidf) => println(s, tfidf)}

  // Convert it to Vectors
  // http://databricks.gitbooks.io/databricks-spark-knowledge-base/content/best_practices/prefer_reducebykey_over_groupbykey.html
  val dictSize = dictTerms.size()
  val sentenceTfIdfsByTermIdRdd = tfidfsRdd.flatMap{
    case (s, tfidfs) => tfidfs.map{
      case (termId, tfidf) => (termId, (Integer.parseInt(s), tfidf))
    }
  }.groupByKey().map{
    case(termId, sentences) =>
      // Sort the sentence by sid, because Vectors.sparse requires that.
      // However, the order ov each Vector is not important,
      // because the RowMatrix only calculate column similarity.
      val sortedSentences = sentences.toArray.sortBy(_._1)
      val sids = sortedSentences.map(_._1)
      val tfidfs = sortedSentences.map(_._2)
      Vectors.sparse(dictSize, sids, tfidfs)
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
//  implicit val sortBySim = new Ordering[MatrixEntry] {
//    override def compare(a: MatrixEntry, b: MatrixEntry) = a.value.compare(b.value)
//  }
//  implicit val sortBySim = new Ordering[(Long, Double)] {
//    override def compare(a: (Long, Double), b: (Long, Double)) = a._2.compare(b._2)
//  }
  // TODO: There must be better way to do this...
  val topN = sims.entries.filter(m => m.value > simThreshold).map{
    case entry => (entry.i, (entry.j, entry.value))
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
      entry._2.map{case (tid, sim) => doc.content().put(String.valueOf(tid), sim)}
      doc
  }.saveToCouchbase("translation")


}
