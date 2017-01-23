package com.bhz.eps.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.bhz.eps.util.FormatedJsonHierarchicalStreamDriver;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;

import lombok.Data;

@XStreamAlias("order")
@Data
public class Order {
	@XStreamAlias("id")
	private String id;
	@XStreamAlias("merchant-id")
	private String merchantId;
	@XStreamAlias("merchant-name")
	private String merchantName;
	@XStreamAlias("generator")
	private String generator;
	@XStreamAlias("order-time")
	private long orderTime;
	@XStreamAlias("shift-number")
	private String shiftNumber;
	@XStreamAlias("clerk-id")
	private String clerkId;
	@XStreamAlias("total-amount")
	private BigDecimal totalAmount;
	
	@XStreamAlias("sale-items")
	private List<SaleItem> saleItems;
	
	@Data
	public static class SaleItem{
		@XStreamAlias("product-code")
		private String productCode;
		@XStreamAlias("amount")
		private BigDecimal amount;
		@XStreamAlias("unit-measure")
		private String unitMeasure;
		@XStreamAlias("unit-price")
		private BigDecimal unitPrice;
		@XStreamAlias("quantity")
		private BigDecimal quantity;
	}
	
	public static void main(String[] args) {
		Order order = new Order();
		order.setClerkId("A1");
		order.setMerchantId("6444");
		order.setMerchantName("北京望京");
		order.setGenerator("POS1|02");
		order.setOrderTime(1485101459L);
		order.setShiftNumber("2");
		order.setTotalAmount(new BigDecimal(340.00));
		List<Order.SaleItem> sis = new ArrayList<Order.SaleItem>();
		Order.SaleItem s1 = new Order.SaleItem();
		s1.setProductCode("0");
		s1.setAmount(new BigDecimal(200));
		s1.setUnitMeasure("LTR");
		s1.setUnitPrice(new BigDecimal(5));
		s1.setQuantity(new BigDecimal(40));
		
		Order.SaleItem s2 = new Order.SaleItem();
		s2.setProductCode("0");
		s2.setAmount(new BigDecimal(140));
		s2.setUnitMeasure("LTR");
		s2.setUnitPrice(new BigDecimal(7));
		s2.setQuantity(new BigDecimal(20));
		
		sis.add(s1);sis.add(s2);
		order.setSaleItems(sis);
		
		XStream x = new XStream(new FormatedJsonHierarchicalStreamDriver());
//		XStream x = new XStream();
		x.autodetectAnnotations(true);
		x.alias("order", Order.class);
		x.alias("SaleItem", Order.SaleItem.class);
		x.setMode(XStream.NO_REFERENCES);
		System.out.println(x.toXML(order));
	}
}
