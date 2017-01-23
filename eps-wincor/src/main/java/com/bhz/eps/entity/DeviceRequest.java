package com.bhz.eps.entity;

import java.util.List;

import lombok.Data;

@Data
public class DeviceRequest {
	private String requestType;
	private String applicationSender;
	private String workstationId;
	private String popId;
	private String terminalId;
	private String requestId;
	private String sequenceId;
	private Output output;
	
	@Data
	public static class Output{
		private String outDeviceTarget;
		private List<TextLine> textLines;
	}
	
	@Data
	public static class TextLine{
		private String erase;
		private String timeout;
		private String menuItem;
	}
	
	@Data
	public static class Input{
		private String inDeviceTarget;
		private String inResult;
		private String command;
	}
}
