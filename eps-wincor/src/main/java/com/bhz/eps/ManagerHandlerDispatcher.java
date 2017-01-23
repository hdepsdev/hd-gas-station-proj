package com.bhz.eps;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

/**
 * 管理接口分发器
 * @author yaoh
 *
 */

public class ManagerHandlerDispatcher extends ChannelHandlerAdapter {
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		ctx.close();
		cause.printStackTrace();
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		// 分发消息给对应的消息处理器
		ProtobufMsgDispatcher.submit(ctx.channel(), msg);
	}
}
