package com.bhz.eps.entity;

import java.io.Serializable;

import lombok.Data;

@Data
public class PayMethod implements Serializable {
	private static final long serialVersionUID = -8814066607858484056L;
	public PayMethod(){};
	public PayMethod(int code,String name){
		setPayMethodCode(code);
		setPayMethodName(name);
	}
	private int payMethodCode;
	private String payMethodName;
}
