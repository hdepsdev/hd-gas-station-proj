package com.bhz.eps.service.impl;

import javax.annotation.Resource;

import com.bhz.eps.dao.ItemMapper;
import com.bhz.eps.entity.Item;
import com.bhz.eps.service.ItemService;
import org.springframework.stereotype.Component;

@Component("ItemService")
public class ItemServiceImpl implements ItemService{

	@Resource
    ItemMapper itemMapper;
	
	@Override
	public void addItem(Item item) {
        itemMapper.insert(item);
	}

	@Override
	public Item getItembyCode(String code) {
		return itemMapper.selectByPrimaryKey(code);
	}
}
