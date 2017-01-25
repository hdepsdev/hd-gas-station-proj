package com.bhz.eps.dao;

import com.bhz.eps.entity.Item;

public interface ItemMapper {
    int deleteByPrimaryKey(String code);

    int insert(Item record);

    int insertSelective(Item record);

    Item selectByPrimaryKey(String code);

    int updateByPrimaryKeySelective(Item record);

    int updateByPrimaryKey(Item record);
}