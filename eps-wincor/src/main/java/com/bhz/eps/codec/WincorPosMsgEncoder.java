package com.bhz.eps.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class WincorPosMsgEncoder extends MessageToByteEncoder<Object> {


    public WincorPosMsgEncoder() {
    	
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
    	
    	
    }
}
