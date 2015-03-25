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
    var pos = 0
    val doc = new Document(ditaXmlFile, ditaXmlFile)
    val sentences : mutable.ListBuffer[Sentence] = mutable.ListBuffer.empty[Sentence]
//    var texts : collection.mutable.MutableList[Sentence] = doc.sentences
//    var texts = mutable.MutableList[Sentence]()

    for(event <- xml){
      event match {
        case EvElemStart(_,"codeblock",_,_) => insideCodeblock = true
        case EvElemEnd(_,"codeblock") => insideCodeblock = false
        case EvElemStart(_,"codeph",_,_) => inlineCount = 2
        case EvElemEnd(_,"codeph") => inlineCount = 1
        case EvElemStart(_,"filepath",_,_) => inlineCount = 2
        case EvElemEnd(_,"filepath") => inlineCount = 1
        case EvText(text) =>
          if(!insideCodeblock) {
            var t = text.trim()
            if(t.nonEmpty){
              t = t.replaceAll("\\s+", " ")
              if(inlineCount == 0){
                val ts = t.split("\\. ")
                for(ti <- ts){
                  val s = new Sentence(pos)
                  s.txt += ("en" -> ti)
                  sentences += s
                }
              } else {
                if(inlineCount == 2) t = " <codeph>" + t + "</codeph> "
                // sentences.get(sentences.size - 1).get.concat("en", t)
                sentences.last.concat("en", t)
                inlineCount -= 1;

                if(inlineCount == 0){
                  // Split the concatenated string.
                  t = sentences.last.txt.get("en").get
//                  texts = texts.dropRight(1)
//                  sentences.dropRight(1)
                  sentences.remove(sentences.size - 1)
                  val ts = t.split("\\. ")
                  for(ti <- ts){
                    val s = new Sentence(pos)
                    s.txt += ("en" -> ti)
                    sentences += s
                  }
                }
              }
              pos += 1
            }
          }
        case _ =>
      }
    }
    doc.sentences = sentences.toList
    for(t <- doc.sentences){
      println(t)

//      val tokens = Tokenizer.tokenize(t)
//      t.tokens = tokens
//      for(token <- tokens){
//        println(token)
//      }
    }
    // println(doc)
    doc
  }


}
