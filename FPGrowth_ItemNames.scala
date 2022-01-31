package alaaomar

// $example on$
import org.apache.spark.ml.fpm.FPGrowth
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._

// $example off$

object FPGrowth_ItemNames {

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

    data.printSchema();
    import spark.implicits._
    //Organize Shopping Basket
   // # Organize the data by shopping basket
   val rawData = data.select(data("dTicketInternalKey"),
       'ItemName)


    val baskets  = rawData.groupBy("dTicketInternalKey").agg(collect_list("ItemName")
      .as("items"))
    //baskets .printSchema()
    baskets .show(false)



    // Extract out the items
  val baskets_ds = baskets.select("items").as[Array[String]].toDF("items")

    baskets_ds.show(5)
    // Use FPGrowth
   val fpgrowth = new FPGrowth().setItemsCol("items").setMinSupport(0.001).setMinConfidence(0)
   val model = fpgrowth.fit(baskets_ds)

    // Display frequent itemsets.
    model.associationRules.show()
    val mostPopularItemInABasket = model.freqItemsets
    mostPopularItemInABasket.createOrReplaceTempView("mostPopularItemInABasket")
    mostPopularItemInABasket.show()

    //View Generated Association Rules

    // Display generated association rules.
    val ifThen = model.associationRules
    ifThen.createOrReplaceTempView("ifThen")
    ifThen.show()

    // transform examines the input items against all the association rules and summarize the
    // consequents as prediction
    model.transform(baskets_ds).show()

    //
    model.write.overwrite()
      .save("data/models/fpgrowth-ItemNames-all")

  }
}
