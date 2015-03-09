package tmem.dita


import tmem.dita.Dictionary
import scala.collection.mutable

/**
 * Created by koji on 2/27/15.
 */
object Test {
  def main(args: Array[String]) = {
    var list = mutable.MutableList(0)
    list.+=(1)
    list.+=(2)
    println(list)
    list = list.dropRight(2)
    println(list)

    var words = mutable.ArrayBuffer.empty[String]
    words += "foo"
    words += "bar"
    words += "bas"
    var dict = new Dictionary(words)
    println(dict.valueOf(0))
  }
}
