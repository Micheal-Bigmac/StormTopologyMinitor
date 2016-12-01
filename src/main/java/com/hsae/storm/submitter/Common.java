package com.hsae.storm.submitter;

import java.util.List;
import java.util.Properties;

import com.hsae.rdbms.common.PropertiesUtil;

public class Common {

	public static String kafkaSpoutToHbase;
	public static String kafkaSpoutToHbaseJar;
	public static String kafkaSpoutToDb2;
	public static String kafkaSpoutToDb2Jar;
	public static String kafkaSpoutToGps;
	public static String kafkaSpoutToGpsJar;
	public static String kafkaSpoutToAlarmPush;
	public static String kafkaSpoutToAlarmPushJar;
	public static String backupZmqkafka;
	public static String backupZmqkafkaJar;
	public static String dirPath;
	public static Properties prop;
	public static String[] SendMailString;
	public static int SleepMillions;
	public static String NIMBUS_HOST;
	public static int NIMBUS_THRIFT_PORT;
	public static String NIMBUS_UI;
	static{
		prop = PropertiesUtil.getProperites("Common.properties");
		kafkaSpoutToHbase = prop.getProperty("kafkaSpoutToHbase");
		kafkaSpoutToHbaseJar = prop.getProperty("kafkaSpoutToHbaseJar");
		kafkaSpoutToDb2 = prop.getProperty("kafkaSpoutToDb2");
		kafkaSpoutToDb2Jar = prop.getProperty("kafkaSpoutToDb2Jar");
		kafkaSpoutToGps = prop.getProperty("kafkaSpoutToGps");
		kafkaSpoutToGpsJar = prop.getProperty("kafkaSpoutToGpsJar");
		kafkaSpoutToAlarmPush = prop.getProperty("kafkaSpoutToAlarmPush");
		kafkaSpoutToAlarmPushJar = prop.getProperty("kafkaSpoutToAlarmPushJar");
		backupZmqkafka=prop.getProperty("backupZmqkafka");
		backupZmqkafkaJar=prop.getProperty("backupZmqkafkaJar");
		dirPath = prop.getProperty("resourceDir");
		SendMailString=prop.getProperty("SendMailString").split(",");
		String stopMillion = prop.getProperty("SleepMillions", "300000");  // 默认5分钟 暂停时间
		SleepMillions=Integer.valueOf(stopMillion);
		NIMBUS_HOST=prop.getProperty("NIMBUS_HOST");
		String port = prop.getProperty("NIMBUS_THRIFT_PORT", "6627");
		NIMBUS_THRIFT_PORT=Integer.valueOf(port);
		NIMBUS_UI=prop.getProperty("NIMBUS_UI");
	}
}
