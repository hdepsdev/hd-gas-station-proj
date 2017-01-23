package com.bhz.eps.processor;

import com.bhz.eps.DeviceService;
import com.bhz.eps.annotation.BizProcessorSpec;
import com.bhz.eps.entity.CardServiceRequest;
import com.bhz.eps.entity.Order;
import com.bhz.eps.msg.BizMessageType;
import com.thoughtworks.xstream.XStream;

@BizProcessorSpec(msgType=BizMessageType.CARDSVR_REQUEST)
public class CardServiceRequestProcessor extends BizProcessor {
	@Override
	public void process() {
		CardServiceRequest csr = (CardServiceRequest)this.getMsgObject();
		//生成订单,调用DeviceService中的方法，完成交易
		Order order = new Order();
		
		DeviceService ds = DeviceService.getInstance("localhost", 4050);
		
		try {
			ds.askBPosDisplay("正在支付，请稍后...");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
