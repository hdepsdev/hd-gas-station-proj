package com.bhz.eps.entity;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;

public class BizMessage implements java.io.Serializable{
	private static final long serialVersionUID = 4173509056300309690L;
	@Getter @Setter
	private int cmd;
	@Getter @Setter
	private Map<String,Object> content;
}
