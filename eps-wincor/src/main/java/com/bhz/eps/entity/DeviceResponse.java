package com.bhz.eps.entity;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

import lombok.Data;

@XStreamAlias("DeviceResponse")
@Data
public class DeviceResponse {
	@XStreamAlias("RequestType")
	@XStreamAsAttribute
	private String requestType;
	@XStreamAlias("ApplicationSender")
	@XStreamAsAttribute
	private String applicationSender;
	@XStreamAlias("WorkstationID")
	@XStreamAsAttribute
	private String workstationId;
	@XStreamAlias("POPID")
	@XStreamAsAttribute
	private String popId;
	@XStreamAlias("TerminalID")
	@XStreamAsAttribute
	private String terminalId;
	@XStreamAlias("RequestID")
	@XStreamAsAttribute
	private String requestId;
	@XStreamAlias("SequenceID")
	@XStreamAsAttribute
	private String sequenceId;
	@XStreamAlias("OverallResult")
	private String overallResult;
	
	@XStreamAlias("Output")
	@Data
	public static class Output{
		@XStreamAlias("OutDeviceTarget")
		@XStreamAsAttribute
		private String outDeviceTarget;
		@XStreamAlias("OutResult")
		@XStreamAsAttribute
		private String outResult;
	}
	
	@Data
	public static class Input{
		@XStreamAlias("InDeviceTarget")
		@XStreamAsAttribute
		private String inDeviceTarget;
		@XStreamAlias("InResult")
		@XStreamAsAttribute
		private String inResult;
		
		private InputValue inputValue;
	}
	
	@XStreamAlias("InputValue")
	@Data
	public static class InputValue{
		@XStreamAlias("InNumber")
		private String inNumber;
	}
}
