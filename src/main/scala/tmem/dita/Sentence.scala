package tmem.dita

/**
 * Created by koji on 2/27/15.
 */
class Sentence(
  // Multiple Sentence can have the same pos, if the XML element has multiple sentences.
  var pos : Int = 0,
  var sentence : String = "",
  var tokens : Seq[String] = Seq.empty
){

  var translated : String = ""

  def concat(text : String) = {
    sentence += text
  }

  override def toString() = {
    pos + ":" + sentence
  }

}
