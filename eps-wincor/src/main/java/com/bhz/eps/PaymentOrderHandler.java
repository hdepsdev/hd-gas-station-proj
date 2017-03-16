package com.bhz.eps;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.bhz.eps.processor.PaymentOrderProcessor;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

/**
 * 管理接口分发器
 * @author yangxb
 *
 */

public class PaymentOrderHandler extends ChannelHandlerAdapter{
	private static final int MAX_THREAD_NUM = 10;
	
	private static ExecutorService executorService = Executors.newFixedThreadPool(MAX_THREAD_NUM);
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		ctx.close();
		cause.printStackTrace();
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

		PaymentOrderProcessor processor = new PaymentOrderProcessor();
		processor.setChannel(ctx.channel());
		processor.setMsgObject(msg);
		
		executorService.submit(processor);
	}
}
