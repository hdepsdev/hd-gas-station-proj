package com.bhz.eps;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import com.bhz.eps.util.WeiXinUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bhz.eps.codec.TPDUDecoder;
import com.bhz.eps.codec.TPDUEncoder;
import com.bhz.eps.entity.Order;
import com.bhz.eps.entity.PayMethod;
import com.bhz.eps.entity.SaleItemEntity;
import com.bhz.eps.pdu.transpos.TPDU;
import com.bhz.eps.util.Converts;
import com.bhz.eps.util.Utils;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
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
import io.netty.util.AttributeKey;

public class TransPosDataSender {
	private static final Logger logger = LogManager.getLogger(TransPosDataSender.class);
	private String transPosIP;
	private int transPosPort;
	
	private static TransPosDataSender sender;
	
	private TransPosDataSender(String ip,int port){
		this.transPosIP = ip;
		this.transPosPort = port;
		logger.trace("Initialize TransPOS Data Sender.");
	}
	
	public static TransPosDataSender getInstance(String ip,int port){
		if(sender == null){
			return new TransPosDataSender(ip, port);
		}else{
			return sender;
		}
	}
	
	/**
	 * 发送支付请求信息
	 * @param payMethodList 支付方式列表
	 * @throws Exception
	 */
	public void selectPayMethodToPos(List<PayMethod> payMethodList,Order order) throws Exception{
		Bootstrap boot = new Bootstrap();
		EventLoopGroup worker = new NioEventLoopGroup();
		try{
			boot.group(worker).option(ChannelOption.TCP_NODELAY, true)
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Integer.parseInt(Utils.systemConfiguration.getProperty("eps.client.data.upload.timeout")))
				.channel(NioSocketChannel.class)
				.handler(new ChannelInitializer<SocketChannel>(){

					@Override
					protected void initChannel(SocketChannel ch) throws Exception {
						ch.pipeline().addLast(new TPDUEncoder());
						ch.pipeline().addLast(new TPDUDecoder());
						ch.pipeline().addLast(new SelectPayMethodHandler(payMethodList,order));
					}
					
				});
			ChannelFuture cf = boot.connect(this.transPosIP, this.transPosPort).sync();
			cf.addListener(new ChannelFutureListener() {
				
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					logger.debug("Established connection to " + transPosIP + " on port " + transPosPort);
				}
				
			});
			cf.channel().closeFuture().sync();
				
		}finally{
			worker.shutdownGracefully();
		}
	}
	
	/**
	 * 发送订单信息给交易POS，交易POS生成订单二维码
	 * @param order 订单信息
	 * @throws Exception
	 */
	public void sendOrderToTransPos(final Order order) throws Exception{
		Bootstrap boot = new Bootstrap();
		EventLoopGroup worker = new NioEventLoopGroup();
		try{
			boot.group(worker).option(ChannelOption.TCP_NODELAY, true)
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Integer.parseInt(Utils.systemConfiguration.getProperty("eps.client.data.upload.timeout")))
				.channel(NioSocketChannel.class)
				.handler(new ChannelInitializer<SocketChannel>(){

					@Override
					protected void initChannel(SocketChannel ch) throws Exception {
						ch.pipeline().addLast(new TPDUEncoder());
						ch.pipeline().addLast(new TPDUDecoder());
						ch.pipeline().addLast(new TransPosOrderHandler(order));
					}
					
				});
			ChannelFuture cf = boot.connect(this.transPosIP, this.transPosPort).sync();
			cf.addListener(new ChannelFutureListener() {
				
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					logger.debug("Established connection to " + transPosIP + " on port " + transPosPort);
				}
				
			});
			cf.channel().closeFuture().sync();
				
		}finally{
			worker.shutdownGracefully();
		}
	}

    /**
     * 发送信息给交易POS
     * @param order 订单信息
     * @throws Exception
     */
    public void sendMsgToTransPos(final Order order) throws Exception{
        if(Utils.systemConfiguration.getProperty("scan.type").equalsIgnoreCase("initiative")){
            sendOrderToTransPos(order);
        }else{
            selectPayMethodToPos(Utils.PAY_METHOD_LIST,order);
        }
    }
	
	/**
	 * 请求POS打单
	 * @param order 订单信息
	 * @throws Exception
	 */
	public void askPosToPrintReceipt(final Order order) throws Exception{
		Bootstrap boot = new Bootstrap();
		EventLoopGroup worker = new NioEventLoopGroup();
		try{
			boot.group(worker).option(ChannelOption.TCP_NODELAY, true)
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Integer.parseInt(Utils.systemConfiguration.getProperty("eps.client.data.upload.timeout")))
				.channel(NioSocketChannel.class)
				.handler(new ChannelInitializer<SocketChannel>(){

					@Override
					protected void initChannel(SocketChannel ch) throws Exception {
						ch.pipeline().addLast(new TPDUEncoder());
						ch.pipeline().addLast(new TPDUDecoder());
						ch.pipeline().addLast(new SendReceiptHandler(order));
					}
					
				});
			ChannelFuture cf = boot.connect(this.transPosIP, this.transPosPort).sync();
			cf.addListener(new ChannelFutureListener() {
				
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					logger.debug("Established connection to " + transPosIP + " on port " + transPosPort);
				}
				
			});
			cf.channel().closeFuture().sync();
				
		}finally{
			worker.shutdownGracefully();
		}
	}
}

class SelectPayMethodHandler extends SimpleChannelInboundHandler<TPDU>{
	private static final Logger logger = LogManager.getLogger(TransPosOrderHandler.class);
	List<PayMethod> pmList;
	Order order;
	
	public SelectPayMethodHandler(List<PayMethod> pmList,Order order) {
		this.pmList = pmList;
		this.order = order;
	}
	
	@Override
	protected void messageReceived(ChannelHandlerContext ctx, TPDU msg) throws Exception {
		logger.info("Receive POS Response[ " + msg  + " ]");
		//@Todo 发送支付网关
	}
	
	@Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        logger.error("", cause);
        super.exceptionCaught(ctx, cause);
    }
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		ByteBuf b = Unpooled.buffer();
		int pmCount = pmList.size();
		b.writeByte(pmCount);
		for(PayMethod pm: pmList){
			b.writeByte(pm.getPayMethodCode());
			b.writeBytes(Utils.convertGB(pm.getPayMethodName(),20).getBytes(Charset.forName("GB2312")));
		}
		
		byte[] result = b.array();
		logger.debug("Send pay method(s) to Trans POS.");
        logger.debug(Arrays.toString(result));
		ctx.writeAndFlush(result);
	}
	
}

class TransPosOrderHandler extends SimpleChannelInboundHandler<TPDU>{
	private static final Logger logger = LogManager.getLogger(TransPosOrderHandler.class);
	Order order;
	
	public TransPosOrderHandler(Order order) {
		this.order = order;
	}
	@Override
	protected void messageReceived(ChannelHandlerContext ctx, TPDU msg) throws Exception {
//		byte[] cmd = msg.getBody().getHeader().getCmd();
		System.out.println(msg);
	}
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        logger.error("", cause);
        super.exceptionCaught(ctx, cause);
    }

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		/*
		char padding = 0x20;
		String strAppid = Utils.systemConfiguration.getProperty("weixin.appid");
		String strMchid = Utils.systemConfiguration.getProperty("weixin.mchid");
		String strNonce = Utils.systemConfiguration.getProperty("weixin.nonce");

		String strTime = Long.toString(order.getOrderTime());
		String strProductId = order.getOrderId();

        SortedMap<Object,Object> parameters = new TreeMap<Object,Object>();
        parameters.put("appid", strAppid);
        parameters.put("mch_id", strMchid);
        parameters.put("time_stamp", strTime);
        parameters.put("nonce_str", strNonce);
        parameters.put("product_id", strProductId);
        String strSign = WeiXinUtil.createSign("UTF-8", parameters);

		StringBuilder sb = new StringBuilder();
		sb.append(Utils.rightPad(strAppid, 32, padding)).append(Utils.rightPad(strMchid, 32, padding))
			.append(Utils.rightPad(strTime, 10, padding)).append(Utils.rightPad(strNonce, 32, padding))
			.append(Utils.rightPad(strProductId, 32, padding)).append(Utils.rightPad(strSign, 32, padding));
		
		byte[] content = sb.toString().getBytes("utf-8");
		
		byte[] stationId = Converts.str2Bcd(order.getMerchantId());
		int casherId = Integer.parseInt(order.getGenerator().split("\\|")[1]);
		byte[] casherNo = Converts.int2U16(casherId);
		byte[] magic = new byte[]{0x30,0x30,0x30,0x30};
		byte[] cmd = new byte[]{'7','0'};
		byte[] tag = new byte[]{'0','0'};
		
		byte[] tmp = Utils.concatTwoByteArray(stationId,casherNo);
		byte[] tmp1 = Utils.concatTwoByteArray(tmp, magic);
		byte[] tmp2 = Utils.concatTwoByteArray(tmp1, cmd);
		byte[] tmp3 = Utils.concatTwoByteArray(tmp2, tag);
		byte[] result = Utils.concatTwoByteArray(tmp3, content);
		
		logger.debug("Send order to Trans POS.");
        logger.debug(Arrays.toString(result));
		ctx.writeAndFlush(result);
		*/
		String payUrl = Utils.systemConfiguration.getProperty("weixin.pay.url");
		String orderId = order.getOrderId();
		String overall = payUrl + orderId;
		ByteBuf length = Unpooled.buffer(4);
		byte[] content = overall.getBytes("utf-8");
		length.writeInt(content.length);
		byte[] contentLength = length.array();
		
		byte[] stationId = Converts.str2Bcd(order.getMerchantId());
		int casherId = Integer.parseInt(order.getGenerator().split("\\|")[1]);
		byte[] casherNo = Converts.int2U16(casherId);
		byte[] magic = new byte[]{0x30,0x30,0x30,0x30};
		byte[] cmd = new byte[]{'7','0'};
		byte[] tag = new byte[]{'0','0'};
		
		byte[] tmp = Utils.concatTwoByteArray(stationId,casherNo);
		byte[] tmp1 = Utils.concatTwoByteArray(tmp, magic);
		byte[] tmp2 = Utils.concatTwoByteArray(tmp1, cmd);
		byte[] tmp3 = Utils.concatTwoByteArray(tmp2, tag);
		byte[] tmp4 = Utils.concatTwoByteArray(tmp3, contentLength);
		byte[] result = Utils.concatTwoByteArray(tmp4, content);
		//保存订单信息到Channel属性中，以便在连接异常时回滚此笔订单。
		ctx.channel().attr(AttributeKey.valueOf("orderId")).set(orderId);
		logger.debug("Send order to Trans POS.");
        logger.debug(Arrays.toString(result));
		ctx.writeAndFlush(result);
	}
	
}

class SendReceiptHandler extends SimpleChannelInboundHandler<TPDU>{
	private static final Logger logger = LogManager.getLogger(TransPosOrderHandler.class);
	Order order;
	
	public SendReceiptHandler(Order order) {
		this.order = order;
	}
	@Override
	protected void messageReceived(ChannelHandlerContext ctx, TPDU msg) throws Exception {
		// TODO Auto-generated method stub
		System.out.println(msg);
	}

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        logger.error("", cause);
        super.exceptionCaught(ctx, cause);
    }
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		char padding = 0x20;
		byte[] stationId = Converts.str2Bcd(order.getMerchantId());
		int casherId = Integer.parseInt(order.getGenerator().split("\\|")[1]);
		byte[] casherNo = Converts.int2U16(casherId);
		byte[] magic = new byte[]{0x30,0x30,0x30,0x30};
		byte[] cmd = new byte[]{'7','1'};
		byte[] tag = new byte[]{'0','0'};
		
		byte[] tmp = Utils.concatTwoByteArray(stationId,casherNo);
		byte[] tmp1 = Utils.concatTwoByteArray(tmp, magic);
		byte[] tmp2 = Utils.concatTwoByteArray(tmp1, cmd);
		byte[] tmp3 = Utils.concatTwoByteArray(tmp2, tag);
		
		ByteBuf b = Unpooled.buffer(69,201);
		//交易流水号
		String orderId = order.getOrderId().substring(order.getOrderId().length()-8);
		b.writeBytes(Converts.str2Bcd(orderId));
		//交易时间
		b.writeBytes(Converts.long2U32(order.getOrderTime()));
		//应收金额
		b.writeBytes(Converts.long2U32(order.getOriginalAmount().multiply(new BigDecimal(100)).longValue()));
		//实付金额
        if (order.getPaymentAmount() != null) {
            b.writeBytes(Converts.long2U32(order.getPaymentAmount().multiply(new BigDecimal(100)).longValue()));
        } else {
            b.writeBytes(Converts.long2U32(0));
        }
		//优惠券优惠金额
        if (order.getCouponAmount() != null) {
            b.writeBytes(Converts.long2U32(order.getCouponAmount().multiply(new BigDecimal(100)).longValue()));
        } else {
            b.writeBytes(Converts.long2U32(0));
        }
		//此次消费获得积分
        if (order.getLoyaltyPoint() != null) {
            b.writeBytes(Converts.long2U32(order.getLoyaltyPoint().multiply(new BigDecimal(100)).longValue()));
        } else {
            b.writeBytes(Converts.long2U32(0));
        }
		//消费信息总条数
		b.writeByte(order.getOrderItems().size());
		//循环填充交易明细
		for(SaleItemEntity sie:order.getOrderItems()){
			b.writeBytes(Utils.convertGB(sie.getItemName(),32).getBytes(Charset.forName("GB2312")));
			b.writeInt(sie.getQuantity().multiply(new BigDecimal(100)).intValue());
			b.writeInt(sie.getUnitPrice().multiply(new BigDecimal(100)).intValue());
			b.writeInt(sie.getAmount().multiply(new BigDecimal(100)).intValue());
		}
		byte[] result = Utils.concatTwoByteArray(tmp3, b.array());
		logger.debug("Send receipt to Trans POS.");
        logger.debug(Arrays.toString(result));
		ctx.writeAndFlush(result);
	}
	
}
