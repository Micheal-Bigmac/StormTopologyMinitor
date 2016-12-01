package com.hsae.storm.submitter;

public class Detail {
	private String window;
	private Long emitted;
	private Long transferred;
	/**
	 * @return the window
	 */
	public String getWindow() {
		return window;
	}
	/**
	 * @param window the window to set
	 */
	public void setWindow(String window) {
		this.window = window;
	}
	/**
	 * @return the emitted
	 */
	public Long getEmitted() {
		return emitted;
	}
	/**
	 * @param emitted the emitted to set
	 */
	public void setEmitted(Long emitted) {
		this.emitted = emitted;
	}
	/**
	 * @return the transferred
	 */
	public Long getTransferred() {
		return transferred;
	}
	/**
	 * @param transferred the transferred to set
	 */
	public void setTransferred(Long transferred) {
		this.transferred = transferred;
	}

 }
