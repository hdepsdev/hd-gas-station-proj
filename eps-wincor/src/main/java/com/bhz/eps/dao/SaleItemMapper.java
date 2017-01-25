package com.bhz.eps.dao;

import com.bhz.eps.entity.SaleItem;

import java.util.List;
import java.util.Map;

public interface SaleItemMapper {
    int deleteByPrimaryKey(String id);

    int insert(SaleItem record);

    int insertSelective(SaleItem record);

    SaleItem selectByPrimaryKey(String id);

    int updateByPrimaryKeySelective(SaleItem record);

    int updateByPrimaryKey(SaleItem record);

    List<SaleItem> selectByParam(Map param);
}