package com.bhz.eps;

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
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.bhz.eps.msg.PaymentReqProto;
import com.bhz.eps.util.Utils;

public class DataCenter {
	private static final Logger logger = LogManager.getLogger(DataCenter.class);
	public final static ApplicationContext appctx = new ClassPathXmlApplicationContext(new String[]{"conf/application-context.xml"});
	
	public void start() throws Exception {
		EventLoopGroup acceptor = new NioEventLoopGroup();
		EventLoopGroup worker = new NioEventLoopGroup();
		ServerBootstrap sb = new ServerBootstrap();
		try{
			sb.group(acceptor, worker)
			.channel(NioServerSocketChannel.class)
			.option(ChannelOption.SO_BACKLOG, 4096)
			.childOption(ChannelOption.TCP_NODELAY, true)
			.childHandler(new ChannelInitializer<SocketChannel>(){

				@Override
				protected void initChannel(SocketChannel ch) throws Exception {
					ChannelPipeline pipeline = ch.pipeline();
					pipeline.addLast(new ProtobufVarint32FrameDecoder());
					pipeline.addLast(new ProtobufVarint32LengthFieldPrepender());
					pipeline.addLast(new ProtobufEncoder());
					pipeline.addLast(new ProtobufDecoder(PaymentReqProto.PaymentReq.getDefaultInstance()));
					pipeline.addLast(new PaymentOrderHandler());
				}
				
			});
			
			ChannelFuture cf = sb.bind(Integer.parseInt(Utils.systemConfiguration.getProperty("data.center.port"))).sync();
			cf.addListener(new ChannelFutureListener(){

				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					logger.info("Data Center is started and listening on port " + Utils.systemConfiguration.getProperty("data.center.port"));
				}
				
			});
			cf.channel().closeFuture().sync();
		}finally{
			worker.shutdownGracefully();
			acceptor.shutdownGracefully();
		}
	}
}
