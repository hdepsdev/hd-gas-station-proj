package com.bhz.eps;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bhz.eps.codec.TPDUDecoder;
import com.bhz.eps.codec.TPDUEncoder;
import com.bhz.eps.entity.Order;
import com.bhz.eps.pdu.transpos.BizPDUData;
import com.bhz.eps.pdu.transpos.BizPDUHeader;
import com.bhz.eps.pdu.transpos.TPDU;
import com.bhz.eps.pdu.transpos.TPDUBody;
import com.bhz.eps.pdu.transpos.TPDUHeader;
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
	
	public void sendOrderToTransPos(Order order) throws Exception{
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
}

class TransPosOrderHandler extends SimpleChannelInboundHandler<Order>{
	private static final Logger logger = LogManager.getLogger(TransPosOrderHandler.class);
	Order order;
	
	public TransPosOrderHandler(Order order) {
		this.order = order;
	}
	@Override
	protected void messageReceived(ChannelHandlerContext ctx, Order msg) throws Exception {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		
		
		logger.debug("Send order to Trans POS.");
		ctx.flush();
	}
	
}
