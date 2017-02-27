package com.bhz.eps;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bhz.eps.codec.WincorPosMsgDecoder;
import com.bhz.eps.codec.WincorPosMsgEncoder;
import com.bhz.eps.entity.DeviceRequest;
import com.bhz.eps.entity.DeviceResponse;
import com.bhz.eps.entity.Order;
import com.bhz.eps.util.Utils;
import com.thoughtworks.xstream.XStream;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;


public class DeviceService {
	private static final Logger logger = LogManager.getLogger(DeviceService.class);
	private String bposIP;
	private int bposPort;
	private static DeviceService service;
	
	
	private DeviceService(String bposIP,int bposPort){
		this.bposIP = bposIP;
		this.bposPort = bposPort;
		logger.trace("Initialize EPS DeviceService.");
	}
	
	public static DeviceService getInstance(String bposIP,int bposPort){
		if(service != null){
			return service;
		}
		service = new DeviceService(bposIP, bposPort);
		return service;
	}
	
	public void submitOrderToWechat(Order order){
		ExecutorService es = Executors.newFixedThreadPool(1);
		Future<Boolean> back = es.submit(new InvokeWechat(order));
		try{
			if(back.get()){
				//调用BPOS屏显请求，告知BPOS支付完成。
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		DeviceService ds = DeviceService.getInstance("localhost", 4050);
		
		try {
			//ds.askBPosDisplay("正在支付，请稍后...", "13572468");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void askBPosDisplay(final String message, final Order order) throws Exception{
        if ("0".equals(Utils.systemConfiguration.getProperty("eps.bpos.ds.send"))) {
            TransPosDataSender.getInstance(Utils.systemConfiguration.getProperty("trans.pos.ip"),
                    Integer.parseInt(Utils.systemConfiguration.getProperty("trans.pos.port"))).sendMsgToTransPos(order);
        } else {
            Bootstrap boot = new Bootstrap();
            EventLoopGroup worker = new NioEventLoopGroup();
            try {
                boot.group(worker).option(ChannelOption.TCP_NODELAY, true)
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Integer.parseInt(Utils.systemConfiguration.getProperty("eps.conn.bpos.timeout")))
                        .channel(NioSocketChannel.class)
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) throws Exception {
                                ch.pipeline().addLast(new WincorPosMsgEncoder());
                                ch.pipeline().addLast(new WincorPosMsgDecoder());
                                ch.pipeline().addLast(new DeviceServiceMessageHandler(message, order));
                            }

                        });
                ChannelFuture cf = boot.connect(this.bposIP, this.bposPort).sync();
                cf.addListener(new ChannelFutureListener() {

                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        logger.debug("Established connection to " + bposIP + " on port " + bposPort);
                    }

                });
                cf.channel().closeFuture().sync();
            } finally {
                worker.shutdownGracefully();
            }
        }
	}
}

class DeviceServiceMessageHandler extends SimpleChannelInboundHandler<DeviceResponse>{
	private static final Logger logger = LogManager.getLogger(DeviceServiceMessageHandler.class);
	String message = "";
	Order order;
	
	private static XStream xstream;
	static {
		xstream = new XStream();
		xstream.autodetectAnnotations(true);
		xstream.ignoreUnknownElements();
	}
	
	public DeviceServiceMessageHandler(String message, Order order) {
		this.message = message;
		this.order = order;
	}
	@Override
	protected void messageReceived(ChannelHandlerContext ctx, DeviceResponse msg) throws Exception {
		if(msg.getOverallResult().equals("Success")){
			//解析呼叫结果，如果请求成功，则将订单发送至微信公众号系统，否则返回。
			logger.debug("Processed sender: [ " + msg.getApplicationSender() + " ] 's order...");
			ctx.channel().pipeline().remove(this);
			//DeviceService.this.submitOrderToWechat(this.order);
            TransPosDataSender.getInstance(Utils.systemConfiguration.getProperty("trans.pos.ip"),
                    Integer.parseInt(Utils.systemConfiguration.getProperty("trans.pos.port"))).sendMsgToTransPos(order);
		}else{
			logger.error("No Response from BPOS on device request.");
		}
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		//发送屏显信息到BPOS
		logger.debug("Send Display request to BPOS");
		
		DeviceRequest dr = new DeviceRequest();
		dr.setRequestType("Output");
		dr.setApplicationSender(Utils.systemConfiguration.getProperty("eps.server.applicationSender"));
		dr.setWorkstationId(Utils.systemConfiguration.getProperty("eps.server.merchant.id"));
		dr.setRequestId(this.order.getOrderId().substring(this.order.getOrderId().length() - 8));
		dr.setSequenceId("1");
		
		DeviceRequest.Output op = new DeviceRequest.Output();
		op.setOutDeviceTarget("CashierDisplay");
		
		DeviceRequest.TextLine tl = new DeviceRequest.TextLine();
		tl.setMenuItem("");
		tl.setTimeout("");
		tl.setErase("true");
		tl.setContent(this.message);
		
		List<DeviceRequest.TextLine> textlineList = new ArrayList<DeviceRequest.TextLine>();
		textlineList.add(tl);
		op.setTextLines(textlineList);
		dr.setOutput(op);

		//DeviceRequest序列化为xml，并发送
		String drxml = xstream.toXML(dr);
		logger.debug(drxml);
		ctx.writeAndFlush(drxml);
	}
}



class InvokeWechat implements Callable<Boolean>{
	private static final Logger logger = LogManager.getLogger(InvokeWechat.class);
	
	private Order order;
	
	public InvokeWechat(Order order){
		this.order = order;
	}
	
	@Override
	public Boolean call() throws Exception {
		return null;
	}

}
