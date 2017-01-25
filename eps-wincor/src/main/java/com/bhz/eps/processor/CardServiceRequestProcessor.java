package com.bhz.eps.processor;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.bhz.eps.DeviceService;
import com.bhz.eps.EPSServer;
import com.bhz.eps.TransPosDataSender;
import com.bhz.eps.annotation.BizProcessorSpec;
import com.bhz.eps.entity.CardServiceRequest;
import com.bhz.eps.entity.Order;
import com.bhz.eps.msg.BizMessageType;
import com.bhz.eps.service.OrderService;
import com.bhz.eps.service.SaleItemService;
import com.bhz.eps.util.Utils;

import javax.annotation.Resource;

@BizProcessorSpec(msgType=BizMessageType.CARDSVR_REQUEST)
public class CardServiceRequestProcessor extends BizProcessor {
	@Override
	public void process() {
		CardServiceRequest csr = (CardServiceRequest)this.getMsgObject();
		String dateString="";
		String timeString = "";
		String bposTime = csr.getPosData().getPosTimestamp();
		if(bposTime.indexOf("T")!=-1){
			dateString = bposTime.substring(0, bposTime.indexOf("T"));
			
			if(bposTime.indexOf("+")!=-1){
				timeString = bposTime.substring(bposTime.indexOf("T")+1,bposTime.indexOf("+"));
			}else if(bposTime.indexOf("-")!=-1){
				timeString = bposTime.substring(bposTime.indexOf("T")+1,bposTime.lastIndexOf("-"));
			}else{
				timeString = bposTime.substring(bposTime.indexOf("T"),bposTime.length()-1);
			}
			
		}
		
		
		//生成订单,存储数据库
		//存储订单信息order
		Order order = new Order();
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try {
			date = sdf.parse(dateString + " " + timeString);
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
		//OrderService ordersrv = EPSServer.appctx.getBean("orderService", OrderService.class);
		//ordersrv.addOrder(order);
		//存储saleitems
		SaleItemService saleItemsrv = EPSServer.appctx.getBean("saleItemService", SaleItemService.class);
        saleItemsrv.saveSaleItems(order, csr.getSaleItemList());
		
		//请求设备显示支付等待
		DeviceService ds = DeviceService.getInstance("localhost", 4050);
		
		try {
			ds.askBPosDisplay("正在支付，请稍后...", csr.getRequestId());
		} catch (Exception e) {
			e.printStackTrace();
		}

        //轮询查询交易状态，当交易完成时停止轮询并将数据传出
        ScheduledExecutorService service = Executors.newScheduledThreadPool(1);
        service.scheduleWithFixedDelay(new CheckStatus(service, orderId), 0, 1, TimeUnit.SECONDS);
    }

    class CheckStatus implements Runnable {
        private ScheduledExecutorService service;
        private String orderId;
        @Resource
        private OrderService orderService;

        CheckStatus(ScheduledExecutorService service, String orderId) {
            this.service = service;
            this.orderId = orderId;
        }

        @Override
        public void run() {
            Order order = orderService.getOrderbyId(orderId);
            if (Order.STATUS_SUCCESS == order.getStatus()) {
                TransPosDataSender sender = TransPosDataSender.getInstance(Utils.systemConfiguration.getProperty("trans.pos.ip"),
                        Integer.parseInt(Utils.systemConfiguration.getProperty("trans.pos.port")));
                try {
                    sender.askPosToPrintReceipt(order);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                service.shutdownNow();
            }
        }
    }

}
