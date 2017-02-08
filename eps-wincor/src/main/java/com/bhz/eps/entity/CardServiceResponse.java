package com.bhz.eps.entity;

import java.math.BigDecimal;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.converters.extended.ToAttributedValueConverter;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@XStreamAlias("CardServiceResponse")
public class CardServiceResponse implements java.io.Serializable {
	private static final long serialVersionUID = 3464249525124250794L;
	@XStreamAlias("RequestType")
	@XStreamAsAttribute
	@Getter @Setter
	private String requestType = "CardPayment";
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

    @XStreamAlias("Terminal")
	@Getter @Setter
	private Terminal terminal;

	public static class Terminal{
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
	@Getter @Setter
	private Tender tender;

	public static class Tender{
		@XStreamAlias("LanguageCode")
		@XStreamAsAttribute
		@Getter @Setter
		private String languageCode = "zh";

        @XStreamConverter(value=ToAttributedValueConverter.class,strings={"totalAmount"})
        @XStreamAlias("TotalAmount")
		@Getter @Setter
		private TotalAmount totalAmount;

        @Data
		public static class TotalAmount{
			@XStreamAlias("PaymentAmount")
			@XStreamAsAttribute
			@Getter @Setter
			private BigDecimal paymentAmount;
			@XStreamAlias("CashBackAmount")
			@XStreamAsAttribute
			@Getter @Setter
			private BigDecimal cashBackAmount;
			@XStreamAlias("RebateAmount")
			@XStreamAsAttribute
			@Getter @Setter
			private BigDecimal rebateAmount;
			@XStreamAlias("OriginalAmount")
			@XStreamAsAttribute
			@Getter @Setter
			private BigDecimal originalAmount;
			@XStreamAlias("Currency")
			@XStreamAsAttribute
			@Getter @Setter
			private String currency = "RMB";
			
			@Getter @Setter
			private BigDecimal totalAmount;
		}

        @XStreamAlias("Authorisation")
		@Getter @Setter
		private Authorisation authorisation;

		public static class Authorisation{
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
	}

    @XStreamAlias("CardValue")
	@Getter @Setter
	private CardValue cardValue;

	public static class CardValue{
		@Getter @Setter
		private Track2 track2;
		
		@XStreamAlias("Track2")
		public static class Track2{
			@XStreamAlias("Ascii")
			@XStreamAsAttribute
			@Getter @Setter
			private String ascii;
			@XStreamAlias("CardPAN")
			@XStreamAsAttribute
			@Getter @Setter
			private String cardPAN;
		}
	}
	
}

