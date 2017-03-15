package com.bhz.eps.entity;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import lombok.Data;

@Data
public class PayPassword {
	
	@XStreamAlias("TxCode")
	private String txCode;
	
	@XStreamAlias("TxDate")
	private String txDate;
	
	@XStreamAlias("MerchantNo")
	private String merchantNo="1";
	
	@XStreamAlias("ReqSeqNo")
	private String reqSeqNo;
	
	@XStreamAlias("TxTime")
	private String txTime;
	
	@XStreamAlias("BrchNo")
	private String brchNo="1";
	
	@XStreamAlias("BrchName")
	private String brchName="Common";
	
	@XStreamAlias("TellerNo")
	private String tellerNo="1";
	
	@XStreamAlias("TellerName")
	private String tellerName="Common";
	
	@XStreamAlias("TxMac")
	private String txMac;
	
	@XStreamAlias("ReqBody")
	private ReqBody reqBody;
	
	@XStreamAlias("ReqBody")
	@Data
	public static class ReqBody{
		
		@XStreamAlias("CardNo")
		private String cardNo;
		
		@XStreamAlias("Password")
		private String password;
		
	}
	
}
