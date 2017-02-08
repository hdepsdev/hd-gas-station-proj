package com.bhz.eps.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import com.bhz.eps.EPSServer;
import com.bhz.eps.dao.OrderMapper;
import com.bhz.eps.dao.SaleItemMapper;
import com.bhz.eps.entity.CardServiceRequest;
import com.bhz.eps.entity.Order;
import com.bhz.eps.entity.SaleItemEntity;
import com.bhz.eps.service.SaleItemService;
import com.bhz.eps.util.Utils;
import org.springframework.stereotype.Component;

@Component("saleItemService")
public class SaleItemServiceImpl implements SaleItemService {

	@Resource
    SaleItemMapper saleItemMapper;
    @Resource
    private OrderMapper orderMapper;
	
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

    @Override
    public void saveSaleItems(Order order, List<CardServiceRequest.SaleItem> saleItemList){
        orderMapper.insert(order);
        for(com.bhz.eps.entity.CardServiceRequest.SaleItem item : saleItemList){
            com.bhz.eps.entity.SaleItemEntity si = new com.bhz.eps.entity.SaleItemEntity();
            si.setId(Utils.generateCompactUUID());
            si.setProductCode(item.getProductCode());
            si.setUnitMeasure(item.getUnitMeasure());
            si.setUnitPrice(item.getUnitPrice());
            si.setQuantity(item.getQuantity());
            si.setItemSeq(item.getItemId());
            si.setTaxCode(item.getTaxCode());
            si.setOrderId(order.getOrderId());
            si.setAmount(item.getAmount());
            addSaleItem(si);
        }
    }
}
