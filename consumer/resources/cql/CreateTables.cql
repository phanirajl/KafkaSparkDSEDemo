CREATE KEYSPACE if not exists demo WITH REPLICATION = {'class': 'SimpleStrategy', 'replication_factor': 1};

CREATE TABLE if not exists demo.sensor_detail (
	edge_id text,
	serial_number text,
	ts10min text,
	ts timestamp,
	depth double,
	value double,
	PRIMARY KEY (( serial_number, ts10min ), ts)
); 

CREATE TABLE if not exists demo.last (
	edge_id text,
	serial_number text,
	ts10min text,
	ts timestamp,
        depth double,
	value double,
	PRIMARY KEY (( serial_number ))
);

CREATE TABLE if not exists demo.count (
	pk int,
	ts timestamp,
	count bigint,
	count_ma double,
	PRIMARY KEY (pk, ts)
);

CREATE TABLE IF NOT EXISTS demo.sensor_meta
( serial_number text,
  deploy_date timestamp,
  floor int,
  hive_num int,
  latitude text,
  longitude text,
  main_date  timestamp,
  manuf_date  timestamp,
  retired    boolean,
  sensor_type  text,
  vendor     text,
  wing       text,
  lat_long   text,
  PRIMARY KEY (serial_number)
)
;
CREATE TABLE IF NOT EXISTS demo.sensor_summary
( serial_number text,
  ts10min text,
  max_depth double,
  max_value  double,
  min_depth double,
  min_value double,
  sum_depth double,
  sum_value double,
  avg_depth double,
  avg_value double,
  mean_depth double,
  mean_value double,
  stddev_depth double,
  stddev_value double,
  row_count   int,
  PRIMARY KEY ((serial_number),ts10min))
WITH CLUSTERING ORDER BY (ts10min DESC)
;

CREATE TABLE IF NOT EXISTS demo.sensor_full_summary
( serial_number text,
  ts10min text,
  max_depth double,
  max_value  double,
  min_depth double,
  min_value double,
  sum_depth double,
  sum_value double,
  avg_depth double,
  avg_value double,
  mean_depth double,
  mean_value double,
  stddev_depth double,
  stddev_value double,
  row_count   int,
  deploy_date timestamp,
  floor int,
  hive_num int,
  latitude text,
  longitude text,
  main_date  timestamp,
  manuf_date  timestamp,
  retired    boolean,
  sensor_type  text,
  vendor     text,
  wing       text,
  lat_long   text,
  PRIMARY KEY ((serial_number),ts10min))
WITH CLUSTERING ORDER BY (ts10min DESC);

CREATE TABLE IF NOT EXISTS demo.dup_full_summary
( serial_number text,
  ts10min text,
  max_depth double,
  max_value  double,
  min_depth double,
  min_value double,
  sum_depth double,
  sum_value double,
  row_count   int,
  deploy_date timestamp,
  floor int,
  hive_num int,
  latitude text,
  longitude text,
  main_date  timestamp,
  manuf_date  timestamp,
  retired    boolean,
  sensor_type  text,
  vendor     text,
  wing       text,
  lat_long   text,
  PRIMARY KEY ((serial_number),ts10min))
WITH CLUSTERING ORDER BY (ts10min DESC);
