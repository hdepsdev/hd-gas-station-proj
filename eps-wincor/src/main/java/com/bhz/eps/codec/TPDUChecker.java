package com.bhz.eps.codec;

import java.util.Arrays;

import com.bhz.eps.pdu.transpos.BizPDUHeader;
import com.bhz.eps.pdu.transpos.TPDU;
import com.bhz.eps.util.CRC8;
import com.bhz.eps.util.TransPosMessageEncryption;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.CharsetUtil;

public class TPDUChecker extends ChannelHandlerAdapter {

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		// TODO Auto-generated method stub
		super.exceptionCaught(ctx, cause);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		TPDU tpdu = (TPDU)msg;
		//对包头检查CRC8
		if(CRC8.calc(tpdu.getHeader().getOriginalContent()) != 0){
			ctx.write(Unpooled.copiedBuffer("CRC8 Error", CharsetUtil.ISO_8859_1));
			return;
		}
		//对业务数据计算MAC
		ByteBuf bizBuf = Unpooled.buffer();
		BizPDUHeader bizHeader = tpdu.getBody().getHeader();
		bizBuf.writeBytes(bizHeader.getOriginalContent());
		bizBuf.writeBytes(tpdu.getBody().getData().getContent());
		byte[] macValue = TransPosMessageEncryption.getPOSMac(bizBuf.array());
		if(!Arrays.equals(macValue, tpdu.getBody().getChecker().getMac())){
			ctx.write(Unpooled.copiedBuffer("MAC verification FAILED!",CharsetUtil.ISO_8859_1));
			return;
		}
		
		ctx.fireChannelRead(msg);
	}

}
