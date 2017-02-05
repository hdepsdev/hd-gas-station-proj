package com.bhz.eps.entity;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

public class Item implements java.io.Serializable{
	private static final long serialVersionUID = 253906985343295769L;
	
	@Getter@Setter
	private String code;
	
	@Getter@Setter
	private String name;
	
	@Getter@Setter
	private int catalog;

}
