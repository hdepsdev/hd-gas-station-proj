package com.bhz.eps.entity;

import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import com.thoughtworks.xstream.converters.extended.ToAttributedValueConverter;

import lombok.Data;

@XStreamAlias("DeviceRequest")
@Data
public class DeviceRequest {
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

    @XStreamAlias("Output")
	private Output output;

	@Data
	public static class Output{
		@XStreamAlias("OutDeviceTarget")
		private String outDeviceTarget;
		@XStreamImplicit
		private List<TextLine> textLines;
	}
	
	@XStreamConverter(value=ToAttributedValueConverter.class,strings={"content"})
	@XStreamAlias("TextLine")
	@Data
	public static class TextLine{
		@XStreamAlias("Erase")
		@XStreamAsAttribute
		private String erase;
		@XStreamAlias("TimeOut")
		@XStreamAsAttribute
		private String timeout;
		@XStreamAlias("MenuItem")
		@XStreamAsAttribute
		private String menuItem;
		private String content;
	}
	
	@XStreamConverter(value=ToAttributedValueConverter.class,strings={"command"})
	@XStreamAlias("Input")
	@Data
	public static class Input{
		@XStreamAlias("InDeviceTarget")
		@XStreamAsAttribute
		private String inDeviceTarget;
		@XStreamAlias("InResult")
		@XStreamAsAttribute
		private String inResult;
		private String command;
	}
}
