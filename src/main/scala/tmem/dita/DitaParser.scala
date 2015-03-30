package tmem.dita
import java.util

import scala.StringBuilder
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.xml.MetaData
import scala.xml.pull.{EvElemEnd, EvElemStart, EvText, XMLEventReader}
import scala.io.Source

/**
 * Created by koji on 2/27/15.
 */
object DitaParser {

  def main(args: Array[String]) = {
    val doc = parse("/Users/koji/dev/Couchbase/projects/docs-ja/ja/learn/admin/Install/hostnames.dita")
  }

  def parseAll(xmlFiles: Iterable[String]) = xmlFiles map parse

  def breakSentences(text : String) : Seq[String] = {
    val ts = text.split("\\. ")
    val texts = ListBuffer[String]()
    for((ti, i) <- ts.zipWithIndex){
      // Re-add the period.
      if(i != ts.size - 1) {
        texts += (ti + ".")
      } else {
        texts += ti
      }
    }
    texts
  }

  def addSentences(text : String, pos : Int, container : mutable.ListBuffer[Sentence]) : Unit = {
    breakSentences(text).foreach{t =>
      val s = new Sentence(pos)
      s.txt += ("en" -> t)
      container += s
    }
  }

  def addSpaceAndText(inlineBuff : StringBuilder, text : String) : Unit = {
    // Adding space if it doesn't have.
    if(!inlineBuff.endsWith(" ")) inlineBuff ++= " "
    inlineBuff ++= text
  }

  def parse(ditaXmlFile: String) : Document = {
    println("Parsing " + ditaXmlFile)
    val xml = new XMLEventReader(Source.fromFile(ditaXmlFile))
    var pos = 0
    val doc = new Document(ditaXmlFile, ditaXmlFile)
    val sentences : mutable.ListBuffer[Sentence] = mutable.ListBuffer.empty[Sentence]


    var ignoring = false
    val inlineTags = """^(uicontrol|wintitle|xref|filepath|codeph|menucascade)$""".r
    val ignoreTags = """^(codeblock)$""".r
    val inlineStack = mutable.Stack[EvElemStart]()
    val inlineBuff = new StringBuilder()
    var previousWasInline = false;
    for(event <- xml){
      event match {
        case EvElemStart(_,ignoreTags(tagName),_,_) => ignoring = true
        case EvElemEnd(_,ignoreTags(tagName)) => ignoring = false
        case EvElemStart(pre, inlineTags(tagName), attrs, scope) => {

          if(inlineStack.isEmpty) {
            // This is the starting element, initiate buffer.
            inlineBuff.clear()
          }
          addSpaceAndText(inlineBuff, s"<$tagName$attrs>")
          inlineStack.push(event.asInstanceOf[EvElemStart])
        }
        case EvElemEnd(_,inlineTags(tagName)) => {

          val elm = inlineStack.pop()
          // If none of text is appended as CDATA, then close the starting tag with "/>".
          val emptyTag = ("^.*(<" + tagName + "[^<>]*)>$").r
          inlineBuff.toString() match {
            case emptyTag(tagStart) =>
              inlineBuff.insert(inlineBuff.length - 1, "/")
            case _ => inlineBuff ++= s"</$tagName>"
          }

          if(inlineStack.isEmpty){
            // Flush the buffer by appending it to the last sentence.
            sentences.last.concat("en", inlineBuff.toString())
            previousWasInline = true;
          }
        }
        case EvElemStart(_,_,_,_) => {
          // Clear concatenating texts.
          previousWasInline = false;
        }
        case EvText(text) =>
          if(!ignoring) {
            var t = text.trim()
            if(t.nonEmpty){
              t = t.replaceAll("\\s+", " ")

              if(inlineStack.isEmpty){
                if(previousWasInline) {
                  // A heuristic rule to join the period right after the closing inline tag.
                  // ex) <p>foo said that to <xref href="xxx">bar</xref> yesterday.</p>
                  val buff = new StringBuilder()
                  sentences.last.txt.get("en") match {
                    case Some(txt) => buff ++= txt
                  }
                  addSpaceAndText(buff, t)
                  t = buff.toString()

                  sentences.remove(sentences.size - 1)
                  previousWasInline = false;
                }

                // Split the string, and flush sentences.
                addSentences(t, pos, sentences)

              } else {
                // Buffer text.
                inlineBuff ++= t
              }

              pos += 1
            }
          }
        case _ =>
      }
    }
    doc.sentences = sentences.toList
    for(t <- doc.sentences){
      println(t.txt.get("en").get)
    }

    doc
  }


}
