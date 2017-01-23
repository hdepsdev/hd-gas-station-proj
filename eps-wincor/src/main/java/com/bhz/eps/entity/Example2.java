package com.bhz.eps.entity;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;

import com.thoughtworks.xstream.XStream;

public class Example2 {
	
	private static XStream xstream;
	static {
		xstream = new XStream();
		xstream.autodetectAnnotations(true);
	}
	
	public static void main(String[] args) throws FileNotFoundException {
		InputStreamReader reader = new InputStreamReader(new FileInputStream("/Users/yaoh/tmp/card_request.xml"));
		xstream.alias("CardServiceRequest", CardServiceRequest.class);
		xstream.alias("POSdata", CardServiceRequest.PosData.class);
		xstream.alias("SaleItem", CardServiceRequest.SaleItem.class);
		CardServiceRequest request = (CardServiceRequest) xstream.fromXML(reader);
		System.out.println(request.getSaleItemList().get(0).getQuantity().toString());
	}
}
