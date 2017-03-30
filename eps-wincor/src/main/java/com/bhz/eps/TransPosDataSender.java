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
        	if(tagBytes[0]=='0' && tagBytes[1]=='2'){

        		return;
        	}
        	if(tagBytes[0] =='0' && tagBytes[1] == '1'){
        		TransPosDataSender tpd = TransPosDataSender.getInstance(Utils.systemConfiguration.getProperty("trans.pos.ip"),
                        Integer.parseInt(Utils.systemConfiguration.getProperty("trans.pos.port")));
        		tpd.selectPayMethodToPos(Utils.PAY_METHOD_LIST, order);
        		ctx.channel().closeFuture().sync();
        		return;
        	}
        	//解析POS返回数据
        	byte[] bytes = msg.getBody().getData().getContent();
        	
        	ByteArrayInputStream reader = new ByteArrayInputStream(bytes);
        	int type = reader.read();//支付类型
        	order.setPayType(type);
        	byte[] bytes_code = new byte[20];
        	reader.read(bytes_code);
        	String code = new String(bytes_code).trim();//条码

            discount(type, code);//计算优惠
            
            if (Utils.systemConfiguration.getProperty("eps.server.stand.alone").equalsIgnoreCase("false")){
            	//组织网络支付请求发送到DataCenter
        		DataCenterOrderSender dcos = DataCenterOrderSender.getInstance(Utils.systemConfiguration.getProperty("data.center.ip"),
                        Integer.parseInt(Utils.systemConfiguration.getProperty("data.center.port")));
                dcos.askDataCenterToPay(order, type, code);
                ctx.channel().close();
            }
            else {
            	//判断支付方式
	            if (type == 1){
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
	            }
	            else if(type == 2){
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
	                String storeId = order.getMerchantId();

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
	            }
	            else if(type==3){//如果是储值会员
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
                    //通过openid获取卡号，再根据卡号进行优惠查询和积分
                    HttpClient client = HttpClients.createDefault();
                    HttpPost post = new HttpPost(Utils.systemConfiguration.getProperty("card.get.url"));
                    List<NameValuePair> param = new ArrayList<NameValuePair>();
                    param.add(new BasicNameValuePair("open_id", openId));
                    param.add(new BasicNameValuePair("type", "2"));
                    post.setEntity(new UrlEncodedFormEntity(param));
                    HttpResponse resp = client.execute(post);
                    String resultWS = EntityUtils.toString(resp.getEntity());
                    Gson gson = new Gson();
                    Map json = gson.fromJson(resultWS, Map.class);
                    if ((Boolean) json.get("success")) {
                        cardNo = json.get("cardId").toString();
                        logger.info(json.get("msg"));
                    } else {
                        logger.error(json.get("msg"));
                    }
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
        order.setCardNumber(cardNo);
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
            oilSale.setOrgId(order.getMerchantId());
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
        }else{
        	BigDecimal total = new BigDecimal(0);
            int i = 0;
            for (SaleItemEntity entity : order.getOrderItems()) {
                PreferentialPriceDetailsResponse ppdr = ppr.getDetails().get(i);
                total = total.add(ppdr.getAmountWithDiscount());
                entity.setCouponAmount(ppdr.getAmountWithDiscount());
                i++;
            }
            
            order.setPaymentAmount(total);
        }
        order.setCouponAmount(new BigDecimal(0));
        
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
                HttpPost post = new HttpPost(Utils.systemConfiguration.getProperty("coupon.list.url"));
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
                    //将返回的优惠券数据封装为优惠券对象
                    for (Map map : list) {
                        Coupon c = new Coupon();
                        c.setId(map.get("ID").toString());
                        if (map.get("CONSUME_TYPE") != null && !map.get("CONSUME_TYPE").toString().trim().equals("")) {
                            c.setConsumeType(new BigDecimal(map.get("CONSUME_TYPE").toString()).intValue());
                        }
                        c.setType(new BigDecimal(map.get("TYPE").toString()).intValue());
                        c.setAccount(new BigDecimal(map.get("ACCOUNT").toString()));
                        if (map.get("TOTAL") != null) {
                            c.setTotal(new BigDecimal(map.get("TOTAL").toString()));
                        } else {
                            c.setTotal(new BigDecimal(0));
                        }
                        couponList.add(c);
                    }
                    //选择优惠券
                    Coupon coupon = selectCoupon(couponList);
                    //使用优惠券
                    if (coupon != null && coupon.getCouponAmount() != null) {
                        //改变订单中金额
                        order.setPaymentAmount(order.getPaymentAmount().subtract(coupon.getCouponAmount()));
                        order.setCouponAmount(order.getCouponAmount().add(coupon.getCouponAmount()));
                        order.setCouponId(coupon.getId());
                        logger.info(json.get("msg"));
                    }
                } else {
                    logger.error("使用优惠券异常：" + json.get("msg"));
                }
            } catch (IOException e) {
                logger.error("使用优惠券异常", e);
            }
        }
	}

    /**
     * 选择对本次订单优惠幅度最大的优惠券
     *
     * @param list
     * @return
     */
    private Coupon selectCoupon(List<Coupon> list) {
        for (Coupon coupon : list) {
            Integer consumeType = coupon.getConsumeType();
            //计算适用于该优惠券的总金额
            BigDecimal total = new BigDecimal(0);
            for (SaleItemEntity entity : order.getOrderItems()) {
                if (consumeType == null) {
                    //不限
                    total = total.add(entity.getCouponAmount());
                } else if (consumeType.intValue() == 1) {
                    //油品
                    if ("1".equals(entity.getItemCatalog())) {
                        total = total.add(entity.getCouponAmount());
                    }
                } else if (consumeType.intValue() == 2) {
                    //非油品
                    if (!"1".equals(entity.getItemCatalog())) {
                        total = total.add(entity.getCouponAmount());
                    }
                }
            }
            //判断优惠类型，计算优惠金额
            if (coupon.getType() == 1) {
                //满减
                BigDecimal threshold = coupon.getTotal();
                if (order.getPaymentAmount().compareTo(threshold) >= 0) {
                    coupon.setCouponAmount(coupon.getAccount());
                }
            } else if (coupon.getType() == 2) {
                //折扣
                coupon.setCouponAmount(total.multiply(coupon.getAccount()).setScale(2, BigDecimal.ROUND_HALF_UP));
            }
        }
        Coupon result = null;
        //选择对本次订单优惠幅度最大的优惠券
        for (Coupon coupon : list) {
            if (result == null) {
                if (coupon.getCouponAmount() != null && coupon.getCouponAmount().compareTo(BigDecimal.ZERO) > 0) {
                    result = coupon;
                }
                continue;
            }
            if (coupon.getCouponAmount() != null
                    && result.getCouponAmount() != null &&
                    coupon.getCouponAmount().compareTo(result.getCouponAmount()) > 0) {
                result = coupon;
            }
        }
        return result;
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
                    oilSale.setOrgId(order.getMerchantId());
                    oilSale.setAmount(entity.getAmount().doubleValue());
                    oilSale.setOilPrices(entity.getUnitPrice().doubleValue());
                    oilSale.setRefueling(entity.getQuantity().doubleValue());
                    oilSale.setOilId(entity.getProductCode());
                    oilSale.setFillingTime(new Date());
                    policyRuleService.handleFlow(oilSale);
                }
            }

            //使用优惠券
            if (order.getCouponId() != null && !order.getCouponId().trim().equals("")) {
                HttpClient client = HttpClients.createDefault();
                List<NameValuePair> param = new ArrayList<NameValuePair>();
                HttpPost post = new HttpPost(Utils.systemConfiguration.getProperty("coupon.use.url"));
                param = new ArrayList<NameValuePair>();
                param.add(new BasicNameValuePair("cardId", cardNo));
                param.add(new BasicNameValuePair("couponId", order.getCouponId()));
                post.setEntity(new UrlEncodedFormEntity(param));
                HttpResponse resp = client.execute(post);
                String result = EntityUtils.toString(resp.getEntity());
                Gson gson = new Gson();
                Map json = gson.fromJson(result, Map.class);
                if ((Boolean) json.get("success")) {
                    //使用成功
                    logger.info(json.get("msg"));
                } else {
                    logger.error("使用优惠券异常：" + json.get("msg"));
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
    Integer consumeType;//消费类型，空为不限，1油品，2非油品
    int type;//优惠类型，1满减，2折扣
    BigDecimal account;//减额或者折扣
    BigDecimal total;//满额
    BigDecimal couponAmount = new BigDecimal(0);//该优惠券对于本订单可减免的金额（根据订单计算得出）
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
		
		ByteBuf b = Unpooled.buffer();
		//交易流水号
		String orderId = order.getOrderId().substring(order.getOrderId().length()-8);
		b.writeBytes(Converts.str2Bcd(orderId));
		//交易时间
		b.writeBytes(Converts.long2U32(order.getOrderTime()));
		//交易类型
		b.writeBytes(Utils.convertGB(Utils.PAY_METHOD_MAP.get(order.getPayType()),20).getBytes(Charset.forName("GB2312")));
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
