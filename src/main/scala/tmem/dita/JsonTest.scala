package tmem.dita

import play.api.libs.json.Json

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
 * Created by koji on 3/17/15.
 */
object JsonTest {

  case class Foo (name : String, var children: List[Foo] = List.empty[Foo], var map : Map[String, String] = Map.empty)

  def main(args: Array[String]) = {
    val list : List[String] = List("hoge")
    println(Json.toJson(list))

    val mlist : mutable.MutableList[String] = mutable.MutableList("hoge")
    mlist += "fuga"
    println(Json.toJson(mlist))

    val seq : Seq[String] = mutable.Seq("hoge")
    println(Json.toJson(seq))

    val mseq : mutable.Seq[String] = mutable.Seq("hoge")
    mseq :+ "fuga"
    println(Json.toJson(mseq))


    implicit val fooFmt = Json.format[Foo]

    implicit val sentenceFmt = Json.format[Sentence]
    implicit val documentFmt = Json.format[Document]
//    implicit val sentencesFmt = Json.format[mutable.MutableList[Sentence]]
//    implicit val sentencesFmt = Json.format[List[Sentence]]
//    implicit val foosFmt = Json.format[mutable.Buffer[Foo]]

    val doc : Document = new Document("uri")
    val sentences : ListBuffer[Sentence] = ListBuffer(new Sentence(0, "foo bar baz"))
    doc.sentences = sentences.toList
    println(Json.toJson(doc))

    val foo : Foo = new Foo("foo")
    foo.children = foo.children :+ new Foo("bar", List.empty, Map("a" -> "1", "c" -> "2"))
    foo.map = Map("a" -> "b", "c" -> "d")
    println(Json.toJson(foo))
  }

}
