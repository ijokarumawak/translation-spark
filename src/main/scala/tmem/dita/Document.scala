package tmem.dita

import java.util.Date

import scala.collection.mutable

/**
 * Created by koji on 3/17/15.
 */
case class Document (
  val uri : String = "",
  val title : String = "",
  var sentences : List[Sentence] = List.empty[Sentence],
  val _type : String = "document",
  val _createdAt : Date = new Date(),
  val _updatedAt : Date = new Date()
  ) {

  override def toString() = {
    uri + ":" + sentences
  }
}


