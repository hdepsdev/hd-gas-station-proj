package com.bhz.eps.service.impl;

import javax.annotation.Resource;

import com.bhz.eps.dao.ItemDao;
import com.bhz.eps.entity.Item;
import com.bhz.eps.service.ItemService;

public class ItemServiceImpl implements ItemService{

	@Resource
	ItemDao itemdao;
	
	@Override
	public void addItem(Item item) {
		itemdao.addItem(item);		
	}

	@Override
	public Item getItembyCode(String code) {
		return itemdao.getItembyCode(code);
	}
}
