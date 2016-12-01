package com.hsae.storm.submitter;

import java.util.ArrayList;
import java.util.List;

public class TopologyStat {
	private List<Detail> topologyStats=new ArrayList<Detail>();

	/**
	 * @return the topologyStats
	 */
	public List<Detail> getTopologyStats() {
		return topologyStats;
	}

	/**
	 * @param topologyStats the topologyStats to set
	 */
	public void setTopologyStats(List<Detail> topologyStats) {
		this.topologyStats = topologyStats;
	}
}
