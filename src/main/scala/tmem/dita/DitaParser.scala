package tmem.dita
import java.util

import scala.collection.mutable
import scala.xml.pull.{EvElemEnd, EvElemStart, EvText, XMLEventReader}
import scala.io.Source

/**
 * Created by koji on 2/27/15.
 */
object DitaParser {

//  def main(args: Array[String]) = parse("/Users/koji/dev/Couchbase/projects/docs-ja/en/learn/admin/REST/rest-user-getname.dita")
  def main(args: Array[String]) = {
    val doc = parse("/Users/koji/dev/Couchbase/projects/docs-ja/en/learn/admin/REST/rest-user-create.dita")
//    val termDoc = Tokenizer.tokenize(doc)
//    for(token <- termDoc.terms){
//      println(token)
//    }
  }

  def parseAll(xmlFiles: Iterable[String]) = xmlFiles map parse

  def parse(ditaXmlFile: String) : Document = {
    println("Parsing " + ditaXmlFile)
    val xml = new XMLEventReader(Source.fromFile(ditaXmlFile))
    var insideCodeblock = false
    var inlineCount = 0
    var texts = mutable.MutableList[Sentence]()
    var pos = 0
    val doc = Document(ditaXmlFile, texts)
    for(event <- xml){
      event match {
        case EvElemStart(_,"codeblock",_,_) => insideCodeblock = true
        case EvElemEnd(_,"codeblock") => insideCodeblock = false
        case EvElemStart(_,"codeph",_,_) => inlineCount = 2
        case EvElemEnd(_,"codeph") => inlineCount = 1
        case EvText(text) =>
          if(!insideCodeblock) {
            var t = text.trim()
            if(t.nonEmpty){
              t = t.replaceAll("\\s+", " ")
              if(inlineCount == 0){
                val ts = t.split("\\. ")
                for(ti <- ts){
                  texts += new Sentence(pos, ti)
                }
              } else {
                if(inlineCount == 2) t = " <codeph>" + t + "</codeph> "
                texts.get(texts.size - 1).get.concat(t)
                inlineCount -= 1;

                if(inlineCount == 0){
                  // Split the concatenated string.
                  t = texts.last.sentence
                  texts = texts.dropRight(1)
                  val ts = t.split("\\. ")
                  for(ti <- ts){
                    texts += new Sentence(pos, ti)
                  }
                }
              }
              pos += 1
            }
          }
        case _ =>
      }
    }
    for(t <- texts){
      println(t)

//      val tokens = Tokenizer.tokenize(t)
//      t.tokens = tokens
//      for(token <- tokens){
//        println(token)
//      }
    }
    doc
  }


  case class Document(docId: String, body: mutable.Iterable[Sentence], labels: Set[String] = Set.empty)

}
