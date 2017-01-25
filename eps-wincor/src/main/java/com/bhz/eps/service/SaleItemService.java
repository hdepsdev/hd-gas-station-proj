package com.bhz.eps.service;

import java.util.List;

import com.bhz.eps.entity.SaleItem;

public interface SaleItemService {
	public void addSaleItem(SaleItem saleItem);
	public List<SaleItem> getSaleItemsbyOrderId(String orderId);
}
