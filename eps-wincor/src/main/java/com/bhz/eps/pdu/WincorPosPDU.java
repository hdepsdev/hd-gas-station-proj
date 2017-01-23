package com.bhz.eps.pdu;

import lombok.Getter;
import lombok.Setter;

public class WincorPosPDU {
	public final static String XML_HEADER = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n";
	@Getter @Setter
	private long length;
	@Getter @Setter
	private String content;
	
	public WincorPosPDU(){
		
	}
	
	public WincorPosPDU(long length, String content){
		this.length = length;
		this.content = content;
	}
}
