package com.bhz.eps.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import com.bhz.eps.dao.SaleItemMapper;
import com.bhz.eps.entity.SaleItemEntity;
import com.bhz.eps.service.SaleItemService;
import org.springframework.stereotype.Component;

@Component("saleItemService")
public class SaleItemServiceImpl implements SaleItemService {

	@Resource
    SaleItemMapper saleItemMapper;
	
	@Override
	public void addSaleItem(SaleItemEntity saleItem) {
        saleItemMapper.insert(saleItem);
	}

	@Override
	public List<SaleItemEntity> getSaleItemsbyOrderId(String orderId) {
        Map param = new HashMap<>();
        param.put("orderId", orderId);
		return saleItemMapper.selectByParam(param);
	}

}
