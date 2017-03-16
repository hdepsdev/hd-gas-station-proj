package com.bhz.eps;

import java.math.BigDecimal;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bhz.eps.entity.Order;
import com.bhz.eps.entity.SaleItemEntity;
import com.bhz.eps.msg.PaymentReqProto;
import com.bhz.eps.msg.PaymentRespProto;
import com.bhz.eps.msg.PaymentRespProto.PaymentResp;
import com.bhz.eps.service.OrderService;
import com.bhz.eps.util.Utils;

public class DataCenterOrderSender {
	private static final Logger logger = LogManager.getLogger(TransPosDataSender.class);
	private String dataCenterIp;
	private int dataCenterPort;
	
	private DataCenterOrderSender(String ip,int port){
		this.dataCenterIp = ip;
		this.dataCenterPort = port;
		logger.trace("Initialize DataCenter Order Sender.");
	}
	
	private static DataCenterOrderSender sender;
	
	public static DataCenterOrderSender getInstance(String ip,int port){
		if(sender == null){
			return new DataCenterOrderSender(ip, port);
		}else{
			return sender;
		}
	}
	
	/**
     * 发送交易信息给DataCenter
     * @param order 订单信息
     * @throws Exception
     */
   public void askDataCenterToPay(final Order order, int methodOfPayment, String authCode) throws Exception{
	   Bootstrap boot = new Bootstrap();
	   EventLoopGroup worker = new NioEventLoopGroup();
	   try{
		   boot.group(worker).option(ChannelOption.TCP_NODELAY, true)
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Integer.parseInt(
						Utils.systemConfiguration.getProperty("data.center.timeout")))
				.channel(NioSocketChannel.class)
				.handler(new ChannelInitializer<SocketChannel>(){

					@Override
					protected void initChannel(SocketChannel ch) throws Exception {
						ch.pipeline().addLast(new ProtobufVarint32FrameDecoder());
						ch.pipeline().addLast(new ProtobufVarint32LengthFieldPrepender());
						ch.pipeline().addLast(new ProtobufEncoder());
						ch.pipeline().addLast(new ProtobufDecoder(PaymentRespProto.PaymentResp.getDefaultInstance()));
						ch.pipeline().addLast(new SendOrderToPay(order, methodOfPayment, authCode));
					}
					
				});

			ChannelFuture cf = boot.connect(dataCenterIp, dataCenterPort).sync();
			cf.addListener(new ChannelFutureListener() {
				
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					logger.debug("Established connection to " + dataCenterIp + " on port " + dataCenterPort);
				}
				
			});
			cf.channel().closeFuture().sync();
				
		}finally{
			worker.shutdownGracefully();
		}
   }
}

class SendOrderToPay extends SimpleChannelInboundHandler<PaymentRespProto.PaymentResp>{
	private static final Logger logger = LogManager.getLogger(SendOrderToPay.class);
	Order order;
	int methodOfPayment;
	String authCode;
	
	public SendOrderToPay(Order order, int methodOfPayment, String authCode){
		this.order = order;
		this.methodOfPayment = methodOfPayment;
		this.authCode = authCode;
	}
	
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        logger.error("", cause);
        super.exceptionCaught(ctx, cause);
    }

	@Override
	protected void messageReceived(ChannelHandlerContext ctx, PaymentResp msg)
			throws Exception {
		//获取DataCenter的支付结果
		if (msg.getPaymentState() == 0)
			paySuccess();
		else
			payFail();
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		//组织订单信息发送DataCenter进行支付
		PaymentReqProto.PaymentReq.Builder req = PaymentReqProto.PaymentReq.newBuilder();
		
		PaymentReqProto.Seller.Builder seller = PaymentReqProto.Seller.newBuilder();
		seller.setProviderId(Utils.systemConfiguration.getProperty("alipay.ProviderId"));
		seller.setStationId(order.getMerchantId());
		seller.setNozzleNumber(order.getClerkId());
		req.setSeller(seller);
		
		req.setWorkOrder(order.getOrderId());
		req.setMethodOfPayment(methodOfPayment);
		
		PaymentReqProto.PaymentAmount.Builder amount = PaymentReqProto.PaymentAmount.newBuilder();
		amount.setTotalAmount(order.getPaymentAmount().doubleValue());
		req.setPaymentAmount(amount);
		
		req.setTitle(order.getMerchantName());
		
		PaymentReqProto.OrderTime.Builder time = PaymentReqProto.OrderTime.newBuilder();
		time.setTimeStart(String.valueOf(order.getOrderTime()));
		req.setOrderTime(time);
		
		for(SaleItemEntity e : order.getOrderItems()){
			PaymentReqProto.Goods.Builder g = PaymentReqProto.Goods.newBuilder();
			g.setGoodsId(e.getId());
			g.setGoodsName(e.getItemName());
			g.setPrice((e.getUnitPrice().multiply(new BigDecimal(100))).intValue());
			g.setQuantity(e.getQuantity().intValue());
			req.addGoodsDetail(g);
		}
		
		req.setAuthCode(authCode);
		
		ctx.writeAndFlush(req);
		
		super.channelActive(ctx);
	}
	
	private OrderService orderService = EPSServer.appctx.getBean("orderService", OrderService.class);
	
	/**
	 * 支付成功后调用
	 */
	private void paySuccess() {
		try {
			order.setStatus(Order.STATUS_SUCCESS);// 设置订单状态为交易成功
			orderService.updateOrder(order);
			
			// TODO 计算积分，由于目前消费时无用户信息，所以暂时无法实现
		} catch (Exception e) {
			logger.error("", e);
		}
	}
	
	/**
	 * 支付失败后调用
	 */
	private void payFail() {
		try {
			int status = order.getStatus();
			if (status == Order.STATUS_WAIT) {// 只有待支付状态的订单才能改变状态
				order.setStatus(Order.STATUS_ERROR);// 设置订单状态为交易失败
				orderService.updateOrder(order);
			}
		} catch (Exception e) {
			logger.error("", e);
		}
	}
}
