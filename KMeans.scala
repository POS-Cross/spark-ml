package alaaomar
import org.apache.spark.ml.clustering.{KMeans, KMeansModel}
import org.apache.spark.ml.feature.VectorAssembler
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types._
import org.apache.spark.ml.linalg.{Vectors, Vector}
import org.apache.spark.ml.clustering.KMeans
import org.apache.spark.sql.functions.udf


object KMeans {

  def main(args: Array[String]): Unit = {
    import org.apache.log4j._
    Logger.getLogger("org").setLevel(Level.ERROR);
    val spark = SparkSession.builder().master("local").getOrCreate()

    // For more information about the data:
    // https://www.kaggle.com/c/titanic/data
    val data = spark.read.
      option("header", "true").
      option("inferSchema","true").
      format("csv").
      load("data/bravo/Query.csv")
    //data.show(10);

    import spark.implicits._


    val datapreparedAll = data.
      select(data("dTicketInternalKey"),
        'dItemInternalKey,
        'bHasPromotions,
        'OfferNo,
        'DeptNo,
        'CustomerNo)

    datapreparedAll.na.drop()
    datapreparedAll.show(5)



    // transform userDf with VectorAssembler to add feature column
    val cols = Array("dTicketInternalKey", "dItemInternalKey","bHasPromotions", "OfferNo","DeptNo","CustomerNo")
    val assembler = new VectorAssembler().setInputCols(cols).setOutputCol("features")
    val featureDf = assembler.transform(datapreparedAll)
    featureDf.printSchema()
    featureDf.show(10)

    // split data set training(70%) and test(30%)
    val seed = 5043
    val Array(trainingData, testData) = featureDf.randomSplit(Array(0.7, 0.3), seed)


    // kmeans model with 6 clusters
    val kmeans = new KMeans()
      .setK(6)
      .setFeaturesCol("features")
      .setPredictionCol("prediction")
    val kmeansModel = kmeans.fit(trainingData)
    kmeansModel.clusterCenters.foreach(println)



    // test the model with test data set
    val predictDf = kmeansModel.transform(testData)
    predictDf.show(10)



    // no of categories
    predictDf.groupBy("prediction").count().show()


    // save model
    kmeansModel.write.overwrite()
      .save("data/models/Bkmeans-model")

    // load model
    // val kmeansModelLoded = KMeansModel
    //  .load("data/models/Bkmeans-model")

  }
}
