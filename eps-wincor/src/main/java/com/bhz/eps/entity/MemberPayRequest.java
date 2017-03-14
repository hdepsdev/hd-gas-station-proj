package com.bhz.eps.entity;

import java.util.ArrayList;
import java.util.List;

import com.bhz.eps.util.FormatedJsonHierarchicalStreamDriver;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;

import lombok.Data;

@Data
public class MemberPayRequest {
	
	@XStreamAlias("TxCode")
	private String txCode;
	
	@XStreamAlias("TxDate")
	private String txDate;
	
	@XStreamAlias("MerchantNo")
	private String merchantNo;
	
	@XStreamAlias("ReqSeqNo")
	private String reqSeqNo;
	
	@XStreamAlias("TxTime")
	private String txTime;
	
	@XStreamAlias("BrchNo")
	private String brchNo;
	
	@XStreamAlias("BrchName")
	private String brchName;
	
	@XStreamAlias("TellerNo")
	private String tellerNo;
	
	@XStreamAlias("TellerName")
	private String tellerName;
	
	@XStreamAlias("TxMac")
	private String txMac;
	
	@XStreamAlias("ReqBody")
	private ReqBody reqBody;
	
	@XStreamAlias("ReqBody")
	@Data
	public static class ReqBody{
		
		@XStreamAlias("CardNo")
		private String cardNo;
		
		@XStreamAlias("OrgID")
		private String orgID;
		
		@XStreamAlias("FillingTime")
		private String fillingTime;
		
		@XStreamAlias("DeductionType")
		private String deductionType;
		
		@XStreamAlias("TotalAmount")
		private String totalAmount;
		
		@XStreamAlias("NumberOfDetail")
		private String numberOfDetail;
		
		@XStreamAlias("ExpandFlowList")
		private List<ExpandFlow> expandFlowList;
	}
	
	@XStreamAlias("ExpandFlowList")
	@Data
	public static class ExpandFlow{
		@XStreamAlias("LimitOilNo")
		private String limitOilNo;
		
		@XStreamAlias("OilPrices")
		private String oilPrices;
		
		@XStreamAlias("Refueling")
		private String refueling;
		
		@XStreamAlias("Amount")
		private String amount;
		
		@XStreamAlias("ICCardBalance")
		private String iCCardBalance;
		
		@XStreamAlias("ListNo")
		private String listNo;
		
		@XStreamAlias("PriceNoDiscount")
		private String priceNoDiscount;
		
		@XStreamAlias("AmountNoDiscount")
		private String amountNoDiscount;
		
		@XStreamAlias("PlatesNumber")
		private String platesNumber;
		
		@XStreamAlias("Shift")
		private String shift;
	}
	
	
	public static void main(String[] args) {
		MemberPayRequest pay = new MemberPayRequest();
		pay.setTxCode("3103");
		pay.setTxDate("20170314");
		pay.setMerchantNo("100");
		pay.setReqSeqNo("");
		pay.setTxTime("");
		pay.setBrchNo("");
		pay.setBrchName("");
		pay.setTellerNo("");
		pay.setTellerName("");
		pay.setTxMac("");
		ReqBody body = new ReqBody();
		body.setCardNo("");
		body.setOrgID("");
		body.setFillingTime("");
		body.setDeductionType("");
		body.setTotalAmount("");
		body.setNumberOfDetail("");
		
		
		List<ExpandFlow> eflist = new ArrayList<ExpandFlow>();
		ExpandFlow ef = new ExpandFlow();
		ef.setLimitOilNo("");
		ef.setOilPrices("");
		ef.setRefueling("");
		ef.setAmount("");
		ef.setICCardBalance("");
		ef.setListNo("");
		ef.setPriceNoDiscount("");
		ef.setAmountNoDiscount("");
		ef.setPlatesNumber("");
		ef.setShift("");
		eflist.add(ef);
		
		body.setExpandFlowList(eflist);
		pay.setReqBody(body);
		
		
		
		
		XStream x = new XStream(new FormatedJsonHierarchicalStreamDriver());
//		XStream x = new XStream();
		x.autodetectAnnotations(true);
//		x.alias("order", Order.class);
//		x.alias("SaleItem", Order.SaleItem.class);
		x.setMode(XStream.NO_REFERENCES);
		System.out.println(x.toXML(pay));
	}
}
