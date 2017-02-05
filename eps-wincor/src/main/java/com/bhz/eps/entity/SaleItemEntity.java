package com.bhz.eps.entity;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

public class SaleItemEntity implements java.io.Serializable {
	private static final long serialVersionUID = 7926713170885076453L;

	@Getter
	@Setter
	private String id;

	@Getter
	@Setter
	private String productCode;

	@Getter
	@Setter
	private String unitMeasure;

	@Getter
	@Setter
	private BigDecimal unitPrice;

	@Getter
	@Setter
	private BigDecimal quantity;

	@Getter
	@Setter
	private String itemCode;

	@Getter
	@Setter
	private String orderId;

	@Getter
	@Setter
	private BigDecimal amount;

	// Virtual Property
	@Getter
	@Setter
	private String code;
	@Getter
	@Setter
	private String itemName;
	@Getter
	@Setter
	private String itemCatalog;
}
