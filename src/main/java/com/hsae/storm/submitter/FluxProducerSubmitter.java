package com.hsae.storm.submitter;

import org.apache.storm.flux.Flux;

public class FluxProducerSubmitter {

	public static String JAR_NAME = "tsp-storm-producer-0.1.0-SNAPSHOT.jar";
	public static int LOCAL_MODE = 1;
	public static Boolean NEED_REJAR = false;
    
	public static void main(String[] args) throws Exception {
		if (LOCAL_MODE == 1) {
			Flux.main(new String[] { "-l", "src/main/resources/write2kafka.yaml", "-s", "300000", "-f", "src/main/resources/system.properties" });
		} else {
			String url = FluxProducerSubmitter.class.getResource("/").toString();
			String targeturl = url.substring(0, url.lastIndexOf("/"));
			targeturl = targeturl.substring(6, targeturl.lastIndexOf("/"));
			boolean packageViaMaven = false;
			if (NEED_REJAR) {
				String projecturl = targeturl.substring(0, targeturl.lastIndexOf("/"));
				//packageViaMaven = JarTool.packageViaMvn(projecturl);
			}
			if (!NEED_REJAR || packageViaMaven) {
				String jarpath = targeturl + "/" + JAR_NAME;
				// 非常关键的一步，使用StormSubmitter提交拓扑时，不管怎么样，都是需要将所需的jar提交到nimbus上去，如果不指定jar文件路径，
				// storm默认会使用System.getProperty("storm.jar")去取，如果不设定，就不能提交
				System.setProperty("storm.jar", jarpath);
				// StormSubmitter.submitTopologyWithProgressBar(t.getName(), config, t.getTopology());
				Flux.main(new String[] { "-r", "-s", "600000000", "src/main/resources/write2kafka.yaml", "-f", "src/main/resources/system.properties" });
			} else {
				throw new Exception("create jar fail error!");
			}
		}
	}
}
