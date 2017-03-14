package com.bhz.eps.pay;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import com.bhz.eps.entity.MemberPayRequest;
import com.bhz.eps.entity.MemberPayRequest.ExpandFlow;
import com.bhz.eps.entity.MemberPayRequest.ReqBody;
import com.bhz.eps.util.FormatedJsonHierarchicalStreamDriver;
import com.bhz.eps.util.Utils;
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
	
	static {
		MEMBER_PAY_URL = Utils.systemConfiguration.getProperty("memberpay.url").trim();
		MEMBER_PAY_HOST = Utils.systemConfiguration.getProperty("memberpay.host").trim();
		MEMBER_PAY_PORT = Integer.parseInt(Utils.systemConfiguration.getProperty("memberpay.port").trim());
	}
	
	public void sendPayJsonInfo(final String req){
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
				ch.pipeline().addLast(new MemberPayInfoHandler());
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
	
	public static class MemberPayInfoHandler extends ChannelHandlerAdapter{

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			if (msg instanceof HttpResponse) {
	            HttpResponse response = (HttpResponse) msg;
	            System.out.println("CONTENT_TYPE:" + response.headers().get(HttpHeaderNames.CONTENT_TYPE));
	        }
	        if(msg instanceof HttpContent){
	            HttpContent content = (HttpContent)msg;
	            ByteBuf buf = content.content();
	            System.out.println(buf.toString(io.netty.util.CharsetUtil.UTF_8));
	            buf.release();
	        }
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			// TODO Auto-generated method stub
			super.exceptionCaught(ctx, cause);
		}
		
	}
	
	public static void main(String[] args) {
		MemberPay mp = new MemberPay();
		MemberPayRequest pay = new MemberPayRequest();
		pay.setTxCode("3103");
		pay.setTxDate("20170314");
		pay.setMerchantNo("100");
		pay.setReqSeqNo("");
		pay.setTxTime("");
		pay.setBrchNo("");
		pay.setBrchName("");
		pay.setTellerNo("");
		pay.setTellerName("");
		pay.setTxMac("");
		ReqBody body = new ReqBody();
		body.setCardNo("");
		body.setOrgID("");
		body.setFillingTime("");
		body.setDeductionType("");
		body.setTotalAmount("");
		body.setNumberOfDetail("");
		
		
		List<ExpandFlow> eflist = new ArrayList<ExpandFlow>();
		ExpandFlow ef = new ExpandFlow();
		ef.setLimitOilNo("");
		ef.setOilPrices("");
		ef.setRefueling("");
		ef.setAmount("");
		ef.setICCardBalance("");
		ef.setListNo("");
		ef.setPriceNoDiscount("");
		ef.setAmountNoDiscount("");
		ef.setPlatesNumber("");
		ef.setShift("");
		eflist.add(ef);
		
		body.setExpandFlowList(eflist);
		pay.setReqBody(body);
		
		
		
		
		XStream x = new XStream(new FormatedJsonHierarchicalStreamDriver());
		x.autodetectAnnotations(true);
		x.setMode(XStream.NO_REFERENCES);
		
		String reqInfo = x.toXML(pay);
		mp.sendPayJsonInfo(reqInfo);
		
	}
}
