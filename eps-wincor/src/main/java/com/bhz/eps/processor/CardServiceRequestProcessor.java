package com.bhz.eps.processor;

import io.netty.channel.Channel;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.*;

import io.netty.util.AttributeKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bhz.eps.DeviceService;
import com.bhz.eps.EPSServer;
import com.bhz.eps.TransPosDataSender;
import com.bhz.eps.annotation.BizProcessorSpec;
import com.bhz.eps.entity.CardServiceRequest;
import com.bhz.eps.entity.CardServiceResponse;
import com.bhz.eps.entity.CardServiceResponse.Tender.*;
import com.bhz.eps.entity.CardServiceResponse.*;
import com.bhz.eps.entity.Order;
import com.bhz.eps.msg.BizMessageType;
import com.bhz.eps.service.OrderService;
import com.bhz.eps.service.SaleItemService;
import com.bhz.eps.util.Utils;
import com.thoughtworks.xstream.XStream;

@BizProcessorSpec(msgType=BizMessageType.CARDSVR_REQUEST)
public class CardServiceRequestProcessor extends BizProcessor {
	private static XStream xstream;
	static {
		xstream = new XStream();
		xstream.autodetectAnnotations(true);
		xstream.ignoreUnknownElements();
	}
	private static final Logger logger = LogManager.getLogger(CardServiceRequestProcessor.class);
	
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
        //test code
        //this.getChannel().attr(AttributeKey.valueOf("orderId")).set(orderId);
        OrderService ordersrv = EPSServer.appctx.getBean("orderService", OrderService.class);
        Order order = ordersrv.getOrderbyId(orderId);
        if (order != null) {
            //如果订单数据已存在，判断状态
            //如果状态为1或2，表示交易成功或已锁定，直接退出
            //如果为99，表示该订单曾出现异常，置状态为0(待支付)，重新发送二维码并启动轮询
            //如果为0，标示待支付，直接退出，因为插入数据时轮询已启动
            logger.debug("orderId: " + orderId + " has exists. status: " + order.getStatus());
            if (order.getStatus() == Order.STATUS_ERROR) {
                order.setStatus(Order.STATUS_WAIT);
                ordersrv.updateOrder(order);
                sendMsgAndSchedule(order, csr);
            }
        } else {
            //订单不存在，插入数据
            logger.debug("insert orderId:" + orderId);
            order = new Order();
            order.setOrderId(orderId);
            order.setMerchantId(merchantId);
            order.setMerchantName(Utils.systemConfiguration.getProperty("eps.server.merchant.name"));
            order.setGenerator(csr.getApplicationSender() + "|" + csr.getWorkstationId());
            order.setOrderTime(date.getTime()/1000);//将毫秒转为秒
            order.setShiftNumber(csr.getPosData().getShiftNumber());
            order.setClerkId(csr.getPosData().getClerkID());
            order.setOriginalAmount(csr.getTotalAmount());
            //OrderService ordersrv = EPSServer.appctx.getBean("orderService", OrderService.class);
            //ordersrv.addOrder(order);
            //存储saleitems
            SaleItemService saleItemsrv = EPSServer.appctx.getBean("saleItemService", SaleItemService.class);
            //设置默认值
            if (order.getPaymentAmount() == null) {
                order.setPaymentAmount(new BigDecimal(0.00));
            }
            if (order.getCouponAmount() == null) {
                order.setCouponAmount(new BigDecimal(0.00));
            }
            if (order.getLoyaltyPoint() == null) {
                order.setLoyaltyPoint(new BigDecimal(0));
            }
            saleItemsrv.saveSaleItems(order, csr.getSaleItemList());
            sendMsgAndSchedule(order, csr);
        }
    }

    /**
     * 发送消息并启动检测交易状态的定时任务
     * @param order
     * @param csr
     */
    private void sendMsgAndSchedule(Order order, CardServiceRequest csr) {
        //请求设备显示支付等待
        DeviceService ds = DeviceService.getInstance(Utils.systemConfiguration.getProperty("eps.bpos.ds.ip"),
                Integer.parseInt(Utils.systemConfiguration.getProperty("eps.bpos.ds.port")));

        new Thread(){
            @Override
            public void run() {
                try {
                    logger.debug("send message");
                    ds.askBPosDisplay("正在支付，请稍后...", order);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();

        //轮询查询交易状态，当交易完成时停止轮询并将数据传出
        logger.debug("create schedule");
        ScheduledExecutorService service = Executors.newScheduledThreadPool(1);
        ScheduledFuture f = service.scheduleWithFixedDelay(new CheckStatus(service, order.getOrderId(), channel, csr), 0, 1, TimeUnit.SECONDS);
    }

    class CheckStatus implements Runnable {
        private ScheduledExecutorService service;
        private String orderId;
        private Channel channel;
        private CardServiceRequest cardServiceRequest;
        private OrderService orderService = EPSServer.appctx.getBean("orderService", OrderService.class);
        private final String weiXinPay = "0010";//TODO 微信支付方式，真实值未知
        
        CheckStatus(ScheduledExecutorService service, String orderId, Channel channel, CardServiceRequest cardservicerequest) {
            logger.debug("create CheckStatus");
            this.service = service;
            this.orderId = orderId;
            this.channel = channel;
            this.cardServiceRequest = cardservicerequest;
        }

        @Override
        public void run() {
            try {
                Order order = orderService.getOrderWithSaleItemsById(orderId);
                //test log
                //logger.debug("start CheckStatus " + this.hashCode());
                //logger.debug("orderId: " + orderId + ", status: " + order.getStatus());
                if (Order.STATUS_ERROR == order.getStatus()) {//订单异常
                    ExecutorService cardResponseService = Executors.newFixedThreadPool(1);
                    cardResponseService.execute(new Runnable() {

                        @Override
                        public void run() {
                            try {
                                // TODO Auto-generated method stub
                                //获取订单相关信息
                                CardServiceResponse csr = new CardServiceResponse();
                                csr.setRequestType(cardServiceRequest.getRequestType());
                                csr.setApplicationSender(cardServiceRequest.getApplicationSender());
                                csr.setWorkstationId(cardServiceRequest.getWorkstationId());
                                csr.setPopId(cardServiceRequest.getPopId());
                                csr.setRequestId(cardServiceRequest.getRequestId());
                                csr.setOverallResult("Aborted");

                                Tender tender = new Tender();

                                TotalAmount totalAmount = new TotalAmount();
                                totalAmount.setTotalAmount(order.getPaymentAmount());
                                totalAmount.setPaymentAmount(order.getPaymentAmount());
                                totalAmount.setRebateAmount(order.getCouponAmount());
                                totalAmount.setOriginalAmount(order.getOriginalAmount());
                                tender.setTotalAmount(totalAmount);

                                Authorisation authorisation = new Authorisation();
                                authorisation.setAcquirerid(weiXinPay);
                                tender.setAuthorisation(authorisation);

                                csr.setTender(tender);
                                //CardServiceResponse序列化为xml，并发送
                                String csrxml = xstream.toXML(csr);
                                logger.debug(csrxml);
                                channel.write(csrxml);
                                channel.flush();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                    });
                    cardResponseService.shutdown();
                    service.shutdownNow();
                    return;
                } else if (Order.STATUS_SUCCESS == order.getStatus()) {//交易完成
                    TransPosDataSender sender = TransPosDataSender.getInstance(Utils.systemConfiguration.getProperty("trans.pos.ip"),
                            Integer.parseInt(Utils.systemConfiguration.getProperty("trans.pos.port")));

                    ExecutorService cardResponseService = Executors.newFixedThreadPool(1);
                    ExecutorService printReceiptService = Executors.newFixedThreadPool(1);

                    cardResponseService.execute(new Runnable() {

                        @Override
                        public void run() {
                            try {
                                // TODO Auto-generated method stub
                                //获取订单相关信息
                                CardServiceResponse csr = new CardServiceResponse();
                                csr.setRequestType(cardServiceRequest.getRequestType());
                                csr.setApplicationSender(cardServiceRequest.getApplicationSender());
                                csr.setWorkstationId(cardServiceRequest.getWorkstationId());
                                csr.setPopId(cardServiceRequest.getPopId());
                                csr.setRequestId(cardServiceRequest.getRequestId());
                                csr.setOverallResult("Success");

                                Tender tender = new Tender();

                                TotalAmount totalAmount = new TotalAmount();
                                totalAmount.setTotalAmount(order.getPaymentAmount());
                                totalAmount.setPaymentAmount(order.getPaymentAmount());
                                totalAmount.setRebateAmount(order.getCouponAmount());
                                totalAmount.setOriginalAmount(order.getOriginalAmount());
                                tender.setTotalAmount(totalAmount);

                                Authorisation authorisation = new Authorisation();
                                authorisation.setAcquirerid(weiXinPay);
                                tender.setAuthorisation(authorisation);

                                csr.setTender(tender);
                                //CardServiceResponse序列化为xml，并发送
                                String csrxml = xstream.toXML(csr);
                                logger.debug(csrxml);
                                channel.write(csrxml);
                                channel.flush();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                    });

                    printReceiptService.execute(new Runnable() {

                        @Override
                        public void run() {
                            // TODO Auto-generated method stub
                            try {
                                sender.askPosToPrintReceipt(order);
                            } catch (Exception e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                    });

                    cardResponseService.shutdown();
                    printReceiptService.shutdown();
                    service.shutdownNow();
                }
            } catch (Exception e) {
                e.printStackTrace();
                service.shutdownNow();
            }
        }
    }

}
