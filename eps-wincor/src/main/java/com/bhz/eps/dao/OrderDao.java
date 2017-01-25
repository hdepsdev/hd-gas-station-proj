package com.bhz.eps.dao;

import org.apache.ibatis.annotations.Param;

import com.bhz.eps.entity.Order;

public interface OrderDao {
	public void addOrder(Order order);
	public Order getOrderbyId(@Param("orderId") String orderId);
}
