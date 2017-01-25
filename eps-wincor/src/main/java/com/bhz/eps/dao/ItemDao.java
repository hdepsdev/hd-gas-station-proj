package com.bhz.eps.dao;

import org.apache.ibatis.annotations.Param;
import com.bhz.eps.entity.Item;

public interface ItemDao {
	public void addItem(Item item);
	public Item getItembyCode(@Param("code")String code);	
}
