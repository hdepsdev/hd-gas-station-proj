package com.bhz.eps.codec;

import java.util.List;

import com.bhz.eps.entity.Order;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bhz.eps.EPSServer;
import com.bhz.eps.pdu.transpos.*;
import com.bhz.eps.service.OrderService;
import com.bhz.eps.service.SaleItemService;
import com.bhz.eps.util.Converts;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.AttributeKey;

public class TPDUDecoder extends ByteToMessageDecoder {
	private static final Logger logger = LogManager.getLogger(TPDUDecoder.class);

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
		logger.info("The Client is disconnected! The Channel related to this client will be closed! --> ");
		ctx.close();
		logger.info("Channel CLOSE OK. --from " + this.getClass());
		try {
            Object o = ctx.channel().attr(AttributeKey.valueOf("orderId")).get();
            logger.debug("orderId: " + o);
            if (o != null) {
                logger.debug("update by orderId: " + o.toString());
                OrderService orderSrv = EPSServer.appctx.getBean("orderService", OrderService.class);
                Order order = orderSrv.getOrderbyId(o.toString());
                int status = order.getStatus();
                if (status == Order.STATUS_WAIT) {
                    order.setStatus(Order.STATUS_ERROR);
                    int i = orderSrv.updateOrder(order);
                    logger.debug("update eps_orders " + i + " rows");
                } else {
                    logger.debug("eps_orders status: " + status + ", cancel update");
                }
            }
        } catch (Exception e) {
            logger.error("", e);
        }
	}
}
