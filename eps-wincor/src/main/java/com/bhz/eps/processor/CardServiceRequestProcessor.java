package com.bhz.eps.processor;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.bhz.eps.DeviceService;
import com.bhz.eps.EPSServer;
import com.bhz.eps.annotation.BizProcessorSpec;
import com.bhz.eps.entity.CardServiceRequest;
import com.bhz.eps.entity.Order;
import com.bhz.eps.msg.BizMessageType;
import com.bhz.eps.service.OrderService;
import com.bhz.eps.service.SaleItemService;
import com.bhz.eps.util.Utils;

@BizProcessorSpec(msgType=BizMessageType.CARDSVR_REQUEST)
public class CardServiceRequestProcessor extends BizProcessor {
	@Override
	public void process() {
		CardServiceRequest csr = (CardServiceRequest)this.getMsgObject();
		
		//生成订单,存储数据库
		//存储订单信息order
		Order order = new Order();
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-ddTHH:mm:ss+8:00");
		try {
			date = sdf.parse(csr.getPosData().getPosTimestamp());
		} catch (ParseException e1) {
			e1.printStackTrace();
		}
		sdf = new SimpleDateFormat("yyyyMMdd");
		String merchantId = Utils.systemConfiguration.getProperty("eps.server.merchant.id");
		String orderId = sdf.format(date) + merchantId +csr.getRequestId();
		order.setOrderId(orderId);
		order.setMerchantId(merchantId);
		order.setMerchantName(Utils.systemConfiguration.getProperty("eps.server.merchant.name"));
		order.setGenerator(csr.getApplicationSender() + csr.getWorkstationId());
		order.setOrderTime(date.getTime());
		order.setShiftNumber(csr.getPosData().getShiftNumber());
		order.setClerkId(csr.getPosData().getClerkID());
		order.setOriginalAmount(csr.getTotalAmount());
		OrderService ordersrv = EPSServer.appctx.getBean("orderService", OrderService.class);
		ordersrv.addOrder(order);
		//存储saleitems
		SaleItemService saleItemsrv = EPSServer.appctx.getBean("saleItemService", SaleItemService.class);
		for(com.bhz.eps.entity.CardServiceRequest.SaleItem item : csr.getSaleItemList()){
			com.bhz.eps.entity.SaleItemEntity si = new com.bhz.eps.entity.SaleItemEntity();
			si.setId(Utils.generateCompactUUID());
			si.setProductCode(item.getProductCode());
			si.setUnitMeasure(item.getUnitMeasure());
			si.setUnitPrice(item.getUnitPrice());
			si.setQuantity(item.getQuantity());
			si.setItemCode(item.getItemId());
			si.setOrderId(orderId);
			si.setAmount(item.getAmount());
			saleItemsrv.addSaleItem(si);
		}	
		
		//请求设备显示支付等待
		DeviceService ds = DeviceService.getInstance("localhost", 4050);
		
		try {
			ds.askBPosDisplay("正在支付，请稍后...", csr.getRequestId());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		//
	}

}
