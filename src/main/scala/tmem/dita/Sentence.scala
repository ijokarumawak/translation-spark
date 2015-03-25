package tmem.dita

/**
 * Created by koji on 2/27/15.
 */
case class Sentence(
  // Multiple Sentence can have the same pos, if the XML element has multiple sentences.
  // Txt contains multiple languages.
  var pos : Int = 0,
  // Globally unique sequence ID.
  var id : Int = 0,
  var txt : Map[String, String] = Map.empty[String, String]
){

  var tokens : Seq[String] = Seq.empty

  def this(pos : Int, en : String) = {
    this(pos)
    txt += ("en" -> en)
  }

  def concat(lang : String, text : String) = {
    val str = txt(lang).concat(text)
    txt += (lang -> str)
  }

  override def toString() = {
    pos + ":" + txt
  }

}
