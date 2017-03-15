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
    public int deleteByPrimaryKey(String orderId) {
        return orderMapper.deleteByPrimaryKey(orderId);
    }

	@Override
	public Order getOrderbyId(String orderId) {
		return orderMapper.selectByPrimaryKey(orderId);
	}

	@Override
	public Order getOrderWithSaleItemsById(String orderId) {
		return orderMapper.getOrderWithItemsById(orderId);
	}

    @Override
    public int updateOrder(Order order) {
        return orderMapper.updateByPrimaryKey(order);
    }
    
    @Override
    public int updateByPrimaryKeySelective(Order order){
    	 return orderMapper.updateByPrimaryKeySelective(order);
    }
}
