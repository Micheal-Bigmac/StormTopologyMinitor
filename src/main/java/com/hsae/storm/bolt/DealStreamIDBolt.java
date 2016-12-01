package com.hsae.storm.bolt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kafka.common.FailedToSendMessageException;
import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;

public class DealStreamIDBolt extends BaseRichBolt {

	private static final Logger LOG = LoggerFactory.getLogger(DealStreamIDBolt.class);
	// public static final String OUTPUT_FIELD_NAME = "source";
	protected OutputCollector collector;

	Producer<String, byte[]> producer = null;
	Producer<String, byte[]> producer1 = null;
	Producer<String, byte[]> producer2 = null;

	public static final String OUTPUT_FIELD_NAME = "source";
	private static final String topic = "topic-online";
//	private static final String topic1 = "topic-online-hadoop";
//	private static final String topic2 = "topic-100035-100036";

	/**
	 * @Fields MAX_DELAY_TIME : 一次接收最多接收多长时间
	 */
	private static final int MAX_DELAY_TIME = 10000;
	private static FileSystem fs = null;
	private static FileStatus[] status = null;
	private static BufferedReader br = null;
	private List<KeyedMessage<String, byte[]>> messages = new ArrayList<KeyedMessage<String, byte[]>>();
//	private List<KeyedMessage<String, byte[]>> messages1 = new ArrayList<KeyedMessage<String, byte[]>>();   
	private KeyedMessage<String, byte[]> message=null;
//	private KeyedMessage<String, byte[]> message1=null; 
//	private KeyedMessage<String, byte[]> _3536 =null;

	@Override
	public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
		this.collector = collector;
		producer = createProducer();
//		producer1 = createProducer();
//		producer2 = createProducer();
//		try {
//			fs = FileSystem.get(new Configuration());
//			status = fs.listStatus(new Path("hdfs://nameservice1/complete"));
//		} catch (IOException e) {
//			LOG.debug("文件初始化错误！", e);
//		}
	}

	private Producer<String, byte[]> createProducer() {
		Properties properties = new Properties();
		properties.put("zookeeper.connect", "name1.cvnavi.com:2181,name2.cvnavi.com:2181,name3.cvnavi.com:2181");// 声明zk
		properties.put("serializer.class", "kafka.serializer.DefaultEncoder");
		properties.put("key.serializer.class", "kafka.serializer.StringEncoder");
		// 该属性表示你需要在消息被接收到的时候发送ack给发送者。以保证数据不丢失
		properties.put("request.required.acks", "1");
		properties.put("metadata.broker.list", "data1.cvnavi.com:9092,data2.cvnavi.com:9092,data3.cvnavi.com:9092,data4.cvnavi.com:9092,data5.cvnavi.com:9092,data6.cvnavi.com:9092");// 声明kafka

		return new Producer<String, byte[]>(new ProducerConfig(properties));
	}


	@Override
	public void execute(Tuple input) {
		long startTime = System.currentTimeMillis();
		List<byte[]> receivedDatas = (List<byte[]>) input.getValueByField("source");// msg为自定义消息Field
	
		try {
			for (byte[] receivedData : receivedDatas) {
				byte[] dataType = new byte[4];
				System.arraycopy(receivedData, 0, dataType, 0, 4);
				int dataTypeId = Bytes.toInt(dataType);

				LOG.warn(dataTypeId + "++++");
				// 车辆ID
				byte[] vehicleId = new byte[8];
				System.arraycopy(receivedData, 4, vehicleId, 0, 8);
				// 消息内容
				byte[] dataContent = new byte[receivedData.length - 12];
				// 数组之间的复制
				System.arraycopy(receivedData, 12, dataContent, 0, receivedData.length - 12);

				// producer：将网关取出得数据发送至kafka消息队列，以车辆ID作为分区
				message = new KeyedMessage<String, byte[]>(topic, String.valueOf(Bytes.toLong(vehicleId)), receivedData);
				
//				message1 = new KeyedMessage<String, byte[]>(topic1, String.valueOf(Bytes.toLong(vehicleId)), dataContent);

				messages.add(message);
//				messages1.add(message1);
//				if (dataTypeId == 100035 || dataTypeId == 100036) {
//					_3536 = new KeyedMessage<String, byte[]>(topic2, String.valueOf(Bytes.toLong(vehicleId)), receivedData);
//					producer2.send(_3536);
//					LOG.warn(dataTypeId + "" + "size" + receivedData.length);
//				}

//				if (System.currentTimeMillis() - startTime >= MAX_DELAY_TIME) {
//					// 当数据条数大于等于1000 或者执行时间大于1秒时 跳出循环
//					sendMessagesFromHdfs(messages);
//				}
			}
			producer.send(messages);
//			producer1.send(messages1);
			collector.ack(input);
			messages.clear();
//			messages1.clear();

		} catch (FailedToSendMessageException e) {
			for (byte[] receivedData : receivedDatas) {
				collector.emit("11111", new Values(Bytes.toHex(receivedData)));
			}
			collector.fail(input);
			LOG.error("Failed to send messages!", e);
		} catch (IllegalStateException e) {
			LOG.error("DealStreamBolt throws IllegalStateException!", e);
			collector.fail(input);
		} catch (Exception e) {
			LOG.error("DealStreamBolt throws error!", e);
			collector.fail(input);
		}
	}


	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declareStream("11111", new Fields(OUTPUT_FIELD_NAME));
	}

}
