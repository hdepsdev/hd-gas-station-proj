package com.bhz.eps;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.alipay.demo.trade.config.Configs;
import com.alipay.demo.trade.model.ExtendParams;
import com.alipay.demo.trade.model.GoodsDetail;
import com.alipay.demo.trade.model.builder.AlipayTradePayRequestBuilder;
import com.alipay.demo.trade.model.result.AlipayF2FPayResult;
import com.alipay.demo.trade.service.AlipayTradeService;
import com.alipay.demo.trade.service.impl.AlipayTradeServiceImpl;
import com.bhz.eps.codec.TPDUDecoder;
import com.bhz.eps.codec.TPDUEncoder;
import com.bhz.eps.entity.AuthcodeToOpenidReqData;
import com.bhz.eps.entity.AuthcodeToOpenidResData;
import com.bhz.eps.entity.Order;
import com.bhz.eps.entity.PayMethod;
import com.bhz.eps.entity.SaleItemEntity;
import com.bhz.eps.pdu.transpos.TPDU;
import com.bhz.eps.service.OrderService;
import com.bhz.eps.service.impl.AuthcodeToOpenidServiceImpl;
import com.bhz.eps.util.Converts;
import com.bhz.eps.util.Utils;
import com.bhz.fcomc.service.PreferentialPriceService;
import com.bhz.posserver.entity.request.PreferentialPriceDetailsRequest;
import com.bhz.posserver.entity.request.PreferentialPriceRequest;
import com.bhz.posserver.entity.response.PreferentialPriceDetailsResponse;
import com.bhz.posserver.entity.response.PreferentialPriceResponse;
import com.tencent.WXPay;
import com.tencent.business.ScanPayBusiness.ResultListener;
import com.tencent.protocol.pay_protocol.ScanPayReqData;
import com.tencent.protocol.pay_protocol.ScanPayResData;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
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
import io.netty.util.AttributeKey;

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
            if (type == 1) {//如果是微信支付
                String authCode; //这个是扫码终端设备从用户手机上扫取到的支付授权号，这个号是跟用户用来支付的银行卡绑定的，有效期是1分钟
                String body; //要支付的商品的描述信息，用户会在支付成功页面里看到这个信息
                String attach; //支付订单里面可以填的附加数据，API会将提交的这个附加数据原样返回
                String outTradeNo; //商户系统内部的订单号,32个字符内可包含字母, 确保在商户系统唯一
                int totalFee; //订单总金额，单位为“分”，只能整数
                String deviceInfo; //商户自己定义的扫码支付终端设备号，方便追溯这笔交易发生在哪台终端设备上
                String spBillCreateIP; //订单生成的机器IP
                String timeStart; //订单生成时间， 格式为yyyyMMddHHmmss，如2009年12 月25 日9 点10 分10 秒表示为20091225091010。时区为GMT+8 beijing。该时间取自商户服务器
                String timeExpire; //订单失效时间，格式同上
                String goodsTag; //商品标记，微信平台配置的商品标记，用于优惠券或者满减使用
                
                //组织数据
                authCode = code;
                StringBuilder temp = new StringBuilder(order.getMerchantName());
                temp.append("-");
                for (SaleItemEntity entity : order.getOrderItems()) {
                	temp.append(entity.getItemName());
                	temp.append(",");
                }
                body = temp.deleteCharAt(temp.length() - 1).toString();
                attach = "";
                outTradeNo = order.getOrderId();
                totalFee = order.getPaymentAmount().multiply(new BigDecimal(100)).intValue();
                deviceInfo = "";
                spBillCreateIP = "";
                timeStart = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
                Calendar c = Calendar.getInstance();
                c.add(Calendar.MINUTE, 1);
                timeExpire = new SimpleDateFormat("yyyyMMddHHmmss").format(c.getTime());
                goodsTag = "";

                //向微信支付网关发送数据
                ScanPayReqData scanPayReqData = new ScanPayReqData(authCode, body, attach, outTradeNo, totalFee, deviceInfo, spBillCreateIP, timeStart, timeExpire, goodsTag);
                
                
                WXPay.doScanPayBusiness(scanPayReqData, new ResultListener(){
                	
					@Override
					public void onFail(ScanPayResData scanPayResData) {
						payFail();
					}

					@Override
					public void onFailByAuthCodeExpire(ScanPayResData scanPayResData) {
						payFail();
					}

					@Override
					public void onFailByAuthCodeInvalid(ScanPayResData scanPayResData) {
						payFail();
					}

					@Override
					public void onFailByMoneyNotEnough(ScanPayResData scanPayResData) {
						payFail();
					}

					@Override
					public void onFailByReturnCodeError(ScanPayResData scanPayResData) {
						payFail();
					}

					@Override
					public void onFailByReturnCodeFail(ScanPayResData scanPayResData) {
						payFail();
					}

					@Override
					public void onFailBySignInvalid(ScanPayResData scanPayResData) {
						payFail();
					}

					@Override
					public void onSuccess(ScanPayResData scanPayResData) {
						paySuccess();
					}
                	
                });
            } else if (type == 2) {//如果是支付宝
            	// (必填) 商户网站订单系统中唯一订单号，64个字符以内，只能包含字母、数字、下划线，
                // 需保证商户系统端不能重复，建议通过数据库sequence生成，
                String outTradeNo = order.getOrderId();

                // (必填) 订单标题，粗略描述用户的支付目的。如“xxx品牌xxx门店消费”
                String subject = order.getMerchantName() + "消费";

                // (必填) 订单总金额，单位为元，不能超过1亿元
                // 如果同时传入了【打折金额】,【不可打折金额】,【订单总金额】三者,则必须满足如下条件:【订单总金额】=【打折金额】+【不可打折金额】
                String totalAmount = order.getPaymentAmount().toString();

                // (必填) 付款条码，用户支付宝钱包手机app点击“付款”产生的付款条码
                String authCode = code; // 条码示例，286648048691290423
                // (可选，根据需要决定是否使用) 订单可打折金额，可以配合商家平台配置折扣活动，如果订单部分商品参与打折，可以将部分商品总价填写至此字段，默认全部商品可打折
                // 如果该值未传入,但传入了【订单总金额】,【不可打折金额】 则该值默认为【订单总金额】- 【不可打折金额】
                //        String discountableAmount = "1.00"; //

                // (可选) 订单不可打折金额，可以配合商家平台配置折扣活动，如果酒水不参与打折，则将对应金额填写至此字段
                // 如果该值未传入,但传入了【订单总金额】,【打折金额】,则该值默认为【订单总金额】-【打折金额】
                String undiscountableAmount = "0.0";

                // 卖家支付宝账号ID，用于支持一个签约账号下支持打款到不同的收款账号，(打款到sellerId对应的支付宝账号)
                // 如果该字段为空，则默认为与支付宝签约的商户的PID，也就是appid对应的PID
                String sellerId = "";

                // 订单描述，可以对交易或商品进行一个详细地描述，比如填写"购买商品3件共20.00元"
                String body = "";

                // 商户操作员编号，添加此参数可以为商户操作员做销售统计
                String operatorId = "";

                // (必填) 商户门店编号，通过门店号和商家后台可以配置精准到门店的折扣信息，详询支付宝技术支持
                String storeId = "";

                // 业务扩展参数，目前可添加由支付宝分配的系统商编号(通过setSysServiceProviderId方法)，详情请咨询支付宝技术支持
                String providerId = "2088100200300400500";
                ExtendParams extendParams = new ExtendParams();
                extendParams.setSysServiceProviderId(providerId);

                // 支付超时，线下扫码交易定义为5分钟
                String timeoutExpress = "5m";

                // 商品明细列表，需填写购买商品详细信息，
                List<GoodsDetail> goodsDetailList = new ArrayList<GoodsDetail>();
                StringBuilder temp = new StringBuilder();
                for (SaleItemEntity entity : order.getOrderItems()) {
                	temp.append(entity.getItemName());
                	temp.append(",");
                    // 创建一个商品信息，参数含义分别为商品id（使用国标）、名称、单价（单位为分）、数量，如果需要添加商品类别，详见GoodsDetail
                    GoodsDetail goods = GoodsDetail.newInstance(entity.getId(), entity.getItemName(), 
                    		entity.getUnitPrice().multiply(new BigDecimal(100)).longValue(), entity.getQuantity().intValue());
                    // 创建好一个商品后添加至商品明细列表
                    goodsDetailList.add(goods);
                }
                if (temp.length() > 0) {
                	temp = temp.deleteCharAt(temp.length() - 1);
                }
                body = temp.toString();

                // 创建条码支付请求builder，设置请求参数
                AlipayTradePayRequestBuilder builder = new AlipayTradePayRequestBuilder()
                    //            .setAppAuthToken(appAuthToken)
                    .setOutTradeNo(outTradeNo).setSubject(subject).setAuthCode(authCode)
                    .setTotalAmount(totalAmount).setStoreId(storeId)
                    .setUndiscountableAmount(undiscountableAmount).setBody(body).setOperatorId(operatorId)
                    .setExtendParams(extendParams).setSellerId(sellerId)
                    .setGoodsDetailList(goodsDetailList).setTimeoutExpress(timeoutExpress);

                // 调用tradePay方法获取当面付应答
                AlipayF2FPayResult result = tradeService.tradePay(builder);
                switch (result.getTradeStatus()) {
                    case SUCCESS:
                    	logger.info("支付宝支付成功: )");
                    	paySuccess();
                        break;

                    case FAILED:
                    	logger.error("支付宝支付失败!!!");
                    	payFail();
                        break;

                    case UNKNOWN:
                    	logger.error("系统异常，订单状态未知!!!");
                    	payFail();
                        break;

                    default:
                    	logger.error("不支持的交易状态，交易返回异常!!!");
                    	payFail();
                        break;
                }
            }else if(type==3){//如果是储值会员
            	
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
        PreferentialPriceService ps = (PreferentialPriceService) EPSServer.appctx.getBean("preferentialPriceService", PreferentialPriceService.class);
        //优惠查询请求对象
        PreferentialPriceRequest pps = new PreferentialPriceRequest();
        //消费时间
        pps.setTransactionTime(new Date().getTime());
        //卡类型
        pps.setCardType("I");
        //卡号
        pps.setCardNo("99000211111200001000");
        //版本号 固定1
        pps.setDbVersion(1);
        //优惠查询请求明细集合
        List<PreferentialPriceDetailsRequest> details = new ArrayList<PreferentialPriceDetailsRequest>();
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
        }
        pps.setDetails(details);
        PreferentialPriceResponse ppr = ps.queryPreferentialPrice(Long.valueOf(Utils.systemConfiguration.getProperty("eps.server.merchant.id")), pps);
        BigDecimal total = new BigDecimal(0);
        for (PreferentialPriceDetailsResponse ppdr : ppr.getDetails()) {
            total = total.add(ppdr.getAmountWithDiscount());
        }
        order.setPaymentAmount(total);
        order.setCouponAmount(order.getOriginalAmount().subtract(total));
        //discountType 优惠类型 0:不满足优惠条件 1:有优惠 2:无优惠
        if (ppr.getDiscountType() == 1) {
            logger.debug("客户享受刷卡优惠,折后金额:" + total);
        } else if (ppr.getDiscountType() == 0) {
            logger.debug("客户不满足优惠条件");
        } else if (ppr.getDiscountType() == 2) {
            logger.debug("客户已设置为无优惠");
        }
        
		try {
			// 获取用户信息
			if (type == 1) {// 只有微信支付可以获取用户数据
				AuthcodeToOpenidServiceImpl atoser = new AuthcodeToOpenidServiceImpl();
				AuthcodeToOpenidResData result = atoser.request(new AuthcodeToOpenidReqData(code));
				if ("SUCCESS".equals(result.getReturn_code()) && "SUCCESS".equals(result.getResult_code())) {
					String openId = result.getOpenid();
					// TODO 通过openid获取用户信息
					logger.debug("-----------------openid:" + openId + "----------------");
				} else {
					logger.error("获取openid失败！");
					payFail();
				}
			}
		} catch (Exception e) {
			payFail();
			logger.error("", e);
		}
	}
	
	/**
	 * 支付成功后调用
	 */
	private void paySuccess() {
		try {
			order.setStatus(Order.STATUS_SUCCESS);// 设置订单状态为交易成功
			orderService.updateOrder(order);
			
			// TODO 计算积分，由于目前消费时无用户信息，所以暂时无法实现
		} catch (Exception e) {
			logger.error("", e);
		}
	}
	
	/**
	 * 支付失败后调用
	 */
	private void payFail() {
		try {
			int status = order.getStatus();
			if (status == Order.STATUS_WAIT) {// 只有待支付状态的订单才能改变状态
				order.setStatus(Order.STATUS_ERROR);// 设置订单状态为交易失败
				orderService.updateOrder(order);
			}
		} catch (Exception e) {
			logger.error("", e);
		}
	}
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
		char padding = 0x20;
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
