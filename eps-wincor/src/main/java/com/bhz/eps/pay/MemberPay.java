package com.bhz.eps.pay;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bhz.eps.EPSServer;
import com.bhz.eps.TransPosDataSender;
import com.bhz.eps.codec.TPDUDecoder;
import com.bhz.eps.codec.TPDUEncoder;
import com.bhz.eps.entity.MemberPayRequest;
import com.bhz.eps.entity.Order;
import com.bhz.eps.entity.PayPassword;
import com.bhz.eps.entity.SaleItemEntity;
import com.bhz.eps.entity.MemberPayRequest.ExpandFlow;
import com.bhz.eps.entity.MemberPayRequest.ReqBody;
import com.bhz.eps.pdu.transpos.TPDU;
import com.bhz.eps.service.OrderService;
import com.bhz.eps.util.Converts;
import com.bhz.eps.util.DateUtil;
import com.bhz.eps.util.FormatedJsonHierarchicalStreamDriver;
import com.bhz.eps.util.HMacUtil;
import com.bhz.eps.util.Utils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.thoughtworks.xstream.XStream;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.handler.codec.http.HttpVersion;

public class MemberPay {
	
	private final static String MEMBER_PAY_URL;
	private final static String MEMBER_PAY_HOST;
	private final static int MEMBER_PAY_PORT;
	private final static String PAY_PASS_URL;

    private MemberPayCallbackInterface callback;

	public MemberPay(MemberPayCallbackInterface callback){
        this.callback = callback;
	}
	
	static {
		MEMBER_PAY_URL = Utils.systemConfiguration.getProperty("memberpay.url").trim();
		MEMBER_PAY_HOST = Utils.systemConfiguration.getProperty("memberpay.host").trim();
		MEMBER_PAY_PORT = Integer.parseInt(Utils.systemConfiguration.getProperty("memberpay.port").trim());
		PAY_PASS_URL = Utils.systemConfiguration.getProperty("memberpay.validpass.url").trim();
	}
	
	public void requestPassword(String cardNo,Order order){
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
						ch.pipeline().addLast(new MemberPayPasswdHandler(cardNo,order));
					}
					
				});
			ChannelFuture cf = boot.connect(Utils.systemConfiguration.getProperty("trans.pos.ip"),
                    Integer.parseInt(Utils.systemConfiguration.getProperty("trans.pos.port"))).sync();
			cf.addListener(new ChannelFutureListener() {
				
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					future.channel().writeAndFlush(requestPosShow(order));
				}
				
			});
			cf.channel().closeFuture().sync();
				
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			worker.shutdownGracefully();
		}
	}
	
	public class ReqPayPasswd implements Runnable{
		String cardNo;
		Order order;
		public ReqPayPasswd(String cardNo,Order order){
			this.cardNo = cardNo;
			this.order = order;
		}
		@Override
		public void run() {
			MemberPay mp = new MemberPay(callback);
			mp.requestPassword(cardNo, order);
		}
		
	}
	
	public void sendPayJsonInfo(final String req,final Order order){
		Bootstrap b = new Bootstrap();
		EventLoopGroup worker = new NioEventLoopGroup();
		b.group(worker).option(ChannelOption.TCP_NODELAY, true)
		.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Integer.parseInt(Utils.systemConfiguration.getProperty("eps.client.data.upload.timeout")))
		.channel(NioSocketChannel.class)
		.handler(new ChannelInitializer<SocketChannel>(){

			@Override
			protected void initChannel(SocketChannel ch) throws Exception {
				ch.pipeline().addLast(new HttpResponseDecoder());
				ch.pipeline().addLast(new HttpRequestEncoder());
				ch.pipeline().addLast(new MemberPayInfoHandler(order));
			}
			
		});
		
		try {
			ChannelFuture cf = b.connect(MEMBER_PAY_HOST,MEMBER_PAY_PORT).sync();
			cf.addListener(new ChannelFutureListener() {
				
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					System.out.println("Prepare to send http request");
					DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, MEMBER_PAY_URL,
							Unpooled.wrappedBuffer(req.getBytes(Charset.forName("utf-8"))));
					
					request.headers().set(HttpHeaderNames.HOST,MEMBER_PAY_HOST);
					request.headers().set(HttpHeaderNames.CONNECTION,HttpHeaderValues.KEEP_ALIVE);
					request.headers().set(HttpHeaderNames.CONTENT_LENGTH, Integer.toString(request.content().readableBytes()));
					request.headers().set(HttpHeaderNames.CONTENT_TYPE,"application/json");
					
					future.channel().writeAndFlush(request);
				}
			});
			cf.channel().closeFuture().sync();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally{
			worker.shutdownGracefully();
		}
		
	}
	
	public class MemberPayPasswdHandler extends ChannelHandlerAdapter{
		String cardNo;
		Order order;
		public MemberPayPasswdHandler(String cardNo,Order order){
			this.cardNo = cardNo;
			this.order = order;
		}
		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			TPDU pdu = (TPDU)msg;
			byte[] content = pdu.getBody().getData().getContent();
			int passwdSize = content[0];
			byte[] passwd = new byte[passwdSize];
			System.arraycopy(content, 1, passwd, 0, passwdSize);
			
			ExecutorService es = Executors.newFixedThreadPool(1);
			String encyptPasswd = HMacUtil.encryptHMAC(new String(passwd), Utils.systemConfiguration.getProperty("ws.trans.key"));
			es.execute(new ValidatePayPass(cardNo,encyptPasswd,order));
			es.shutdown();
		}
		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			// TODO Auto-generated method stub
			super.exceptionCaught(ctx, cause);
		}
	}
	
	public class ValidatePayPass implements Runnable{
		
		private String cardNo;
		private String password;
		private Order order;
		
		public ValidatePayPass(String cardNo,String password,Order order){
			this.cardNo = cardNo;
			this.password = password;
			this.order = order;
		}

		@Override
		public void run() {
			Bootstrap b = new Bootstrap();
			EventLoopGroup worker = new NioEventLoopGroup();
			b.group(worker).option(ChannelOption.TCP_NODELAY, true)
			.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Integer.parseInt(Utils.systemConfiguration.getProperty("eps.client.data.upload.timeout")))
			.channel(NioSocketChannel.class)
			.handler(new ChannelInitializer<SocketChannel>(){

				@Override
				protected void initChannel(SocketChannel ch) throws Exception {
					ch.pipeline().addLast(new HttpResponseDecoder());
					ch.pipeline().addLast(new HttpRequestEncoder());
					ch.pipeline().addLast(new ValidatePasswordHandler(cardNo,order));
				}
				
			});
			
			try {
				ChannelFuture cf = b.connect(MEMBER_PAY_HOST,MEMBER_PAY_PORT).sync();
				cf.addListener(new ChannelFutureListener() {
					
					@Override
					public void operationComplete(ChannelFuture future) throws Exception {
						System.out.println("Prepare to send http request");
						
						PayPassword pp = new PayPassword();
						pp.setTxCode("3102");
		        		pp.setTxTime(DateUtil.format(new Date(), DateUtil.DateEnum.webservice_timeWithMilli));
		        		pp.setTxDate(DateUtil.format(new Date(), DateUtil.DateEnum.webservice_date));
		        		pp.setReqSeqNo(DateUtil.format(new Date(), DateUtil.DateEnum.webservice_date));
		        		PayPassword.ReqBody ppbody = new PayPassword.ReqBody();
		        		ppbody.setCardNo(cardNo);
		        		ppbody.setPassword(password);
		        		pp.setReqBody(ppbody);
		        		String macValue = HMacUtil.encryptHMAC(pp.getTxCode()+pp.getTxTime()+pp.getReqBody().getCardNo() + pp.getReqBody().getPassword(), 
		        				Utils.systemConfiguration.getProperty("ws.trans.key"));
		        		pp.setTxMac(macValue);
						
						XStream x = new XStream(new FormatedJsonHierarchicalStreamDriver());
		        		x.autodetectAnnotations(true);
		        		x.setMode(XStream.NO_REFERENCES);
		        		
		        		String req = x.toXML(pp);
						
						DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, PAY_PASS_URL,
								Unpooled.wrappedBuffer(req.getBytes(Charset.forName("utf-8"))));
						
						request.headers().set(HttpHeaderNames.HOST,MEMBER_PAY_HOST);
						request.headers().set(HttpHeaderNames.CONNECTION,HttpHeaderValues.KEEP_ALIVE);
						request.headers().set(HttpHeaderNames.CONTENT_LENGTH, Integer.toString(request.content().readableBytes()));
						request.headers().set(HttpHeaderNames.CONTENT_TYPE,"application/json");
						
						future.channel().writeAndFlush(request);
					}
				});
				cf.channel().closeFuture().sync();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally{
				worker.shutdownGracefully();
			}
		}
		
	}
	
	public class ValidatePasswordHandler extends ChannelHandlerAdapter{
		Order order;
		String cardNo;
		private final Logger logger = LogManager.getLogger(ValidatePasswordHandler.class);
		
		public ValidatePasswordHandler(String cardNo, Order order){
			this.order = order;
			this.cardNo = cardNo;
		}
		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			if (msg instanceof HttpResponse) {
	            HttpResponse response = (HttpResponse) msg;
	            System.out.println("CONTENT_TYPE:" + response.headers().get(HttpHeaderNames.CONTENT_TYPE));
	        }
	        if(msg instanceof HttpContent){
	            HttpContent content = (HttpContent)msg;
	            ByteBuf buf = content.content();
	            String result = buf.toString(io.netty.util.CharsetUtil.UTF_8);
	            if(result==null || result.trim().equals("")){
	            	return;
	            }
	            System.out.println(result);
	            buf.release();
	            
	            Map<String,String> map = parseJsonData(result);
	            String reCode = map.get("RespCode");
	            if(reCode != null && !reCode.equals("00")){
	            	ExecutorService es = Executors.newFixedThreadPool(1);
					es.execute(new ReqPayPasswd(this.cardNo,this.order));
					es.shutdown();
	            }else{
	            	MemberPayRequest pay = new MemberPayRequest();
	        		pay.setTxCode("3103");
	        		pay.setTxDate(DateUtil.format(new Date(), DateUtil.DateEnum.webservice_date));
	        		pay.setMerchantNo("1000");
	        		pay.setReqSeqNo(order.getOrderId());
	        		pay.setTxTime(DateUtil.format(new Date(), DateUtil.DateEnum.webservice_timeWithMilli));
	        		pay.setBrchNo(order.getMerchantId());
	        		pay.setBrchName(order.getMerchantName());
	        		pay.setTellerNo(order.getClerkId());
	        		pay.setTellerName(order.getClerkId());
	        		
	        		ReqBody body = new ReqBody();
	        		body.setCardNo(cardNo);
	        		body.setOrgID(order.getMerchantId());
	        		body.setFillingTime(DateUtil.format(new Date(), DateUtil.DateEnum.webservice_timeWithMilli));
	        		body.setDeductionType("20");
	        		body.setTotalAmount(order.getPaymentAmount().toString());
	        		body.setNumberOfDetail(Integer.toString(order.getOrderItems().size()));
	        		
	        		
	        		List<ExpandFlow> eflist = new ArrayList<ExpandFlow>();
	        		Set<SaleItemEntity> orderItemList = order.getOrderItems();
	        		int counter = 1;
	        		for(SaleItemEntity sie:orderItemList){
	        			ExpandFlow ef = new ExpandFlow();
	            		ef.setLimitOilNo(sie.getProductCode());
	            		ef.setOilPrices(sie.getUnitPrice().toString());
	            		ef.setRefueling(sie.getQuantity().toString());
	            		ef.setAmount(sie.getAmount().toString());
	            		ef.setICCardBalance("0");
	            		ef.setListNo(Integer.toString(counter));
	            		ef.setPriceNoDiscount(sie.getAmount().toString());
	            		ef.setAmountNoDiscount(sie.getAmount().toString());
	            		ef.setPlatesNumber("");
	            		ef.setShift(order.getShiftNumber());
	            		eflist.add(ef);
	            		counter++;
	        		}
	        		
	        		
	        		body.setExpandFlowList(eflist);
	        		pay.setReqBody(body);
	        		String macStr = pay.getTxCode() + pay.getTxTime() + pay.getReqBody().getCardNo() + 
	        				pay.getReqBody().getOrgID() + pay.getReqBody().getFillingTime() + 
	        				pay.getReqBody().getDeductionType() + pay.getReqBody().getTotalAmount() +
	        				pay.getReqBody().getNumberOfDetail();
	        		String key = Utils.systemConfiguration.getProperty("ws.trans.key");
	        		pay.setTxMac(HMacUtil.encryptHMAC(macStr, key));
	        		
	        		XStream x = new XStream(new FormatedJsonHierarchicalStreamDriver());
	        		x.autodetectAnnotations(true);
	        		x.setMode(XStream.NO_REFERENCES);
	        		
	        		String reqInfo = x.toXML(pay);
	        		logger.info("【会员消费请求消息】：" + reqInfo);
	        		
	        		sendPayJsonInfo(reqInfo,order);
	            }
	            
	        }
		}

		@Override
		public void channelReadComplete(ChannelHandlerContext ctx) {
			ctx.channel().closeFuture().addListener(new ChannelFutureListener(){

				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					logger.info("支付完成");
				}
				
			});
		}
		
		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			// TODO Auto-generated method stub
			super.exceptionCaught(ctx, cause);
            ExecutorService es = Executors.newFixedThreadPool(1);
            es.execute(new ReqPayPasswd(this.cardNo,this.order));
            es.shutdown();
		}
	}
	
	public static Map<String,String> parseJsonData(String jsonData){
		GsonBuilder gb = new GsonBuilder();
		Gson g = gb.create();
		Map<String, String> map = g.fromJson(jsonData, new TypeToken<Map<String, String>>() {}.getType());
	    return map;
	}
	
	public class MemberPayInfoHandler extends ChannelHandlerAdapter{

        Order order;
		
		public MemberPayInfoHandler(Order order) {
			this.order = order;
		}

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			if (msg instanceof HttpResponse) {
	            HttpResponse response = (HttpResponse) msg;
	            System.out.println("CONTENT_TYPE:" + response.headers().get(HttpHeaderNames.CONTENT_TYPE));
	        }
	        if(msg instanceof HttpContent){
	            HttpContent content = (HttpContent)msg;
	            ByteBuf buf = content.content();
                String result = buf.toString(io.netty.util.CharsetUtil.UTF_8);
                if(result==null || result.trim().equals("")){
                    return;
                }
                System.out.println(result);
                buf.release();

                Map<String,String> map = parseJsonData(result);
                String reCode = map.get("RespCode");
                if ("00".equals(reCode)) {
                    callback.success();
                } else {
                    callback.fail(map.get("RespErrMsg"), null);
                }
	        }
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			// TODO Auto-generated method stub
			super.exceptionCaught(ctx, cause);
            callback.fail(cause.getMessage(), cause);
		}
		
	}
		
	public byte[] requestPosShow(Order order){
		
		byte[] stationId = Converts.str2Bcd(order.getMerchantId());
		int casherId = Integer.parseInt(order.getGenerator().split("\\|")[1]);
		byte[] casherNo = Converts.int2U16(casherId);
		byte[] magic = new byte[]{0x30,0x30,0x30,0x30};
		byte[] cmd = new byte[]{'7','3'};
		byte[] tag = new byte[]{'0','0'};
		
		byte[] tmp = Utils.concatTwoByteArray(stationId,casherNo);
		byte[] tmp1 = Utils.concatTwoByteArray(tmp, magic);
		byte[] tmp2 = Utils.concatTwoByteArray(tmp1, cmd);
		byte[] tmp3 = Utils.concatTwoByteArray(tmp2, tag);
		
		
		byte[] data = askPos2ShowPasswdInput(TransPosDataSender.MSG_INFO,"请输入支付密码").array();
		
		byte[] result = Utils.concatTwoByteArray(tmp3, data);
		return result;
		
	}
	
	private ByteBuf askPos2ShowPasswdInput(int msgType,String msg){
		byte[] msgBytes = msg.getBytes(Charset.forName("GB2312"));
		int msgLength = msgBytes.length;
		
		ByteBuf b = Unpooled.buffer();
		b.writeByte(msgLength);//显示信息长度
		b.writeByte(msgType);//显示消息类型
		b.writeBytes(msgBytes);//显示信息
		return b;
	}
}
