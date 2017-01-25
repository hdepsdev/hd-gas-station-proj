package com.bhz.eps.service.impl;

import javax.annotation.Resource;

import com.bhz.eps.dao.OrderDao;
import com.bhz.eps.entity.Order;
import com.bhz.eps.service.OrderService;

public class OrderServiceImpl implements OrderService{
	@Resource
	private OrderDao orderdao;
	
	@Override
	public void addOrder(Order order) {
		orderdao.addOrder(order);		
	}

	@Override
	public Order getOrderbyId(String orderId) {
		return orderdao.getOrderbyId(orderId);
	}

}
