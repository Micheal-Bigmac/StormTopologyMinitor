package com.hsae.storm.submitter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.storm.Config;
import org.apache.storm.generated.AuthorizationException;
import org.apache.storm.generated.ClusterSummary;
import org.apache.storm.generated.KillOptions;
import org.apache.storm.generated.Nimbus;
import org.apache.storm.generated.NotAliveException;
import org.apache.storm.generated.TopologySummary;
import org.apache.storm.thrift.TException;
import org.apache.storm.utils.NimbusClient;
import org.apache.storm.utils.Utils;
import org.mortbay.log.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;

public class MonitorStormTopology {
	public Common common = null;
	public List<String> ErorTopology = new ArrayList<String>();
	public Set<String> reStart = new HashSet<String>();
	public Nimbus.Client client = null;
	private static final Logger LOG = LoggerFactory.getLogger(MonitorStormTopology.class);
	public ClusterSummary clusterInfo = null;
	private static SendMail sendmail = null;
	private static Map<String, List<Long>> _metric = null;
	// private TreeMap<String, V>

	public MonitorStormTopology() {

		Map conf = Utils.readStormConfig();
		// nimbus服务器地址
		conf.put(Config.NIMBUS_HOST, Common.NIMBUS_HOST);
		// nimbus thrift地址
		conf.put(Config.NIMBUS_THRIFT_PORT, Common.NIMBUS_THRIFT_PORT);
		client = NimbusClient.getConfiguredClient(conf).getClient();
		sendmail = new SendMail();
		_metric = new HashMap<String, List<Long>>();
	}

	public static void main(String[] args) throws Exception {

		MonitorStormTopology moitor = new MonitorStormTopology();
		while (true) {
			moitor.MonitorTopology();
			if (moitor.ErorTopology.size() > 0) {
				moitor.deactivateTopology();
				Log.info(" start sleep " + Common.SleepMillions + "million seconds");
				Thread.currentThread().sleep(Common.SleepMillions);
				Log.info("start kill Topology ");
				moitor.killTopology();
				Log.info("Have killed Topology ");
				Thread.currentThread().sleep(15000);
			}
			// moitor.ErorTopology.add("topic-online-topology-20161117-hbase");
			// moitor.ErorTopology.add("topic-online-topology-20161111-gps");
			 moitor.ErorTopology.add("topic-test-topology-20161125-hbase");

			for (String value : moitor.ErorTopology) {

				Log.info(" start Republic Topology " + value);
				if (Common.kafkaSpoutToAlarmPush.equals(value)) {
					CommonSubmit.ReSubmit(Common.kafkaSpoutToAlarmPush, Common.kafkaSpoutToAlarmPushJar);
				} else if (Common.kafkaSpoutToDb2.equals(value)) {
					CommonSubmit.ReSubmit(Common.kafkaSpoutToDb2, Common.kafkaSpoutToDb2Jar);
				} else if (Common.kafkaSpoutToGps.equals(value)) {
					CommonSubmit.ReSubmit(Common.kafkaSpoutToGps, Common.kafkaSpoutToGpsJar);
				} else if (Common.kafkaSpoutToHbase.equals(value)) {
					CommonSubmit.ReSubmit(Common.kafkaSpoutToHbase, Common.kafkaSpoutToHbaseJar);
				} else if (Common.backupZmqkafka.equals(value)) {
					CommonSubmit.ReSubmit(Common.backupZmqkafka, Common.backupZmqkafkaJar);
				} else {
					Log.info(" not found  such Topology ,Please Check  your configuration  Common.properties");
				}
				sendmail.send(Common.SendMailString, "Storm Topology name is " + value + ", sleep " + Common.SleepMillions + " have beam deactived then killed finally republic topology Please chose one person to confirm");
			}
			Log.info(" start sleep " +5 + "minutes");
			Thread.currentThread().sleep(5 * 1000 * 60);
			
		}
	}

	private void deactivateTopology() throws NotAliveException, AuthorizationException, TException {
		for (String topology : this.ErorTopology) {
			LOG.info("存在问题的  任务 有  topology" + topology + " start deactative topology  " + topology);
			client.deactivate(topology);
		}
	}

	/**
	 * 调用thrift 接口 重启worker 目前测试接口不好使
	 */
	private void restartWorker() {

		// for (String _value : reStart) {
		// System.out.println(_value);
		// }
	}

	public String getHttpConnectResponse(String mapDecoderUrl, String paramString, String Type) throws IOException {
		URL url = new URL(mapDecoderUrl);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod(Type);
		conn.setDoOutput(true);// 是否输入参数
		if (paramString != null) {
			conn.getOutputStream().write(paramString.getBytes());// 输入参数
		}
		InputStream inStream = conn.getInputStream();
		String result = new String(readInputStream(inStream), "UTF-8");
		inStream.close();
		conn.disconnect();
		return result;
	}

	private byte[] readInputStream(InputStream inStream) throws IOException {
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int len = 0;
		while ((len = inStream.read(buffer)) != -1) {
			outStream.write(buffer, 0, len);
		}
		byte[] data = outStream.toByteArray();
		outStream.close();
		inStream.close();
		return data;
	}

	public TopologyStat SendUrlGet(String mapDecoderUrl, String paramString) throws IOException {
		List<String> result = new ArrayList<String>();
		String jsonStr = getHttpConnectResponse(mapDecoderUrl, paramString, "GET");
		TopologyStat responseList = JSON.parseObject(jsonStr, TopologyStat.class);
		return responseList;
	}

	private void MonitorTopology() throws AuthorizationException, TException, NotAliveException, IOException {
		clusterInfo = client.getClusterInfo();
		StringBuffer urlBuffer = null;
		TopologyStat Stat = null;
		List<TopologySummary> topologyList = clusterInfo.get_topologies();
		for (TopologySummary topologySummary : topologyList) {
			String topology_name = topologySummary.get_name();
			String get_id = topologySummary.get_id();
			urlBuffer = new StringBuffer(Common.NIMBUS_UI);
			urlBuffer.append("api/v1/topology/");
			urlBuffer.append(get_id);
			Stat = SendUrlGet(urlBuffer.toString(), null);

			Long emitted = Stat.getTopologyStats().get(0).getEmitted();
			if (emitted == null) {
				emitted = 0l;
			}
			if (emitted == 0) {
				ErorTopology.add(topology_name);
			} else {
				monitor_metric(topology_name, emitted);
			}
			// 测试
			// ErorTopology.add(topology_name);
		}
	}

	private void monitor_metric(String topology_name, Long _10_default) {
		if (_metric.containsKey(topology_name)) {
			List<Long> list = _metric.get(topology_name);
			if (list.size() == 3) {
				long one_ = list.get(2) - list.get(1);
				long two_ = list.get(1) - list.get(0);
				long three_ = _10_default - list.get(3);
				list.add(_10_default);
				if (three_ < 0 && two_ < 0) {
					sendmail.send(Common.SendMailString, "Storm Topology name is " + topology_name + " is always  slow down");
				}
				list.remove(0);
			} else {
				list.add(_10_default);
			}
		} else {
			List<Long> list = new ArrayList<Long>();
			list.add(_10_default);
			_metric.put(topology_name, list);
		}
	}

	private static void printlnAckEmit(Map<String, Map<String, Long>> get_acked) {
		for (String _key : get_acked.keySet()) {
			Map<String, Long> ack_map = get_acked.get(_key);
			System.out.print(" key is " + _key + " value is ");
			for (String __key : ack_map.keySet()) {
				System.out.print(" --key " + __key + "  --value " + ack_map.get(__key));
			}
			System.out.println();
		}
	}

	private static void printlnEmit(Map<String, Map<String, Long>> get_acked) {
		for (String _key : get_acked.keySet()) {
			Map<String, Long> ack_map = get_acked.get(_key);
			System.out.print(" key is " + _key + " value is ");
			for (String __key : ack_map.keySet()) {
				System.out.print(" --key " + __key + "  --value " + ack_map.get(__key));
			}
			System.out.println();
		}
	}

	private void killTopology() throws NotAliveException, AuthorizationException, TException {
		KillOptions killOpts = new KillOptions();
		killOpts.set_wait_secs(5);
		for (String topologyName : this.ErorTopology) {
			client.killTopologyWithOpts(topologyName, killOpts);
			Log.info(" start kill Topology with 5 seconds ,Killing Topology is " + topologyName);
		}
	}

}
