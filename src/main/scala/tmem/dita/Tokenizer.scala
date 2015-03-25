package tmem.dita


import java.io.StringReader
import org.apache.lucene.analysis.en.EnglishAnalyzer
import org.apache.lucene.util.Version
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import scala.collection.mutable

object Tokenizer {
  val LuceneVersion = Version.LUCENE_48



  def tokenizeAll(sentences : Iterable[Sentence]) : Iterable[TermDoc] = sentences map tokenize
  // def tokenizeAll(sentences : Iterable[Sentence]) : Iterable[String] = sentences flatMap tokenize

  def tokenize(sentence: Sentence): TermDoc = TermDoc(String.valueOf(sentence.pos), tokenize(sentence.txt("en")))

  def tokenize(content: String): Seq[String] = {
    val tReader = new StringReader(content)
    val analyzer = new EnglishAnalyzer(LuceneVersion)
    val tStream = analyzer.tokenStream("contents", tReader)
    val term = tStream.addAttribute(classOf[CharTermAttribute])
    tStream.reset()

    val result = mutable.ArrayBuffer.empty[String]
    while(tStream.incrementToken()) {
      val termValue = term.toString
      if (!(termValue matches ".*[\\d\\.].*")) {
        result += term.toString
      }
    }
    result
  }
}

case class TermDoc(doc: String, terms: Iterable[String])