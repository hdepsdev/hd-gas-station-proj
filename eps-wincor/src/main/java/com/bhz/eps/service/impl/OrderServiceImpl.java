package com.bhz.eps.service.impl;

import javax.annotation.Resource;

import com.bhz.eps.dao.OrderMapper;
import com.bhz.eps.entity.Order;
import com.bhz.eps.service.OrderService;
import org.springframework.stereotype.Component;

@Component("orderService")
public class OrderServiceImpl implements OrderService{
	@Resource
	private OrderMapper orderMapper;
	
	@Override
	public void addOrder(Order order) {
        orderMapper.insert(order);
	}

	@Override
	public Order getOrderbyId(String orderId) {
		return orderMapper.selectByPrimaryKey(orderId);
	}

}
