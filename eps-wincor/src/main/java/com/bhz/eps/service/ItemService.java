package com.bhz.eps.service;

import com.bhz.eps.entity.Item;

public interface ItemService {
	public void addItem(Item item);
	public Item getItembyCode(String code);
}
