package com.bhz.eps.dao;

import com.bhz.eps.entity.SaleItemEntity;

import java.util.List;
import java.util.Map;

public interface SaleItemMapper {
    int deleteByPrimaryKey(String id);

    int deleteByOrderId(String orderId);

    int insert(SaleItemEntity record);

    int insertSelective(SaleItemEntity record);

    SaleItemEntity selectByPrimaryKey(String id);

    int updateByPrimaryKeySelective(SaleItemEntity record);

    int updateByPrimaryKey(SaleItemEntity record);

    List<SaleItemEntity> selectByParam(Map param);
}