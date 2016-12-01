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
import org.apache.storm.generated.ExecutorSpecificStats;
import org.apache.storm.generated.ExecutorStats;
import org.apache.storm.generated.ExecutorSummary;
import org.apache.storm.generated.KillOptions;
import org.apache.storm.generated.Nimbus;
import org.apache.storm.generated.NotAliveException;
import org.apache.storm.generated.SpoutStats;
import org.apache.storm.generated.SpoutStats._Fields;
import org.apache.storm.generated.TopologyInfo;
import org.apache.storm.generated.TopologySummary;
import org.apache.storm.thrift.TException;
import org.apache.storm.thrift.meta_data.FieldMetaData;
import org.apache.storm.utils.NimbusClient;
import org.apache.storm.utils.Utils;

import com.alibaba.fastjson.JSON;

public class MonitorTest2 {
	public static List<String> ErorTopology = new ArrayList<String>();
	public static Set<String> reStart = new HashSet<String>();

	public static void main(String[] args) throws Exception {
		MonitorStormTopology();
		System.out.println("存在问题的  任务 有  topology ");
		for (String value : ErorTopology) {
			System.out.println(value);
			if (Common.kafkaSpoutToAlarmPush.equals(value)) {
				CommonSubmit.ReSubmit(Common.kafkaSpoutToAlarmPush, Common.kafkaSpoutToAlarmPushJar);
			} else if (Common.kafkaSpoutToDb2.equals(value)) {
				CommonSubmit.ReSubmit(Common.kafkaSpoutToDb2, Common.kafkaSpoutToDb2Jar);
			} else if (Common.kafkaSpoutToGps.equals(value)) {
				CommonSubmit.ReSubmit(Common.kafkaSpoutToGps, Common.kafkaSpoutToGpsJar);
			} else if (Common.kafkaSpoutToHbase.equals(value)) {
				CommonSubmit.ReSubmit(Common.kafkaSpoutToHbase, Common.kafkaSpoutToGpsJar);
			}
		}

	}

	/**
	 * 调用thrift 接口 重启worker 目前测试接口不好使
	 */
	private static void restartWorker() {
		for (String _value : reStart) {
			System.out.println(_value);
		}
	}

	private static void MonitorStormTopology() throws AuthorizationException, TException, NotAliveException, IOException {
		Map conf = Utils.readStormConfig();
		// nimbus服务器地址
		conf.put(Config.NIMBUS_HOST, "10.10.11.11");
		// nimbus thrift地址
		conf.put(Config.NIMBUS_THRIFT_PORT, 6627);
		Nimbus.Client client = NimbusClient.getConfiguredClient(conf).getClient();
		ClusterSummary clusterInfo = client.getClusterInfo();
		List<TopologySummary> topologyList = clusterInfo.get_topologies();
		StringBuffer urlBuffer=null;
		TopologyStat Stat=null;
		for (TopologySummary topologySummary : topologyList) {
			String topology_name = topologySummary.get_name();
			String get_id = topologySummary.get_id();
			System.out.println(" 当前 topology is " + get_id);
			urlBuffer=new StringBuffer(Common.NIMBUS_UI);
			urlBuffer.append("/api/v1/topology/");
			TopologyInfo topologyInfo = client.getTopologyInfo(get_id);
			List<ExecutorSummary> get_executors = topologyInfo.get_executors();
			urlBuffer.append(get_id);
			Stat = SendUrlGet(urlBuffer.toString(), null);
		}
	}

	private static void parse(String topology_name, List<ExecutorSummary> get_executors) {
		for (ExecutorSummary tmp : get_executors) {
			String get_component_id = tmp.get_component_id();
			if ("kafka-spout".equals(get_component_id)) {
				// String restart = tmp.get_host() + ":" + tmp.get_port();
				ExecutorStats get_stats = tmp.get_stats();
				Map<String, Map<String, Long>> get_emitted = get_stats.get_emitted();
				printlnEmit(get_emitted);
				ExecutorSpecificStats get_specific = get_stats.get_specific();
				SpoutStats get_spout = get_specific.get_spout();
				Map<_Fields, FieldMetaData> metadatamap = get_spout.metaDataMap;
				Map<String, Map<String, Long>> get_acked = get_spout.get_acked();
				Map<String, Long> Miniutes_Time_Map = get_acked.get("600");
				printlnAckEmit(get_acked);
				if (Miniutes_Time_Map != null) {
					Long long1 = Miniutes_Time_Map.get("default");
					if (long1 == 0) {
						ErorTopology.add(topology_name);
					}
				}

			}
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

	private static boolean kill(String topologyName, Nimbus.Client client) throws NotAliveException, AuthorizationException, TException {
		boolean kill;
		KillOptions killOpts = new KillOptions();
		killOpts.set_wait_secs(5);
		client.killTopologyWithOpts(topologyName, killOpts);
		kill = true;
		System.out.println("killed " + topologyName);
		return kill;
	}

	public static Map SendUrlPost(String mapDecoderUrl, String paramString) throws IOException {
		List<String> result = new ArrayList<String>();
		String jsonStr = getHttpConnectResponse(mapDecoderUrl, paramString, "POST");
		Map responseList = JSON.parseObject(jsonStr, Map.class);
		return responseList;
	}

	public static TopologyStat SendUrlGet(String mapDecoderUrl, String paramString) throws IOException {
		List<String> result = new ArrayList<String>();
		String jsonStr = getHttpConnectResponse(mapDecoderUrl, paramString, "GET");
		TopologyStat responseList = JSON.parseObject(jsonStr, TopologyStat.class);
		return responseList;
	}

	public static String getHttpConnectResponse(String mapDecoderUrl, String paramString, String Type) throws IOException {
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

	private static byte[] readInputStream(InputStream inStream) throws IOException {
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
}
