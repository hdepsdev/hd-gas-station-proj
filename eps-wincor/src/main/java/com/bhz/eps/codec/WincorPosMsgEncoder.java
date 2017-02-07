package com.bhz.eps.codec;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bhz.eps.pdu.WincorPosPDU;
import com.thoughtworks.xstream.XStream;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class WincorPosMsgEncoder extends MessageToByteEncoder<Object> {
	
	private static XStream xstream;
	static {
		xstream = new XStream();
		xstream.autodetectAnnotations(true);
		xstream.ignoreUnknownElements();
	}
	
	private static final Logger logger = LogManager.getLogger(WincorPosMsgDecoder.class);

    public WincorPosMsgEncoder() {
    	
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
    	String content = (String)msg;
    	content = WincorPosPDU.XML_HEADER + content;
    	
    	logger.info(content);
    	
    	int length = content.getBytes("UTF-8").length;
    	byte[] lv = new byte[length + 4];
    	lv[0] = (byte)(length>>24);
    	lv[1] = (byte)(length>>16);
    	lv[2] = (byte)(length>>8);
    	lv[3] = (byte)(length&0xFF);
    	System.arraycopy(content.getBytes("UTF-8"), 0, lv, 4, length);
    	
    	out.writeBytes(lv);
    }
}
