package com.bhz.eps.entity;

import lombok.Data;

@Data
public class DeviceResponse {
	private String requestType;
	private String applicationSender;
	private String workstationId;
	private String popId;
	private String terminalId;
	private String requestId;
	private String sequenceId;
	private String overallResult;
	
	@Data
	public static class Output{
		private String outDeviceTarget;
		private String outResult;
	}
	
	@Data
	public static class Input{
		private String inDeviceTarget;
		private String inResult;
		private InputValue inputValue;
	}
	
	@Data
	public static class InputValue{
		private String inNumber;
	}
}
