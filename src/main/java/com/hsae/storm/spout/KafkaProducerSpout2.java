package com.hsae.storm.spout;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.hbase.util.Bytes;
import org.apache.storm.spout.SpoutOutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichSpout;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;

/**
 * @ClassName: KafkaProducerSpout
 * @Description: 从网关读取数据并写入kafka
 * @author: 熊尧
 * @company: 上海势航网络科技有限公司
 * @date 2016年5月3日 下午3:52:01
 */
public class KafkaProducerSpout2 extends BaseRichSpout {

	private int i = 0;

	private static final long serialVersionUID = 1L;

	private static final Logger LOG = LoggerFactory.getLogger(KafkaProducerSpout2.class);

	/**
	 * @Fields EACH_RECEIVE_MAX_SIZE : 一次接收，每种数据类型最多接收多少个数据包
	 */
	private static final int EACH_DATA_TYPE_RECEIVE_MAX_SIZE = 1000;

	/**
	 * @Fields MAX_DELAY_TIME : 一次接收最多接收多长时间
	 */
	private static final int MAX_DELAY_TIME = 10000;

	/**
	 * @Fields CONTINUOUSLY_RECEIVE_COUNT :
	 *         单个receive连续接收处理的最大次数，当达到这个次数时，就必须切换到下一个reciever，<br />
	 *         防止一个receiver独占资源<br />
	 *         收取EACH_RECEIVE_MAX_SIZE或者达到MAX_DELAY_TIME后将数据emit，算是一次接收处理
	 */
	private static final int CONTINUOUSLY_RECEIVE_TIMES = 60;

	/**
	 * @Fields HEARTBEAT_INTERVAL : 失联时间
	 */
	private static final int OUT_OF_CONTACT = 60;

	private static final int RECEIVE_TIM_OUT = 200;

	private ZMQ.Context zmq_context = null;

	/**
	 * @Fields zmpSenderAddrs : zmq接收者地址
	 */
	private String[] zmpSenderAddrs = null;

	/**
	 * @Fields receivers : zmq接收者队列
	 */
	private List<ZMQ.Socket> receivers = null;

	private List<Long> lastContactTimeOfReceivers = new ArrayList<Long>();

	/**
	 * @Fields pollingIndex : 轮询zmq的sender的索引
	 */
	private short pollingIndex = 0;

	/**
	 * @Fields currentReceivedTimes : 当前接收处理了多少次
	 */
	private int currentReceivedTimes = 0;

	public static final String OUTPUT_FIELD_NAME = "source";

	private SpoutOutputCollector _collector = null;

	public KafkaProducerSpout2 setZmpSenderAddrs(String zmpSenderAddrs) {
		this.zmpSenderAddrs = zmpSenderAddrs.split(",", -1);
		return this;
	}

	private ZMQ.Socket initZMQSocket(String addr, int receiveTimeOut) {
		ZMQ.Socket receiver = zmq_context.socket(ZMQ.PULL);
		receiver.setReceiveTimeOut(receiveTimeOut);
		receiver.connect(addr);
		LOG.info("Init ZMQ.Socket at " + addr);
		return receiver;
	}

	@Override
	public void open(Map conf, TopologyContext context, SpoutOutputCollector collector) {
		_collector = collector;
		zmq_context = ZMQ.context(2);
		receivers = new ArrayList<ZMQ.Socket>();
		for (int i = 0; i < zmpSenderAddrs.length; i++) {
			receivers.add(initZMQSocket(zmpSenderAddrs[i], RECEIVE_TIM_OUT));
			lastContactTimeOfReceivers.add((new Date()).getTime());
		}
	}

	@Override
	public void ack(Object msgId) {
		LOG.info("ack  success " + msgId);
	}

	@Override
	public void fail(Object msgId) {
		LOG.info("ack  fail " + msgId);
	}

	/**
	 * @Title: nextPollingIndex @Description: 切换到下一个receiver @author: 韩欣宇 @date
	 *         2015年6月11日 下午1:16:23 @throws
	 */
	private void nextPollingIndex() {
		pollingIndex++;
		if (pollingIndex >= receivers.size()) {
			pollingIndex = 0;
		}
		currentReceivedTimes = 0;
	}

	@Override
	public void nextTuple() {
		HashMap<Integer, List<byte[]>> typeId2ContentMap = new HashMap<Integer, List<byte[]>>();
		int currentPollingIndex = pollingIndex;
		LOG.debug("receive " + currentPollingIndex + " start  receiving data ----------");
		long startTime = System.currentTimeMillis();
		List<byte[]> messages = new ArrayList<byte[]>();
		while (true) {
			byte[] receivedData = receivers.get(currentPollingIndex).recv();// 接收器去从网关中去数据
			if (receivedData == null || receivedData.length <= 4) {
				// 切换到下一个receiver
				LOG.debug("receive " + currentPollingIndex + " receive null and break");
				long currentTime = (new Date()).getTime();
				if ((currentTime - lastContactTimeOfReceivers.get(currentPollingIndex)) / 1000.0 > OUT_OF_CONTACT) {
					Socket socket = receivers.get(currentPollingIndex);
					socket.close();
					receivers.set(currentPollingIndex, initZMQSocket(zmpSenderAddrs[currentPollingIndex], RECEIVE_TIM_OUT));
					socket = null;
					lastContactTimeOfReceivers.set(currentPollingIndex, currentTime);
				}
				nextPollingIndex();
				break;
			} else {
				i++;
				long currentTime = (new Date()).getTime();
				lastContactTimeOfReceivers.set(currentPollingIndex, currentTime);

				byte[] dataType = new byte[4];
				System.arraycopy(receivedData, 0, dataType, 0, 4);
				int dataTypeId = Bytes.toInt(dataType);
				if (dataTypeId == 100035 | dataTypeId == 100036) {
					LOG.debug("receive " + dataTypeId + " ");
				}
				// 车辆ID
				byte[] vehicleId = new byte[8];
				System.arraycopy(receivedData, 4, vehicleId, 0, 8);
				// 消息内容
				byte[] dataContent = new byte[receivedData.length - 12];
				// 数组之间的复制
				System.arraycopy(receivedData, 12, dataContent, 0, receivedData.length - 12);

				messages.add(receivedData);

				if (!typeId2ContentMap.containsKey(dataTypeId)) {
					List<byte[]> dataList = new ArrayList<byte[]>();
					typeId2ContentMap.put(dataTypeId, dataList);
				}

				// 添加到相应队列
				List<byte[]> contentList = typeId2ContentMap.get(dataTypeId);
				contentList.add(dataContent);

				_collector.emit("111111", new Values(Bytes.toHex(receivedData)));

				if (contentList.size() >= EACH_DATA_TYPE_RECEIVE_MAX_SIZE) {
					break;
				}

			}
			if (System.currentTimeMillis() - startTime >= MAX_DELAY_TIME) {
				// 当数据条数大于等于1000 或者执行时间大于1秒时 跳出循环
				break;
			}
		}
		_collector.emit("222222", new Values(messages));
		LOG.warn("========发送到到kafka的数据条数为：=======" + i);
		LOG.warn("receiver " + currentPollingIndex + " finish receiving data in " + (System.currentTimeMillis() - startTime) + "ms");
		// 当前已经接收处理的次数+1，如果已经超出了CONTINUOUSLY_RECEIVE_TIMES，就换下一个接收器
		currentReceivedTimes++;
		if (currentReceivedTimes >= CONTINUOUSLY_RECEIVE_TIMES) {
			nextPollingIndex();
		}
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declareStream("111111", new Fields(OUTPUT_FIELD_NAME));
		declarer.declareStream("222222", new Fields(OUTPUT_FIELD_NAME));
	}
}
