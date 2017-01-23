package com.bhz.eps.entity;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.converters.extended.ToAttributedValueConverter;

import lombok.Getter;
import lombok.Setter;

@XStreamAlias("CardServiceResponse")
public class CardServiceResponse implements java.io.Serializable {
	private static final long serialVersionUID = 3464249525124250794L;
	@XStreamAlias("RequestType")
	@XStreamAsAttribute
	@Getter @Setter
	private String requestType;
	@XStreamAlias("ApplicationSender")
	@XStreamAsAttribute
	@Getter @Setter
	private String applicationSender;
	@XStreamAlias("WorkstationID")
	@XStreamAsAttribute
	@Getter @Setter
	private String workstationId;
	@XStreamAlias("POPID")
	@XStreamAsAttribute
	@Getter @Setter
	private String popId;
	@XStreamAlias("RequestID")
	@XStreamAsAttribute
	@Getter @Setter
	private String requestId;
	@XStreamAlias("OverallResult")
	@XStreamAsAttribute
	@Getter @Setter
	private String overallResult;
	
	@Getter @Setter
	private Terminal terminal;
	
	@Getter @Setter
	private Tender tender;
	
	@Getter @Setter
	private CardValue cardValue;
	
}

@XStreamAlias("Terminal")
class Terminal{
	@XStreamAlias("TerminalID")
	@XStreamAsAttribute
	@Getter @Setter
	private String terminalId;
	@XStreamAlias("TerminalBatch")
	@XStreamAsAttribute
	@Getter @Setter
	private String terminalBatch;
	@XStreamAlias("STAN")
	@XStreamAsAttribute
	@Getter @Setter
	private String stan;
	@XStreamAlias("ApplicationID")
	@XStreamAsAttribute
	@Getter @Setter
	private String applicationId;
}

@XStreamAlias("Tender")
class Tender{
	@XStreamAlias("LanguageCode")
	@XStreamAsAttribute
	@Getter @Setter
	private String languageCode;
	
	@Getter @Setter
	private TotalAmount totalAmount;
	
	@Getter @Setter
	private Authorisation authorisation;
}

@XStreamConverter(value=ToAttributedValueConverter.class,strings={"totalAmount"})
@XStreamAlias("TotalAmount")
class TotalAmount{
	@XStreamAlias("Currency")
	@XStreamAsAttribute
	@Getter @Setter
	private String currency;
	
	@Getter @Setter
	private String totalAmount;
}

@XStreamAlias("Authorisation")
class Authorisation{
	@XStreamAlias("AcquirerID")
	@XStreamAsAttribute
	@Getter @Setter
	private String acquirerid;
	@XStreamAlias("CardPAN")
	@XStreamAsAttribute
	@Getter @Setter
	private String cardPAN;
	@XStreamAlias("StartDate")
	@XStreamAsAttribute
	@Getter @Setter
	private String startDate;
	@XStreamAlias("ExpiryDate")
	@XStreamAsAttribute
	@Getter @Setter
	private String expireDate;
	@XStreamAlias("TimeStamp")
	@XStreamAsAttribute
	@Getter @Setter
	private String timeStamp;
	@XStreamAlias("ActionCode")
	@XStreamAsAttribute
	@Getter @Setter
	private String actionCode;
	@XStreamAlias("ApprovalCode")
	@XStreamAsAttribute
	@Getter @Setter
	private String approvalCode;
	@XStreamAlias("AcquirerBatch")
	@XStreamAsAttribute
	@Getter @Setter
	private String acquirerBatch;
	@XStreamAlias("CardCircuit")
	@XStreamAsAttribute
	@Getter @Setter
	private String cardCircuit;
	@XStreamAlias("ReceiptNumber")
	@XStreamAsAttribute
	@Getter @Setter
	private String receiptNumber;
}

@XStreamAlias("CardValue")
class CardValue{
	@Getter @Setter
	private Track2 track2;
}

@XStreamAlias("Track2")
class Track2{
	@XStreamAlias("Ascii")
	@XStreamAsAttribute
	@Getter @Setter
	private String ascii;
	@XStreamAlias("CardPAN")
	@XStreamAsAttribute
	@Getter @Setter
	private String cardPAN;
}


