package tmem.dita

import org.apache.spark.SparkContext
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.linalg.distributed.RowMatrix

import scala.collection.mutable

/**
 * Created by koji on 2/27/15.
 */
object DimSumTest2 {
  def main(args: Array[String]) = {

    // Create SparkContext
    val sc = new SparkContext("local[4]", "test")


    // Columns: Sentences
    // Rows: Terms
    val vectors = Seq(
      Vectors.dense(0.0, 0.3, 0.3, 0.0),
      Vectors.dense(0.1, 0.2, 0.3, 0.1),
      Vectors.dense(0.1, 1.3, 1.3, 0.1),
      Vectors.dense(1.1, 0.3, 0.3, 1.1)
    )

    // put collections in Spark
    val rows = sc.parallelize(vectors)

    val mat = new RowMatrix(rows)
    println("mat")
    println(mat.numRows())
    println(mat.numCols())


    val simPerfect = mat.columnSimilarities()


    println("simPerfect")
    println(simPerfect.numRows())
    println(simPerfect.numCols())
    for(entry <- simPerfect.entries){
      println(entry)
    }


  }
}
