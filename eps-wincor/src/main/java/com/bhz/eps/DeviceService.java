package com.bhz.eps;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bhz.eps.entity.DeviceResponse;
import com.bhz.eps.entity.Order;
import com.bhz.eps.processor.CardServiceRequestProcessor;
import com.bhz.eps.util.Utils;

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


public class DeviceService {
	private static final Logger logger = LogManager.getLogger(DeviceService.class);
	private String bposIP;
	private int bposPort;
	private static DeviceService service;
	
	
	private DeviceService(String bposIP,int bposPort){
		this.bposIP = bposIP;
		this.bposPort = bposPort;
		logger.trace("Initialize EPS DeviceService.");
	}
	
	public static DeviceService getInstance(String bposIP,int bposPort){
		if(service != null){
			return service;
		}
		service = new DeviceService(bposIP, bposPort);
		return service;
	}
	
	public void submitOrderToWechat(Order order){
		ExecutorService es = Executors.newFixedThreadPool(1);
		Future<Boolean> back = es.submit(new InvokeWechat(order));
		try{
			if(back.get()){
				//调用BPOS屏显请求，告知BPOS支付完成。
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
	
	
	public void askBPosDisplay(String message) throws Exception{
		Bootstrap boot = new Bootstrap();
		EventLoopGroup worker = new NioEventLoopGroup();
		try{
			boot.group(worker).option(ChannelOption.TCP_NODELAY, true)
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Integer.parseInt(Utils.systemConfiguration.getProperty("eps.conn.bpos.timeout")))
				.channel(NioSocketChannel.class)
				.handler(new ChannelInitializer<SocketChannel>(){	
					@Override
					protected void initChannel(SocketChannel ch) throws Exception {
						ch.pipeline().addLast(new DeviceServiceMessageHandler(message));
					}
					
				});
			ChannelFuture cf = boot.connect(this.bposIP, this.bposPort).sync();
			cf.addListener(new ChannelFutureListener() {
				
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					logger.debug("Established connection to " + bposIP + " on port " + bposPort);
				}
				
			});
			cf.channel().closeFuture().sync();
		}finally{
			worker.shutdownGracefully();
		}
	}
}

class DeviceServiceMessageHandler extends SimpleChannelInboundHandler<DeviceResponse>{
	private static final Logger logger = LogManager.getLogger(DeviceServiceMessageHandler.class);
	String message = "";
	public DeviceServiceMessageHandler(String message) {
		this.message = message;
	}
	@Override
	protected void messageReceived(ChannelHandlerContext ctx, DeviceResponse msg) throws Exception {
		if(msg.getOverallResult().equals("Success")){
			//解析呼叫结果，如果请求成功，则将订单发送至微信公众号系统，否则返回。
			logger.debug("Processed sender: [ " + msg.getApplicationSender() + " ] 's order...");
			ctx.channel().pipeline().remove(this);
			//发送订单至公众号系统
			DeviceService.this.submitOrderToWechat();
		}else{
			logger.error("No Response from BPOS on device request.");
		}
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		//发送屏显信息到BPOS
		logger.debug("Send Display request to BPOS");
		ctx.flush();
	}
}



class InvokeWechat implements Callable<Boolean>{
	private static final Logger logger = LogManager.getLogger(InvokeWechat.class);
	
	private Order order;
	
	public InvokeWechat(Order order){
		this.order = order;
	}
	
	@Override
	public Boolean call() throws Exception {
		return null;
	}

}
