package com.bhz.eps.dao;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.bhz.eps.entity.SaleItem;

public interface SaleItemDao {
	public void addSaleItem(SaleItem saleItem);
	public List<SaleItem> getSaleItembyId(@Param("id") String id);
}
