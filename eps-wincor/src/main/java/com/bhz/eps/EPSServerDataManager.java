package com.bhz.eps;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bhz.eps.msg.PaymentReqProto;
import com.bhz.eps.msg.PaymentReqProto.PaymentReq;
import com.bhz.eps.msg.PaymentRespProto;
import com.bhz.eps.msg.PaymentRespProto.PaymentResp;
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
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

public class EPSServerDataManager {
	private static final Logger logger = LogManager.getLogger(EPSServerDataManager.class);
	private String hostIP;
	private int hostPort;
	private static EPSServerDataManager manager;
	
	private EPSServerDataManager(String hostIP,int hostPort){
		this.hostIP = hostIP;
		this.hostPort = hostPort;
		logger.trace("Initialize EPS Data Manager.");
	}
	
	public static EPSServerDataManager getInstance(String hostIP,int hostPort){
		if(manager != null){
			return manager;
		}
		manager = new EPSServerDataManager(hostIP, hostPort);
		return manager;
	}
	
	public void submitTask(){
		ExecutorService es = Executors.newFixedThreadPool(10);
		Future<List<PaymentReq>> back = (Future<List<PaymentReq>>)es.submit(new DataSubmitter());
		try {
			List<PaymentReq> result = back.get();
			if(!result.isEmpty()){
				submitTransData(result);
			}
		} catch (InterruptedException e) {
			logger.error(e.getMessage());
		} catch (ExecutionException e) {
			logger.error(e.getMessage());
		} catch (Exception e) {
			logger.error(e.getMessage());
		} finally{
			es.shutdown();
		}
	}
	
	private void submitTransData(List<PaymentReq> payload) throws Exception{
		Bootstrap boot = new Bootstrap();
		EventLoopGroup worker = new NioEventLoopGroup();
		try{
			boot.group(worker).option(ChannelOption.TCP_NODELAY, true)
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Integer.parseInt(Utils.systemConfiguration.getProperty("eps.client.data.upload.timeout")))
				.channel(NioSocketChannel.class)
				.handler(new ChannelInitializer<SocketChannel>(){	
					@Override
					protected void initChannel(SocketChannel ch) throws Exception {
						ch.pipeline().addLast(new ProtobufVarint32FrameDecoder());
						ch.pipeline().addLast(new ProtobufVarint32LengthFieldPrepender());
						ch.pipeline().addLast(new ProtobufEncoder());
						ch.pipeline().addLast(new ProtobufDecoder(PaymentRespProto.PaymentResp.getDefaultInstance()));
						ch.pipeline().addLast(new EPSClientHandler(payload));
					}
					
				});
			ChannelFuture cf = boot.connect(this.hostIP, this.hostPort).sync();
			cf.addListener(new ChannelFutureListener() {
				
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					logger.debug("Established connection to " + hostIP + " on port " + hostPort);
				}
				
			});
			cf.channel().closeFuture().sync();
		}finally{
			worker.shutdownGracefully();
		}
	}
}

class EPSClientHandler extends SimpleChannelInboundHandler<PaymentRespProto.PaymentResp>{
	private static final Logger logger = LogManager.getLogger(EPSClientHandler.class);
	List<PaymentReq> payload = new ArrayList<PaymentReq>();
	public EPSClientHandler(List<PaymentReq> payload) {
		this.payload = payload;
	}
	@Override
	protected void messageReceived(ChannelHandlerContext ctx, PaymentResp msg) throws Exception {
		if(msg.getResult().getResultCode().equals("1")){
			//Do upload
			logger.debug("Processed work order [ " + msg.getWorkOrder() + " ] ");
		}else{
			logger.error(msg.getResult().getErrorCause());
		}
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		for(PaymentReq req:payload){
			ctx.write(req);
		}
		logger.debug("Upload Nozzle orders.");
		ctx.flush();
	}
}

class DataSubmitter implements Callable<List<PaymentReqProto.PaymentReq>>{
	private static final Logger logger = LogManager.getLogger(DataSubmitter.class);
	
	public DataSubmitter(){
		
	}
	
	@Override
	public List<PaymentReq> call() throws Exception {
		List<PaymentReq> result = new ArrayList<PaymentReq>();
		
		return result;
	}

}
