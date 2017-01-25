package com.bhz.eps.codec;

import java.util.List;

import com.bhz.eps.pdu.transpos.*;

import com.bhz.eps.util.Converts;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class TPDUDecoder extends ByteToMessageDecoder {

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		TPDU pdu = new TPDU();
		TPDUHeader pduHeader = new TPDUHeader();
		pduHeader.setSyncCode(in.readBytes(2).array());
		pduHeader.setLength(in.readUnsignedInt());
		pduHeader.setType(in.readByte());
		pduHeader.setPkgNum(in.readShort());
		pduHeader.setCrc8(in.readByte());
		//rewind
		in.readerIndex(0);
		pduHeader.setOriginalContent(in.readBytes(10).array());
		
		TPDUBody pduBody = new TPDUBody();
		BizPDUHeader bizHeader = new BizPDUHeader();
		BizPDUData bizData = new BizPDUData();
		BizPDUChecker bizChecker = new BizPDUChecker();
		bizHeader.setStationID(Converts.decodeBCD(in.readBytes(5)));
		bizHeader.setCashier(in.readUnsignedShort());
		bizHeader.setMagicNo(in.readUnsignedInt());
		bizHeader.setCmd(in.readBytes(2).array());
		bizHeader.setTag(in.readBytes(2).array());
		//rewind
		in.readerIndex(in.readerIndex()-15);
		bizHeader.setOriginalContent(in.readBytes(15).array());
		
		byte[] bizContent = new byte[in.readableBytes()-4];
		in.readBytes(bizContent);	
		bizData.setContent(bizContent);
		byte[] bizMac = new byte[4];
		in.readBytes(bizMac);
		bizChecker.setMac(bizMac);
		pduBody.setChecker(bizChecker);
		pduBody.setHeader(bizHeader);
		pduBody.setData(bizData);
		pdu.setHeader(pduHeader);
		pdu.setBody(pduBody);
		out.add(pdu);
	}
	
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		System.out.print("The Client is disconnected! The Channel related to this client will be closed! --> ");
		ctx.close();
		System.out.println("Channel CLOSE OK.");
	}
}
