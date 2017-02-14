package com.bhz.eps.service;

import com.bhz.eps.entity.Order;

public interface OrderService {
	public void addOrder(Order order);
    public int deleteByPrimaryKey(String orderId);
	public Order getOrderbyId(String orderId);
	public Order getOrderWithSaleItemsById(String orderId);
}
