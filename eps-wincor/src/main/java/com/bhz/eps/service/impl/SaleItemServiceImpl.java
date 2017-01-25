package com.bhz.eps.service.impl;

import java.util.List;

import javax.annotation.Resource;

import com.bhz.eps.dao.SaleItemDao;
import com.bhz.eps.entity.SaleItem;
import com.bhz.eps.service.SaleItemService;

public class SaleItemServiceImpl implements SaleItemService {

	@Resource
	SaleItemDao saleItemDao;
	
	@Override
	public void addSaleItem(SaleItem saleItem) {
		saleItemDao.addSaleItem(saleItem);
	}

	@Override
	public List<SaleItem> getSaleItemsbyOrderId(String orderId) {
		return saleItemDao.getSaleItembyId(orderId);
	}

}
