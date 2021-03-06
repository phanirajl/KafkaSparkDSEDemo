package com.datastax.demo

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Created by carybourgeois on 10/30/15.
  *  Modified by jasonhaugland on 10/20/16.
  *  changed to structured streaming by jasonhaugland on 11/29/18.
 */

/**
  */
import java.sql.Timestamp
import java.text.{DateFormat, SimpleDateFormat}

import com.datastax.driver.core.Session

import collection.JavaConversions._

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._

import com.datastax.spark.connector.cql.CassandraConnector
import com.datastax.spark.connector.streaming._
import org.apache.spark.sql.streaming.{OutputMode, Trigger}
import org.apache.spark.sql.types.{IntegerType,StringType}



object SparkKafkaConsumer {


  def main(args: Array[String]) {

    println(s"entered main")

    val sparkJob = new SparkJob()
    try {
      sparkJob.runJob()
    } catch {
      case ex: Exception =>
        println("error in main running spark job")
    }
  }
}

class SparkJob extends Serializable {

  println(s"before build spark session")

  def runJob() = {
  val appName = "SparkKafkaConsumer"

  val sparkSession =
    SparkSession.builder
      .appName(appName)
      .config("spark.cassandra.connection.host", "node0")
      .getOrCreate()

  println(s"after build spark session")
  val sensorMinuteFormat = new SimpleDateFormat("YYYYMMddHHmm")


  println(s"before reading kafka stream after runJob")

  import sparkSession.implicits._


    val sensDetailDS = sparkSession.readStream
      .format("kafka")
      .option("subscribe", "stream_ts")
      .option("failOnDataLoss", "false")
      .option("startingOffsets", "latest")
      .option("kafka.bootstrap.servers", "node0:9092")
      .option("includeTimestamp", true)
      .load()
      .selectExpr("CAST(value AS STRING)","CAST(timestamp as Timestamp)",
                  "CAST(key as STRING)")
      .as[(String, Timestamp, String)] 

    println(s"finished reading transaction kafka stream ")
    sensDetailDS.printSchema()

    val sensDetailCols =  List("edge_id","serial_number","depth","value","ts","ts10min")

    val sens_df =
        sensDetailDS.map { line =>
        val payload = line._1.split(";")
        val currentMinute = sensorMinuteFormat.format(line._2)
        (payload(0), payload(1),             
	 payload(4).toDouble,
	 payload(5).toDouble,
	 line._2, currentMinute
         )
      }.toDF(sensDetailCols: _*)
    println(s"after sens_df ")
    sens_df.printSchema()

    val windowedCount = sens_df
      .groupBy( $"serial_number",window($"ts", "10 minutes"))
      .agg(
	   max($"depth").alias("max_depth"),min($"depth").alias("min_depth"),
           mean("depth").alias("mean_depth"),stddev("depth").alias("stddev_depth"),
	   avg($"depth").alias("avg_depth"), sum($"depth").alias("sum_depth"),
	   max($"value").alias("max_value"),min($"value").alias("min_value"),
           mean("value").alias("mean_value"),stddev("value").alias("stddev_value"),
  	   avg($"value").alias("avg_value"), sum($"value").alias("sum_value"),
	   count(lit(1)).alias("row_count")
          )
    println(s"after window ")
    windowedCount.printSchema()
    
    val clean_df = windowedCount.selectExpr ( "serial_number", 
 	"Cast(date_format(window.start, 'yyyyMMddhhmm') as string) as ts10min",
	"Cast(max_depth as double) as max_depth",
	"Cast(min_depth as double) as min_depth",
	"Cast(avg_depth as double) as avg_depth",
	"Cast(sum_depth as double) as sum_depth",
	"Cast(mean_depth as double) as mean_depth",
	"Cast(stddev_depth as double) as stddev_depth",
	"Cast(max_value as double) as max_value",
	"Cast(min_value as double) as min_value",
	"Cast(avg_value as double) as avg_value",
	"Cast(sum_value as double) as sum_value",
	"Cast(mean_value as double) as mean_value",
	"Cast(stddev_value as double) as stddev_value",
	"Cast(row_count as int) as row_count")

    println(s"after clean_df ")
    clean_df.printSchema()
 
/*
    --   decided not to join this here as it makes very wide table
    --    can join in analtyic query later but it works  :)
    val joined_df = clean_df.join(sens_meta_df, Seq("serial_number"))
    println(s"after joined_df ")
    joined_df.printSchema()
*/
  
    val det_query = sens_df.writeStream
      .format("org.apache.spark.sql.cassandra")
      .option("checkpointLocation", "dsefs://node0:5598/checkpoint/detail/")
      .option("keyspace", "demo")
      .option("table", "sensor_detail")
      .outputMode(OutputMode.Update)
      .start()
    println (s"after write to sensor_detail")

    val det2_query = sens_df.writeStream
      .format("org.apache.spark.sql.cassandra")
      .option("checkpointLocation", "dsefs://node0:5598/checkpoint/detail2/")
      .option("keyspace", "demo")
      .option("table", "last")
      .outputMode(OutputMode.Update)
      .start()

    val win_query = clean_df.writeStream
      .format("org.apache.spark.sql.cassandra")
      .option("checkpointLocation", "dsefs://node0:5598/checkpoint/summary/")
      .option("keyspace", "demo")
      .option("table", "sensor_summary")
      .outputMode(OutputMode.Update)
      .start()

    win_query.awaitTermination()
    det_query.awaitTermination()
    det2_query.awaitTermination()
//     better might be awaitAnyTermination
//    sparkSession.streams.awaitAnyTermination()
    println(s"after awaitTermination ")
    sparkSession.stop()
  }
}
