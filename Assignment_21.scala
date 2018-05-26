import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.udf

object Assignment_21 {

  //Case class to hold Sports Data
  case class Sports_Data (firstname:String, lastname:String, sports:String, medal_type:String, age:Int, year:Int, country:String)

  def main(args:Array[String]): Unit = {
    //Let us create a spark session object
    //Create a case class globally to be used inside the main method
    val spark = SparkSession
      .builder()
      .master("local")
      .appName("Spark SQL Assignment 20")
      .config("spark.some.config.option", "some-value")
      .getOrCreate()

    println("spark session object is created")

    //Read the Holiday Details from Local file
    val data = spark.sparkContext.textFile("E:\\Prachi IMP\\Hadoop\\Day 18 Spark\\Day 21 Spark SQL\\Sports_Data.txt")

    import spark.implicits._

    //Remove Heder
    val header = data.first()

    //Create Holdiays DF
    val SportsDF = data.filter(row => row != header).map(_.split(","))
      .map(x => Sports_Data(firstname = x(0), lastname = x(1), sports = x(2), medal_type = x(3), age = x(4).toInt, year = x(5).toInt, country = x(6))).toDF()
    //Printing each row of Sports DF
    SportsDF.show()

    //Task 1.1 : What are the total number of gold medal winners every year
    //Need to group on year where medal type if gold

    //Approach 1: Using Spark SQL Operations
    SportsDF.filter("medal_type='gold'").groupBy("year").count().orderBy("year").show()

    //Approach 2: Using SQL Query
    SportsDF.createOrReplaceTempView("Sports_Table")
    spark.sql("Select year,count(year) as Winners from Sports_Table where medal_type='gold' group by year order by year").show()

    //Task 1.2 How many silver medals have been won by USA in each sport
    //Need to group on sports where country is USA and medal_type is silver

    //Approach 1 : Using Spark SQL operations
    SportsDF.filter("country='USA' and medal_type='silver'").groupBy("sports").count().show()

    //Approach 2: Using SQL Query
    spark.sql("Select sports,count(sports) as Winners from Sports_Table where medal_type='silver' and country='USA' group by sports").show()

    //Task 2.1 :Using udfs on dataframe
    //1. Change firstname, lastname columns into
    //Mr.first_two_letters_of_firstname<space>lastname
    //for example - michael, phelps becomes Mr.mi phelps

    //write a basic function in scala

    def Name=(fname: String, lname: String)=>{
      var newName:String=null
      if (fname != null && lname != null) {
        newName="Mr.".concat(fname.substring(0, 2)).concat(" ")concat(lname)
      }
      newName
    }


    //first we have to create a UDF which returns the output as mentioned in above use case
    //Writing the UDF
    val Change_Name = udf(Name(_:String,_:String))

    //Approach 1 : For calling the Custom user define function without registering
    SportsDF.withColumn("Name", Change_Name($"firstname", $"lastname")).show()

    //Approach 2: By registering the function
    spark.sqlContext.udf.register("Name", Name)

    spark.sql("Select Name(firstname,lastname) as changed_Name, sports,medal_type,age,year,country from Sports_Table").show()

    //Task 2.2 2. Add a new column called ranking using udfs on dataframe, where :
    //gold medalist, with age >= 32 are ranked as pro
    //gold medalists, with age <= 31 are ranked amateur
    //silver medalist, with age >= 32 are ranked as expert
    //silver medalists, with age <= 31 are ranked rookie

    //Write basic scala function for the required use case
    def ranking_recived =(medal_type:String,age:Int)=> {
      if(medal_type.equalsIgnoreCase("gold") && age>=32) "pro"
      else if(medal_type.equalsIgnoreCase("gold") && age <=31) "amateur"
      else if(medal_type.equalsIgnoreCase("silver") && age >= 32) "amateur"
      else if(medal_type.equalsIgnoreCase("silver") && age <= 31) "amateur"
      else ""
    }

    val Rankings = udf(ranking_recived(_:String,_:Int))

    //Approach 1: Without Registering the UDF and calling with Spark SQL Operatios
    SportsDF.withColumn("Ranking",Rankings($"medal_type",$"age")).show()

    //Approach 2:By Registering the function
    spark.sqlContext.udf.register("Rankings",ranking_recived)
    spark.sql("Select Rankings(medal_type,age) as changed_Name, sports,medal_type,age,year,country from Sports_Table").show()
    }
  }