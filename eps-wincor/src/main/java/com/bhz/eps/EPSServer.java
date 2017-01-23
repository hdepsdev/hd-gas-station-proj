package com.bhz.eps;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.bhz.eps.codec.WincorPosMsgDecoder;
import com.bhz.eps.codec.WincorPosMsgEncoder;
import com.bhz.eps.util.ClassUtil;
import com.bhz.eps.util.Utils;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * 服务端启动类
 * @author yaoh
 *
 */
public class EPSServer {
	
	private static final Logger logger = LogManager.getLogger(EPSServer.class);
	public final static ApplicationContext appctx = new ClassPathXmlApplicationContext(new String[]{"conf/application-context.xml"});
	
	public void start() throws Exception {
		EventLoopGroup acceptor = new NioEventLoopGroup();
		EventLoopGroup worker = new NioEventLoopGroup();
		ServerBootstrap sb = new ServerBootstrap();
		try{
			sb.group(acceptor, worker)
			.channel(NioServerSocketChannel.class)
			.option(ChannelOption.SO_BACKLOG, 2048)
			.childOption(ChannelOption.TCP_NODELAY, true)
			.childHandler(new ChannelInitializer<SocketChannel>(){

				@Override
				protected void initChannel(SocketChannel ch) throws Exception {
					ChannelPipeline pipeline = ch.pipeline();
					pipeline.addLast(new WincorPosMsgEncoder());
					pipeline.addLast("WincorPosMsgLength",new LengthFieldBasedFrameDecoder(4096,0,4,0,0));
					pipeline.addLast(new WincorPosMsgDecoder());
					pipeline.addLast("BizDispatcher",new BizHandlerDispatcher());
				}
				
			});
			
			ChannelFuture cf = sb.bind(Integer.parseInt(Utils.systemConfiguration.getProperty("eps.server.port"))).sync();
			cf.addListener(new ChannelFutureListener(){

				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					logger.info("Server is started and listening on port " + Utils.systemConfiguration.getProperty("eps.server.port"));
				}
				
			});
			cf.channel().closeFuture().sync();
		}finally{
			worker.shutdownGracefully();
			acceptor.shutdownGracefully();
		}
	}
	
	public static void main(String[] args) throws Exception{
		ClassUtil.initTypeToProcessorClassMap();
		if(Utils.systemConfiguration.getProperty("eps.server.data.upload.need").equalsIgnoreCase("true")){
			startEPSDataUploader();
		}
		startConfigureManager();
		EPSServer b = new EPSServer();
		b.start();
	}
	
	private static void startEPSDataUploader(){
		ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);
		ses.scheduleAtFixedRate(new RunEPSDataUploader(), 0, 
				Integer.parseInt(Utils.systemConfiguration.getProperty("eps.server.data.upload.interval")), 
				TimeUnit.SECONDS);
	}
	
	private static void startConfigureManager(){
		ExecutorService es = Executors.newFixedThreadPool(1);
		es.execute(new RunEPSConfigureManager());
	}
}

class RunEPSDataUploader implements Runnable{
	private static final Logger logger = LogManager.getLogger(RunEPSDataUploader.class);
	@Override
	public void run() {
		EPSServerDataManager dataMgr = EPSServerDataManager.getInstance(Utils.systemConfiguration.getProperty("eps.data.server.ip"), 
				Integer.parseInt(Utils.systemConfiguration.getProperty("eps.data.server.port")));
		try {
			dataMgr.submitTask();
		} catch (Exception e) {
			if(e instanceof java.net.ConnectException){
				logger.error(e.getMessage());
//				System.out.println(e.getMessage());
			}else{
				e.printStackTrace();
			}
		}
	}
	
}

class RunEPSConfigureManager implements Runnable{

	@Override
	public void run() {
		EPSConfigureManager ecm = new EPSConfigureManager();
		try {
			ecm.startConfigureManager();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
