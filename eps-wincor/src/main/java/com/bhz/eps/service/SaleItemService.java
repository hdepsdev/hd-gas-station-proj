package com.bhz.eps.service;

import java.util.List;

import com.bhz.eps.entity.CardServiceRequest;
import com.bhz.eps.entity.Order;
import com.bhz.eps.entity.SaleItemEntity;

public interface SaleItemService {
	public void addSaleItem(SaleItemEntity saleItem);
	public List<SaleItemEntity> getSaleItemsbyOrderId(String orderId);
    public void saveSaleItems(Order order, List<CardServiceRequest.SaleItem> saleItemList);
}
