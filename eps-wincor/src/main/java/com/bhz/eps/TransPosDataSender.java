package com.bhz.eps;

import com.alipay.demo.trade.config.Configs;
import com.alipay.demo.trade.model.ExtendParams;
import com.alipay.demo.trade.model.GoodsDetail;
import com.alipay.demo.trade.model.builder.AlipayTradePayRequestBuilder;
import com.alipay.demo.trade.model.result.AlipayF2FPayResult;
import com.alipay.demo.trade.service.AlipayTradeService;
import com.alipay.demo.trade.service.impl.AlipayTradeServiceImpl;
import com.bhz.eps.codec.TPDUDecoder;
import com.bhz.eps.codec.TPDUEncoder;
import com.bhz.eps.entity.*;
import com.bhz.eps.pay.MemberPay;
import com.bhz.eps.pay.MemberPayCallbackInterface;
import com.bhz.eps.pdu.transpos.TPDU;
import com.bhz.eps.service.OrderService;
import com.bhz.eps.service.impl.AuthcodeToOpenidServiceImpl;
import com.bhz.eps.util.Converts;
import com.bhz.eps.util.Utils;
import com.bhz.fcomc.service.PreferentialPriceService;
import com.bhz.point.calc.entity.OilSale;
import com.bhz.point.calc.service.PolicyRuleService;
import com.bhz.posserver.entity.request.PreferentialPriceDetailsRequest;
import com.bhz.posserver.entity.request.PreferentialPriceRequest;
import com.bhz.posserver.entity.response.PreferentialPriceDetailsResponse;
import com.bhz.posserver.entity.response.PreferentialPriceResponse;
import com.google.gson.Gson;
import com.tencent.WXPay;
import com.tencent.business.ScanPayBusiness.ResultListener;
import com.tencent.protocol.pay_protocol.ScanPayReqData;
import com.tencent.protocol.pay_protocol.ScanPayResData;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import lombok.Data;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;

public class TransPosDataSender {
	private static final Logger logger = LogManager.getLogger(TransPosDataSender.class);
	private String transPosIP;
	private int transPosPort;
	
	public final static int MSG_INFO=0x01;
	public final static int MSG_ERROR=0x02;
	
	private static TransPosDataSender sender;
	
	private TransPosDataSender(String ip,int port){
		this.transPosIP = ip;
		this.transPosPort = port;
		logger.trace("Initialize TransPOS Data Sender.");
	}
	
	public static TransPosDataSender getInstance(String ip,int port){
		if(sender == null){
			return new TransPosDataSender(ip, port);
		}else{
			return sender;
		}
	}
	
	/**
	 * 发送支付请求信息
	 * @param payMethodList 支付方式列表
	 * @throws Exception
	 */
	public void selectPayMethodToPos(List<PayMethod> payMethodList,Order order) throws Exception{
		Bootstrap boot = new Bootstrap();
		EventLoopGroup worker = new NioEventLoopGroup();
		try{
			boot.group(worker).option(ChannelOption.TCP_NODELAY, true)
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Integer.parseInt(Utils.systemConfiguration.getProperty("eps.client.data.upload.timeout")))
				.channel(NioSocketChannel.class)
				.handler(new ChannelInitializer<SocketChannel>(){

					@Override
					protected void initChannel(SocketChannel ch) throws Exception {
						ch.pipeline().addLast(new TPDUEncoder());
						ch.pipeline().addLast(new TPDUDecoder());
						ch.pipeline().addLast(new SelectPayMethodHandler(payMethodList,order));
					}
					
				});
			ChannelFuture cf = boot.connect(this.transPosIP, this.transPosPort).sync();
			cf.addListener(new ChannelFutureListener() {
				
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					logger.debug("Established connection to " + transPosIP + " on port " + transPosPort);
				}
				
			});
			cf.channel().closeFuture().sync();
				
		}finally{
			worker.shutdownGracefully();
		}
	}
	
	/**
	 * 发送订单信息给交易POS，交易POS生成订单二维码
	 * @param order 订单信息
	 * @throws Exception
	 */
	public void sendOrderToTransPos(final Order order) throws Exception{
		Bootstrap boot = new Bootstrap();
		EventLoopGroup worker = new NioEventLoopGroup();
		try{
			boot.group(worker).option(ChannelOption.TCP_NODELAY, true)
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Integer.parseInt(Utils.systemConfiguration.getProperty("eps.client.data.upload.timeout")))
				.channel(NioSocketChannel.class)
				.handler(new ChannelInitializer<SocketChannel>(){

					@Override
					protected void initChannel(SocketChannel ch) throws Exception {
						ch.pipeline().addLast(new TPDUEncoder());
						ch.pipeline().addLast(new TPDUDecoder());
						ch.pipeline().addLast(new TransPosOrderHandler(order));
					}
					
				});
			ChannelFuture cf = boot.connect(this.transPosIP, this.transPosPort).sync();
			cf.addListener(new ChannelFutureListener() {
				
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					logger.debug("Established connection to " + transPosIP + " on port " + transPosPort);
				}
				
			});
			cf.channel().closeFuture().sync();
				
		}finally{
			worker.shutdownGracefully();
		}
	}

    /**
     * 发送信息给交易POS
     * @param order 订单信息
     * @throws Exception
     */
    public void sendMsgToTransPos(final Order order) throws Exception{
        if(Utils.systemConfiguration.getProperty("scan.type").equalsIgnoreCase("initiative")){
            sendOrderToTransPos(order);
        }else{
            selectPayMethodToPos(Utils.PAY_METHOD_LIST, order);
        }
    }
	
	/**
	 * 请求POS打单
	 * @param order 订单信息
	 * @throws Exception
	 */
	public void askPosToPrintReceipt(final Order order) throws Exception{
		Bootstrap boot = new Bootstrap();
		EventLoopGroup worker = new NioEventLoopGroup();
		try{
			boot.group(worker).option(ChannelOption.TCP_NODELAY, true)
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Integer.parseInt(Utils.systemConfiguration.getProperty("eps.client.data.upload.timeout")))
				.channel(NioSocketChannel.class)
				.handler(new ChannelInitializer<SocketChannel>(){

					@Override
					protected void initChannel(SocketChannel ch) throws Exception {
						ch.pipeline().addLast(new TPDUEncoder());
						ch.pipeline().addLast(new TPDUDecoder());
						ch.pipeline().addLast(new SendReceiptHandler(order));
					}
					
				});
			ChannelFuture cf = boot.connect(this.transPosIP, this.transPosPort).sync();
			cf.addListener(new ChannelFutureListener() {
				
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					logger.debug("Established connection to " + transPosIP + " on port " + transPosPort);
				}
				
			});
			cf.channel().closeFuture().sync();
				
		}finally{
			worker.shutdownGracefully();
		}
	}
}

class SelectPayMethodHandler extends SimpleChannelInboundHandler<TPDU>{
	private static final Logger logger = LogManager.getLogger(TransPosOrderHandler.class);
    private static final String CARD_NO_DEF = "99000211111200001000";//所有非会员消费卡号
    // 支付宝当面付2.0服务
    private static AlipayTradeService tradeService;
    {
    	/** 一定要在创建AlipayTradeService之前调用Configs.init()设置默认参数
         *  Configs会读取classpath下的zfbinfo.properties文件配置信息，如果找不到该文件则确认该文件是否在classpath目录
         */
        Configs.init("conf/zfbinfo.properties");

        /** 使用Configs提供的默认参数
         *  AlipayTradeService可以使用单例或者为静态成员对象，不需要反复new
         */
        tradeService = new AlipayTradeServiceImpl.ClientBuilder().build();
    }
    private OrderService orderService = EPSServer.appctx.getBean("orderService", OrderService.class);
    
	List<PayMethod> pmList;
	Order order;
    String cardNo = CARD_NO_DEF;
	
	public SelectPayMethodHandler(List<PayMethod> pmList,Order order) {
		this.pmList = pmList;
		this.order = orderService.getOrderWithSaleItemsById(order.getOrderId());//重新查询以便获取明细数据
	}
	
	@Override
	protected void messageReceived(ChannelHandlerContext ctx, TPDU msg) throws Exception {
		logger.info("Receive POS Response[ " + msg  + " ]");
        try {
        	//解析返回消息
        	byte[] tagBytes = msg.getBody().getHeader().getTag();
        	//当消息为非正常选择，则循环调用选择列表请求。
        	if(tagBytes[0]!='0' || tagBytes[1]!='0'){
        		TransPosDataSender tpd = TransPosDataSender.getInstance(Utils.systemConfiguration.getProperty("trans.pos.ip"),
                        Integer.parseInt(Utils.systemConfiguration.getProperty("trans.pos.port")));
        		tpd.selectPayMethodToPos(Utils.PAY_METHOD_LIST, order);
        		ctx.channel().close();
        		return;
        	}
        	//解析POS返回数据
        	byte[] bytes = msg.getBody().getData().getContent();
        	
        	ByteArrayInputStream reader = new ByteArrayInputStream(bytes);
        	int type = reader.read();//支付类型
        	byte[] bytes_code = new byte[20];
        	reader.read(bytes_code);
        	String code = new String(bytes_code).trim();//条码

            discount(type, code);//计算优惠
            
            //判断支付方式
            if (type == 1|| type == 2){
                //组织网络支付请求发送到DataCenter
        		DataCenterOrderSender dcos = DataCenterOrderSender.getInstance(Utils.systemConfiguration.getProperty("data.center.ip"),
                        Integer.parseInt(Utils.systemConfiguration.getProperty("data.center.port")));
                dcos.askDataCenterToPay(order, type, code);
                ctx.channel().close();
            }else if(type==3){//如果是储值会员
            	MemberPay mp = new MemberPay(new MemberPayCallbackInterface() {
                    @Override
                    public void success() {
                        paySuccess();
                    }

                    @Override
                    public void fail(String msg, Throwable e) {
                        payFail();
                    }
                });
            	mp.requestPassword(code,order);
            }
        } catch(Exception e) {
            logger.error("", e);
        }
	}
	
	@Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        logger.error("", cause);
        super.exceptionCaught(ctx, cause);
    }
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		showPayList(ctx);
	}
	
	private void showPayList(ChannelHandlerContext ctx){
		byte[] stationId = Converts.str2Bcd(order.getMerchantId());
		int casherId = Integer.parseInt(order.getGenerator().split("\\|")[1]);
		byte[] casherNo = Converts.int2U16(casherId);
		byte[] magic = new byte[]{0x30,0x30,0x30,0x30};
		byte[] cmd = new byte[]{'7','2'};
		byte[] tag = new byte[]{'0','0'};
		
		byte[] tmp = Utils.concatTwoByteArray(stationId,casherNo);
		byte[] tmp1 = Utils.concatTwoByteArray(tmp, magic);
		byte[] tmp2 = Utils.concatTwoByteArray(tmp1, cmd);
		byte[] tmp3 = Utils.concatTwoByteArray(tmp2, tag);
		
		
		byte[] data = askPos2PayList(TransPosDataSender.MSG_INFO,"请选择支付方式").array();
		
		byte[] result = Utils.concatTwoByteArray(tmp3, data);
		
		logger.debug("Send pay method(s) to Trans POS.");
        logger.debug(Arrays.toString(result));
		ctx.writeAndFlush(result);
	}
	
	private ByteBuf askPos2PayList(int msgType,String msg){
		byte[] msgBytes = msg.getBytes(Charset.forName("GB2312"));
		int msgLength = msgBytes.length;
		
		ByteBuf b = Unpooled.buffer();
		int pmCount = pmList.size();
		b.writeByte(msgLength);//显示信息长度
		b.writeByte(msgType);//显示消息类型
		b.writeBytes(msgBytes);//显示信息
		b.writeByte(pmCount);//总共支付列表的长度
		for(PayMethod pm: pmList){
			b.writeByte(pm.getPayMethodCode());
			b.writeBytes(Utils.convertGB(pm.getPayMethodName(),20).getBytes(Charset.forName("GB2312")));
		}
		return b;
	}
	
	/**
	 * 计算优惠
	 */
	private void discount(int type, String code) {
        try {
            // 获取用户信息
            if (type == 1) {// 只有微信支付可以获取用户数据
                AuthcodeToOpenidServiceImpl atoser = new AuthcodeToOpenidServiceImpl();
                AuthcodeToOpenidResData result = atoser.request(new AuthcodeToOpenidReqData(code));
                if ("SUCCESS".equals(result.getReturn_code()) && "SUCCESS".equals(result.getResult_code())) {
                    String openId = result.getOpenid();
                    // TODO 通过openid获取卡号，再根据卡号进行优惠查询和积分
                    //cardNo =
                    logger.debug("-----------------openid:" + openId + "----------------");
                } else {
                    logger.error("获取openid失败！");
                    payFail();
                }
            } else if (type == 2) {// 支付宝用户按非会员消费

            } else if (type == 3) {// 会员消费时，code就是卡号
                cardNo = code;
            }
        } catch (Exception e) {
            payFail();
            logger.error("", e);
        }

        PreferentialPriceService ps = (PreferentialPriceService) EPSServer.appctx.getBean("preferentialPriceService", PreferentialPriceService.class);
        //优惠查询请求对象
        PreferentialPriceRequest pps = new PreferentialPriceRequest();
        //消费时间
        pps.setTransactionTime(new Date().getTime());
        //卡类型
        pps.setCardType("I");
        //卡号
        pps.setCardNo(cardNo);
        //版本号 固定1
        pps.setDbVersion(1);
        //优惠查询请求明细集合
        List<PreferentialPriceDetailsRequest> details = new ArrayList<PreferentialPriceDetailsRequest>();
        //积分
        int point = 0;
        for (SaleItemEntity entity : order.getOrderItems()) {
            //优惠查询请求明细
            PreferentialPriceDetailsRequest ppdr = new PreferentialPriceDetailsRequest();
            //折前金额
            ppdr.setAmountNoDiscount(entity.getAmount());
            //折前单价
            ppdr.setPriceNoDiscount(entity.getUnitPrice());
            //消费数量
            ppdr.setQuantity(entity.getQuantity());
            //油品号
            ppdr.setOilCode(entity.getProductCode());
            details.add(ppdr);

            //根据卡号进行积分
            PolicyRuleService policyRuleService = (PolicyRuleService)EPSServer.appctx.getBean("pointRuleService", PolicyRuleService.class);
            OilSale oilSale = new OilSale();
            oilSale.setCardId(cardNo);
            oilSale.setOrgId("1111111111");
            oilSale.setAmount(entity.getAmount().doubleValue());
            oilSale.setOilPrices(entity.getUnitPrice().doubleValue());
            oilSale.setRefueling(entity.getQuantity().doubleValue());
            oilSale.setOilId(entity.getProductCode());
            oilSale.setFillingTime(new Date());
            try {
                point += policyRuleService.preCalcPoint(oilSale);
            } catch (Exception e) {
                logger.error("积分异常", e);
            }
        }
        order.setLoyaltyPoint(new BigDecimal(point));
        pps.setDetails(details);
        PreferentialPriceResponse ppr = ps.queryPreferentialPrice(Long.valueOf(Utils.systemConfiguration.getProperty("eps.server.merchant.id")), pps);
        if(ppr.getDetails()==null || ppr.getDetails().isEmpty()){
        	order.setPaymentAmount(order.getOriginalAmount());
        	order.setCouponAmount(new BigDecimal(0));
        }else{
        	BigDecimal total = new BigDecimal(0);
            for (PreferentialPriceDetailsResponse ppdr : ppr.getDetails()) {
                total = total.add(ppdr.getAmountWithDiscount());
            }
            
            order.setPaymentAmount(total);
            order.setCouponAmount(order.getOriginalAmount().subtract(total));
        }
        
        //discountType 优惠类型 0:不满足优惠条件 1:有优惠 2:无优惠
        if (ppr.getDiscountType() == 1) {
            logger.debug("客户享受刷卡优惠,折后金额:" + order.getPaymentAmount());
        } else if (ppr.getDiscountType() == 0) {
            logger.debug("客户不满足优惠条件");
        } else if (ppr.getDiscountType() == 2) {
            logger.debug("客户已设置为无优惠");
        }

        if (cardNo != null && !cardNo.trim().equals("") && !cardNo.trim().equals(CARD_NO_DEF)) {
            //使用优惠券
            try {
                HttpClient client = HttpClients.createDefault();
                HttpPost post = new HttpPost("URL");
                List<NameValuePair> param = new ArrayList<NameValuePair>();
                param.add(new BasicNameValuePair("cardId", cardNo));
                post.setEntity(new UrlEncodedFormEntity(param));
                HttpResponse resp = client.execute(post);
                String result = EntityUtils.toString(resp.getEntity());
                Gson gson = new Gson();
                Map json = gson.fromJson(result, Map.class);
                if ((Boolean) json.get("success")) {
                    List<Map> list = (List<Map>)json.get("list");
                    List<Coupon> couponList = new ArrayList<Coupon>();
                    //TODO 将返回的优惠券数据封装为优惠券对象
                    //选择优惠券
                    Coupon coupon = selectCoupon(couponList);
                    //使用优惠券
                    if (coupon != null) {
                        post = new HttpPost("URL");
                        param = new ArrayList<NameValuePair>();
                        param.add(new BasicNameValuePair("cardId", cardNo));
                        param.add(new BasicNameValuePair("couponId", coupon.getId()));
                        post.setEntity(new UrlEncodedFormEntity(param));
                        resp = client.execute(post);
                        result = EntityUtils.toString(resp.getEntity());
                        json = gson.fromJson(result, Map.class);
                        if ((Boolean) json.get("success")) {
                            //TODO 使用成功，改变订单中金额
                        }
                    }
                }
            } catch (IOException e) {
                logger.error("使用优惠券异常", e);
            }
        }
	}

    private Coupon selectCoupon(List<Coupon> list) {
        //TODO 取优惠幅度最大的优惠券
        return null;
    }
	
	/**
	 * 支付成功后调用
	 */
	public void paySuccess() {
		try {
			order.setStatus(Order.STATUS_SUCCESS);// 设置订单状态为交易成功
			orderService.updateByPrimaryKeySelective(order);

            //保存积分
            if (cardNo != null && !cardNo.trim().equals("") && !cardNo.trim().equals(CARD_NO_DEF)) {
                for (SaleItemEntity entity : order.getOrderItems()) {
                    //根据卡号进行积分
                    PolicyRuleService policyRuleService = (PolicyRuleService) EPSServer.appctx.getBean("pointRuleService", PolicyRuleService.class);
                    OilSale oilSale = new OilSale();
                    oilSale.setCardId(cardNo);
                    oilSale.setOrgId("1111111111");
                    oilSale.setAmount(entity.getAmount().doubleValue());
                    oilSale.setOilPrices(entity.getUnitPrice().doubleValue());
                    oilSale.setRefueling(entity.getQuantity().doubleValue());
                    oilSale.setOilId(entity.getProductCode());
                    oilSale.setFillingTime(new Date());
                    policyRuleService.preCalcPoint(oilSale);
                }
            }
		} catch (Exception e) {
			logger.error("", e);
		}
	}
	
	/**
	 * 支付失败后调用
	 */
	public void payFail() {
		try {
			int status = order.getStatus();
			if (status == Order.STATUS_WAIT) {// 只有待支付状态的订单才能改变状态
				order.setStatus(Order.STATUS_ERROR);// 设置订单状态为交易失败
				orderService.updateByPrimaryKeySelective(order);
			}
		} catch (Exception e) {
			logger.error("", e);
		}
	}
}

/**
 * 优惠券对象
 */
@Data
class Coupon {
    String id;
    String consume_type;//消费类型，1油品，2非油品
    String type;//1满减，2折扣
    BigDecimal account;//减额或者折扣
    BigDecimal total;//满额
    BigDecimal couponAmount;//该优惠券对于本订单可减免的金额（根据订单计算得出）
}

class TransPosOrderHandler extends SimpleChannelInboundHandler<TPDU>{
	private static final Logger logger = LogManager.getLogger(TransPosOrderHandler.class);
	Order order;
	
	public TransPosOrderHandler(Order order) {
		this.order = order;
	}
	@Override
	protected void messageReceived(ChannelHandlerContext ctx, TPDU msg) throws Exception {
//		byte[] cmd = msg.getBody().getHeader().getCmd();
		System.out.println(msg);
	}
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        logger.error("", cause);
        super.exceptionCaught(ctx, cause);
    }

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		/*
		char padding = 0x20;
		String strAppid = Utils.systemConfiguration.getProperty("weixin.appid");
		String strMchid = Utils.systemConfiguration.getProperty("weixin.mchid");
		String strNonce = Utils.systemConfiguration.getProperty("weixin.nonce");

		String strTime = Long.toString(order.getOrderTime());
		String strProductId = order.getOrderId();

        SortedMap<Object,Object> parameters = new TreeMap<Object,Object>();
        parameters.put("appid", strAppid);
        parameters.put("mch_id", strMchid);
        parameters.put("time_stamp", strTime);
        parameters.put("nonce_str", strNonce);
        parameters.put("product_id", strProductId);
        String strSign = WeiXinUtil.createSign("UTF-8", parameters);

		StringBuilder sb = new StringBuilder();
		sb.append(Utils.rightPad(strAppid, 32, padding)).append(Utils.rightPad(strMchid, 32, padding))
			.append(Utils.rightPad(strTime, 10, padding)).append(Utils.rightPad(strNonce, 32, padding))
			.append(Utils.rightPad(strProductId, 32, padding)).append(Utils.rightPad(strSign, 32, padding));
		
		byte[] content = sb.toString().getBytes("utf-8");
		
		byte[] stationId = Converts.str2Bcd(order.getMerchantId());
		int casherId = Integer.parseInt(order.getGenerator().split("\\|")[1]);
		byte[] casherNo = Converts.int2U16(casherId);
		byte[] magic = new byte[]{0x30,0x30,0x30,0x30};
		byte[] cmd = new byte[]{'7','0'};
		byte[] tag = new byte[]{'0','0'};
		
		byte[] tmp = Utils.concatTwoByteArray(stationId,casherNo);
		byte[] tmp1 = Utils.concatTwoByteArray(tmp, magic);
		byte[] tmp2 = Utils.concatTwoByteArray(tmp1, cmd);
		byte[] tmp3 = Utils.concatTwoByteArray(tmp2, tag);
		byte[] result = Utils.concatTwoByteArray(tmp3, content);
		
		logger.debug("Send order to Trans POS.");
        logger.debug(Arrays.toString(result));
		ctx.writeAndFlush(result);
		*/
		String payUrl = Utils.systemConfiguration.getProperty("weixin.pay.url");
		String orderId = order.getOrderId();
		String overall = payUrl + orderId;
		ByteBuf length = Unpooled.buffer(4);
		byte[] content = overall.getBytes("utf-8");
		length.writeInt(content.length);
		byte[] contentLength = length.array();
		
		byte[] stationId = Converts.str2Bcd(order.getMerchantId());
		int casherId = Integer.parseInt(order.getGenerator().split("\\|")[1]);
		byte[] casherNo = Converts.int2U16(casherId);
		byte[] magic = new byte[]{0x30,0x30,0x30,0x30};
		byte[] cmd = new byte[]{'7','0'};
		byte[] tag = new byte[]{'0','0'};
		
		byte[] tmp = Utils.concatTwoByteArray(stationId,casherNo);
		byte[] tmp1 = Utils.concatTwoByteArray(tmp, magic);
		byte[] tmp2 = Utils.concatTwoByteArray(tmp1, cmd);
		byte[] tmp3 = Utils.concatTwoByteArray(tmp2, tag);
		byte[] tmp4 = Utils.concatTwoByteArray(tmp3, contentLength);
		byte[] result = Utils.concatTwoByteArray(tmp4, content);
		//保存订单信息到Channel属性中，以便在连接异常时回滚此笔订单。
		ctx.channel().attr(AttributeKey.valueOf("orderId")).set(orderId);
		logger.debug("Send order to Trans POS.");
        logger.debug(Arrays.toString(result));
		ctx.writeAndFlush(result);
	}
	
}

class SendReceiptHandler extends SimpleChannelInboundHandler<TPDU>{
	private static final Logger logger = LogManager.getLogger(TransPosOrderHandler.class);
	Order order;
	
	public SendReceiptHandler(Order order) {
		this.order = order;
	}
	@Override
	protected void messageReceived(ChannelHandlerContext ctx, TPDU msg) throws Exception {
		// TODO Auto-generated method stub
		System.out.println(msg);
	}

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        logger.error("", cause);
        super.exceptionCaught(ctx, cause);
    }
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		byte[] stationId = Converts.str2Bcd(order.getMerchantId());
		int casherId = Integer.parseInt(order.getGenerator().split("\\|")[1]);
		byte[] casherNo = Converts.int2U16(casherId);
		byte[] magic = new byte[]{0x30,0x30,0x30,0x30};
		byte[] cmd = new byte[]{'7','1'};
		byte[] tag = new byte[]{'0','0'};
		
		byte[] tmp = Utils.concatTwoByteArray(stationId,casherNo);
		byte[] tmp1 = Utils.concatTwoByteArray(tmp, magic);
		byte[] tmp2 = Utils.concatTwoByteArray(tmp1, cmd);
		byte[] tmp3 = Utils.concatTwoByteArray(tmp2, tag);
		
		ByteBuf b = Unpooled.buffer(69,201);
		//交易流水号
		String orderId = order.getOrderId().substring(order.getOrderId().length()-8);
		b.writeBytes(Converts.str2Bcd(orderId));
		//交易时间
		b.writeBytes(Converts.long2U32(order.getOrderTime()));
		//应收金额
		b.writeBytes(Converts.long2U32(order.getOriginalAmount().multiply(new BigDecimal(100)).longValue()));
		//实付金额
        if (order.getPaymentAmount() != null) {
            b.writeBytes(Converts.long2U32(order.getPaymentAmount().multiply(new BigDecimal(100)).longValue()));
        } else {
            b.writeBytes(Converts.long2U32(0));
        }
		//优惠券优惠金额
        if (order.getCouponAmount() != null) {
            b.writeBytes(Converts.long2U32(order.getCouponAmount().multiply(new BigDecimal(100)).longValue()));
        } else {
            b.writeBytes(Converts.long2U32(0));
        }
		//此次消费获得积分
        if (order.getLoyaltyPoint() != null) {
            b.writeBytes(Converts.long2U32(order.getLoyaltyPoint().multiply(new BigDecimal(100)).longValue()));
        } else {
            b.writeBytes(Converts.long2U32(0));
        }
		//消费信息总条数
		b.writeByte(order.getOrderItems().size());
		//循环填充交易明细
		for(SaleItemEntity sie:order.getOrderItems()){
			b.writeBytes(Utils.convertGB(sie.getItemName(),32).getBytes(Charset.forName("GB2312")));
			b.writeInt(sie.getQuantity().multiply(new BigDecimal(100)).intValue());
			b.writeInt(sie.getUnitPrice().multiply(new BigDecimal(100)).intValue());
			b.writeInt(sie.getAmount().multiply(new BigDecimal(100)).intValue());
		}
		byte[] result = Utils.concatTwoByteArray(tmp3, b.array());
		logger.debug("Send receipt to Trans POS.");
        logger.debug(Arrays.toString(result));
		ctx.writeAndFlush(result);
	}
	
}
