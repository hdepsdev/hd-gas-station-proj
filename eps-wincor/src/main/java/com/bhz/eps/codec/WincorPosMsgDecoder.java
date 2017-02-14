package com.bhz.eps.codec;

import com.bhz.eps.entity.CardServiceRequest;
import com.bhz.eps.entity.DeviceResponse;
import com.bhz.eps.pdu.WincorPosPDU;
import com.thoughtworks.xstream.XStream;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class WincorPosMsgDecoder extends ByteToMessageDecoder {

	private static XStream xstream;
	static {
		xstream = new XStream();
		xstream.autodetectAnnotations(true);
		xstream.ignoreUnknownElements();
	}
	
	private static final Logger logger = LogManager.getLogger(WincorPosMsgDecoder.class);
	
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		
		WincorPosPDU pdu = new WincorPosPDU();
	
		pdu.setLength(in.readUnsignedInt());
		
		byte[] content = new byte[in.readableBytes()];
		in.readBytes(content);
		String xmlContent = new String(content,"utf-8");
		
		logger.info(xmlContent);
		
		if (xmlContent.contains("RequestType=\"CardPayment\"")) {
			xstream.alias("CardServiceRequest", CardServiceRequest.class);
			xstream.alias("POSdata", CardServiceRequest.PosData.class);
			xstream.alias("SaleItem", CardServiceRequest.SaleItem.class);
			CardServiceRequest request = (CardServiceRequest) xstream
					.fromXML(xmlContent);
			out.add(request);
		}
		else if (xmlContent.contains("RequestType=\"Output\"")) {
			xstream.alias("DeviceResponse", DeviceResponse.class);
			xstream.alias("Output", DeviceResponse.Output.class);
			DeviceResponse response = (DeviceResponse) xstream
					.fromXML(xmlContent);
			out.add(response);
		}
		
	}
	
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		logger.info("The Client is disconnected! The Channel related to this client will be closed! --> ");
		ctx.close();
		logger.info("Channel CLOSE OK. --from " + this.getClass());
        //test code
        /*
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
        */
	}
}
