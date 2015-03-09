package tmem.dita

import com.google.common.collect.ImmutableSet
import org.apache.spark.SparkContext
import scala.collection.mutable

/**
 * Created by koji on 2/27/15.
 */
object VectorizeTest {
  def main(args: Array[String]) = {

    // Create SparkContext
    val sc = new SparkContext("local[4]", "test")


    // Generate test data.
    var s1 = new Sentence(0, "foo bar baz")
    var s2 = new Sentence(1, "foo bar baz")
    var s3 = new Sentence(2, "foo bar baz")
    var s4 = new Sentence(3, "foo bar baz")
    var s5 = new Sentence(4, "foo bari baz")
    var sentences = mutable.ArrayBuffer.empty[Sentence]
    sentences += s1
    sentences += s2
    sentences += s3
    sentences += s4
    sentences += s5

    // Create term dictionary
    var termDocs = Tokenizer.tokenizeAll(sentences)

    for(termDoc <- termDocs) println(termDoc)

    // put collections in Spark
    val termDocsRdd = sc.parallelize[TermDoc](termDocs.toSeq)

    val numDocs = termDocs.size

    // create dictionary term => id
    // and id => term
    val terms = termDocsRdd.flatMap(_.terms).distinct().collect().sortBy(identity)
    val termDict = new Dictionary(terms)

    // Debug dictionary.
    var dictTerms = termDict.indexToTerm
    //    for(key <- (dictTerms.keySet(): ImmutableSet[Int]).toArray) {
    for(key <- dictTerms.keySet().toArray) {
      println(key)
      println(termDict.valueOf(key.asInstanceOf[Int]))
    }

    // compute TFIDF and generate vectors
    // for IDF
    val idfs = (termDocsRdd.flatMap(termDoc => termDoc.terms.map((termDoc.doc, _))).distinct().groupBy(_._2) collect {
      // mapValues not implemented :-(
      // if term is present in less than 3 documents then remove it
      case (term, docs) if docs.size > 3 =>
        term -> (numDocs.toDouble / docs.size.toDouble)
    }).collect.toMap

    // Debug idfs
    for (key <- idfs.keys) {
      println(key)
      println(idfs.get(key))
    }

    // termDict.tfIdfs()
    val tfidfs = termDict.tfIdfs(terms, idfs)

    // Debug tf*idfs
    // ArrayBuffer((0,0.41666666666666663), (2,0.3333333333333333), (3,0.3333333333333333))
    // Term(0) = 0.4166, Term(2) = 0.3333, Term(3) = 0.3333
    // Term(1) is discarded
    println(tfidfs)

    // Create some vector
    val vector = termDict.vectorize(tfidfs)
    println(vector)

    // Test input
    // 0: bar
    // 1: bari
    // 2: baz
    // 3: foo

    val si = new Sentence(10, "foo bari bar bar")
    val siTerms = Tokenizer.tokenize(si.sentence)
    val siTfIdfs = termDict.tfIdfs(siTerms, idfs)

    val siVector = termDict.vectorize(siTfIdfs)
    println(siVector)


  }
}
