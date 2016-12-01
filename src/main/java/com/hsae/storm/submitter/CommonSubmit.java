package com.hsae.storm.submitter;
import org.apache.storm.flux.Flux;
import org.apache.storm.shade.org.eclipse.jetty.util.log.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hsae.storm.spout.KafkaProducerSpout;

public class CommonSubmit {

	public static int LOCAL_MODE = 0;
	public static Boolean NEED_REJAR = false;
	private static final Logger LOG = LoggerFactory.getLogger(CommonSubmit.class);

	public static void ReSubmit(String topology,String JAR_NAME) throws Exception {
		if (LOCAL_MODE ==1) {
			if(Common.kafkaSpoutToAlarmPush.equals(topology)){
				Flux.main(new String[] { "-l", "-s", "600000000", "src/main/resources/kafkaSpoutToAlarmPush.yaml", "-f", "src/main/resources/system.properties" });
			}else if(Common.kafkaSpoutToDb2.equals(topology)){
				Flux.main(new String[] { "-l", "-s", "600000000", "src/main/resources/kafkaSpoutToDb2.yaml", "-f", "src/main/resources/system.properties" });
			}else if(Common.kafkaSpoutToGps.equals(topology)){
				Flux.main(new String[] { "-l", "-s", "600000000", "src/main/resources/kafkaSpoutToGps.yaml", "-f", "src/main/resources/system.properties" });
			}else if(Common.kafkaSpoutToHbase.equals(topology)){
				Flux.main(new String[] { "-l", "-s", "600000000", "src/main/resources/kafkaSpoutToHbase.yaml", "-f", "src/main/resources/system.properties" });
			}else if(Common.backupZmqkafka.equals(topology)){
				Flux.main(new String[] { "-l","-s", "600000000", "src/main/resources/write2kafka.yaml",  "-f", "src/main/resources/system.properties" });
			}else{
				LOG.info("<<<<<<<<< there is not such topology >>>>>>>>>>>>");
			}
		} else {
			String url = CommonSubmit.class.getResource("/").toString();
			String targeturl = url.substring(0, url.lastIndexOf("/"));
			targeturl = targeturl.substring(6, targeturl.lastIndexOf("/"));
			boolean packageViaMaven = false;
			if (NEED_REJAR) {
				String projecturl = targeturl.substring(0, targeturl.lastIndexOf("/"));
			}
			if (!NEED_REJAR || packageViaMaven) {
				String jarpath =targeturl + "/" + JAR_NAME;//线下测试
				Log.info("Jar Path :"+jarpath);
//				String jarpath=Common.dirPath+"/"+JAR_NAME;
				// 非常关键的一步，使用StormSubmitter提交拓扑时，不管怎么样，都是需要将所需的jar提交到nimbus上去，如果不指定jar文件路径，
				// storm默认会使用System.getProperty("storm.jar")去取，如果不设定，就不能提交
				System.setProperty("storm.jar", jarpath);
				if(Common.kafkaSpoutToAlarmPush.equals(topology)){
					Flux.main(new String[] { "-r", "-s", "600000000", Common.dirPath+"src/main/resources/kafkaSpoutToAlarmPush.yaml", "-f", Common.dirPath+"src/main/resources/system-offline.properties" });
				}else if(Common.kafkaSpoutToDb2.equals(topology)){
					Flux.main(new String[] { "-r", "-s", "600000000",Common.dirPath+"src/main/resources/kafkaSpoutToDb2.yaml", "-f", Common.dirPath+"src/main/resources/system-offline.properties" });
				}else if(Common.kafkaSpoutToGps.equals(topology)){
					Flux.main(new String[] { "-r", "-s", "600000000", Common.dirPath+"src/main/resources/kafkaSpoutToGps.yaml", "-f", Common.dirPath+"src/main/resources/system-offline.properties" });
				}else if(Common.kafkaSpoutToHbase.equals(topology)){
					Flux.main(new String[] { "-r", "-s", "600000000", Common.dirPath+"src/main/resources/kafkaSpoutToHbase.yaml", "-f", Common.dirPath+"src/main/resources/system-offline.properties" });
				}else if(Common.backupZmqkafka.equals(topology)){
					Flux.main(new String[] { "-r", "-s", "600000000", Common.dirPath+"src/main/resources/write2kafka-offline.yaml", "-f",Common.dirPath+"src/main/resources/system-offline.properties" });
				}
			} else {
				throw new Exception("create jar fail error!");
			}
		}
	}

}
