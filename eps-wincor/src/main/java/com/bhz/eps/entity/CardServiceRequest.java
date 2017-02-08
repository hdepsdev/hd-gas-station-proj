package com.bhz.eps.entity;

import java.math.BigDecimal;
import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

import lombok.Getter;
import lombok.Setter;

@XStreamAlias("CardServiceRequest")
public class CardServiceRequest implements java.io.Serializable{
	private static final long serialVersionUID = -3395151870249830713L;
	
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
	
	@XStreamAlias("POSdata")
	@Getter @Setter
	private PosData posData;
	
	@XStreamAlias("TotalAmount")
	@Getter @Setter
	private BigDecimal totalAmount;
	
	@XStreamImplicit
	@Getter @Setter
	private List<SaleItem> saleItemList;
	
	public static class PosData{
		
		@XStreamAsAttribute
		@XStreamAlias("LanguageCode")
		@Getter @Setter
		private String languageCode;
		@XStreamAlias("POSTimeStamp")
		@Getter @Setter
		private String posTimestamp;
		@XStreamAlias("ShiftNumber")
		@Getter @Setter
		private String shiftNumber;
		@XStreamAlias("ClerkID")
		@Getter @Setter
		private String ClerkID;
	}
	
	
	@XStreamAlias("SaleItem")
	public static class SaleItem{
		@XStreamAsAttribute
		@XStreamAlias("ItemID")
		@Getter @Setter
		private int itemId;
		@XStreamAlias("ProductCode")
		@Getter @Setter
		private String productCode;
		@XStreamAlias("Amount")
		@Getter @Setter
		private BigDecimal amount;
		@XStreamAlias("UnitMeasure")
		@Getter @Setter
		private String unitMeasure;
		@XStreamAlias("UnitPrice")
		@Getter @Setter
		private BigDecimal unitPrice;
		@XStreamAlias("Quantity")
		@Getter @Setter
		private BigDecimal quantity;
		@XStreamAlias("TaxCode")
		@Getter @Setter
		private String taxCode;
		@XStreamAlias("AdditionalProductCode")
		@Getter @Setter
		private String additionalProductCode;
		@XStreamAlias("SaleChannel")
		@Getter @Setter
		private String saleChannel;
		@XStreamAlias("AdditionalProductInfo")
		@Getter @Setter
		private String addtionalProductInfo;
	}
	
}

