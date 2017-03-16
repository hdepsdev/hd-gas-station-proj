package com.bhz.eps.processor;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.alipay.demo.trade.config.Configs;
import com.alipay.demo.trade.model.ExtendParams;
import com.alipay.demo.trade.model.GoodsDetail;
import com.alipay.demo.trade.model.builder.AlipayTradePayRequestBuilder;
import com.alipay.demo.trade.model.result.AlipayF2FPayResult;
import com.alipay.demo.trade.service.AlipayTradeService;
import com.alipay.demo.trade.service.impl.AlipayTradeServiceImpl;
import com.bhz.eps.entity.Order;
import com.bhz.eps.entity.SaleItemEntity;
import com.bhz.eps.msg.PaymentReqProto;
import com.bhz.eps.msg.PaymentRespProto;
import com.bhz.eps.service.OrderService;
import com.bhz.eps.service.impl.OrderServiceImpl;
import com.tencent.WXPay;
import com.tencent.business.ScanPayBusiness.ResultListener;
import com.tencent.protocol.pay_protocol.ScanPayReqData;
import com.tencent.protocol.pay_protocol.ScanPayResData;

public class PaymentOrderProcessor extends BizProcessor{
	private static final Logger logger = LogManager.getLogger(PaymentOrderProcessor.class);
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
	@Override
	public void process() {
		try{
			//处理订单信息
			PaymentReqProto.PaymentReq reqmsg = (PaymentReqProto.PaymentReq)msgObject;
			//根据付款方法完成支付
			int type = reqmsg.getMethodOfPayment();
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
	            authCode = reqmsg.getAuthCode();
	            StringBuilder temp = new StringBuilder(reqmsg.getSeller().getProviderId());
	            temp.append("-");
	            for (PaymentReqProto.Goods goods : reqmsg.getGoodsDetailList()) {
	            	temp.append(goods.getGoodsName());
	            	temp.append(",");
	            }
	            body = temp.deleteCharAt(temp.length() - 1).toString();	//去除最后一个","
	            attach = reqmsg.getAttach();
	            outTradeNo = reqmsg.getWorkOrder();
	            totalFee = (int)(reqmsg.getPaymentAmount().getTotalAmount() * 100);
	            deviceInfo = reqmsg.getSeller().getNozzleNumber();
	            spBillCreateIP = "";
	            timeStart = reqmsg.getOrderTime().getTimeStart();
	            timeExpire = reqmsg.getOrderTime().getTimeExpire();
	            goodsTag = reqmsg.getGoodsTag();
	
	            //向微信支付网关发送数据
	            ScanPayReqData scanPayReqData = new ScanPayReqData(authCode, body, attach, outTradeNo, totalFee, deviceInfo, spBillCreateIP, timeStart, timeExpire, goodsTag);
	            WXPay.doScanPayBusiness(scanPayReqData, new ResultListener(){
	
					@Override
					public void onFail(ScanPayResData scanPayResData) {
						payFail(reqmsg, 99, scanPayResData.getReturn_msg());
					}
	
					@Override
					public void onFailByAuthCodeExpire(ScanPayResData scanPayResData) {
						payFail(reqmsg, -1, scanPayResData.getReturn_msg());
					}
	
					@Override
					public void onFailByAuthCodeInvalid(ScanPayResData scanPayResData) {
						payFail(reqmsg, -2, scanPayResData.getReturn_msg());
					}
	
					@Override
					public void onFailByMoneyNotEnough(ScanPayResData scanPayResData) {
						payFail(reqmsg, -3, scanPayResData.getReturn_msg());
					}
	
					@Override
					public void onFailByReturnCodeError(ScanPayResData scanPayResData) {
						payFail(reqmsg, -4, scanPayResData.getReturn_msg());
					}
	
					@Override
					public void onFailByReturnCodeFail(ScanPayResData scanPayResData) {
						payFail(reqmsg, -5, scanPayResData.getReturn_msg());
					}
	
					@Override
					public void onFailBySignInvalid(ScanPayResData scanPayResData) {
						payFail(reqmsg, -6, scanPayResData.getReturn_msg());
					}
	
					@Override
					public void onSuccess(ScanPayResData scanPayResData) {
						paySuccess(reqmsg);
					}
	            	
	            });
	        } else if (type == 2) {//如果是支付宝
	        	// (必填) 商户网站订单系统中唯一订单号，64个字符以内，只能包含字母、数字、下划线，
	            // 需保证商户系统端不能重复，建议通过数据库sequence生成，
	            String outTradeNo = reqmsg.getWorkOrder();
	
	            // (必填) 订单标题，粗略描述用户的支付目的。如“xxx品牌xxx门店消费”
	            String subject = reqmsg.getTitle();
	
	            // (必填) 订单总金额，单位为元，不能超过1亿元
	            // 如果同时传入了【打折金额】,【不可打折金额】,【订单总金额】三者,则必须满足如下条件:【订单总金额】=【打折金额】+【不可打折金额】
	            DecimalFormat format = new DecimalFormat("#.00");
	            String totalAmount = format.format(reqmsg.getPaymentAmount().getTotalAmount());
	
	            // (必填) 付款条码，用户支付宝钱包手机app点击“付款”产生的付款条码
	            String authCode = reqmsg.getAuthCode(); // 条码示例，286648048691290423
	            // (可选，根据需要决定是否使用) 订单可打折金额，可以配合商家平台配置折扣活动，如果订单部分商品参与打折，可以将部分商品总价填写至此字段，默认全部商品可打折
	            // 如果该值未传入,但传入了【订单总金额】,【不可打折金额】 则该值默认为【订单总金额】- 【不可打折金额】
	            String discountableAmount = format.format(reqmsg.getPaymentAmount().getDiscountableAmount()); //
	
	            // (可选) 订单不可打折金额，可以配合商家平台配置折扣活动，如果酒水不参与打折，则将对应金额填写至此字段
	            // 如果该值未传入,但传入了【订单总金额】,【打折金额】,则该值默认为【订单总金额】-【打折金额】
	            String undiscountableAmount = format.format(reqmsg.getPaymentAmount().getUndiscountableAmount());
	
	            // 卖家支付宝账号ID，用于支持一个签约账号下支持打款到不同的收款账号，(打款到sellerId对应的支付宝账号)
	            // 如果该字段为空，则默认为与支付宝签约的商户的PID，也就是appid对应的PID
	            String sellerId = reqmsg.getSeller().getSellerId();
	
	            // 订单描述，可以对交易或商品进行一个详细地描述，比如填写"购买商品3件共20.00元"
	            String body = "";
	
	            // 商户操作员编号，添加此参数可以为商户操作员做销售统计
	            String operatorId = reqmsg.getSeller().getNozzleNumber();
	
	            // (必填) 商户门店编号，通过门店号和商家后台可以配置精准到门店的折扣信息，详询支付宝技术支持
	            String storeId = reqmsg.getSeller().getStationId();
	
	            // 业务扩展参数，目前可添加由支付宝分配的系统商编号(通过setSysServiceProviderId方法)，详情请咨询支付宝技术支持
	            String providerId = reqmsg.getSeller().getProviderId();
	            ExtendParams extendParams = new ExtendParams();
	            extendParams.setSysServiceProviderId(providerId);
	
	            // 支付超时，线下扫码交易定义为5分钟
	            String timeoutExpress = reqmsg.getOrderTime().getTimeoutExpress();
	
	            // 商品明细列表，需填写购买商品详细信息，
	            List<GoodsDetail> goodsDetailList = new ArrayList<GoodsDetail>();
	            StringBuilder temp = new StringBuilder();
	            for (PaymentReqProto.Goods goods : reqmsg.getGoodsDetailList()) {
	            	temp.append(goods.getGoodsName());
	            	temp.append(",");
	                // 创建一个商品信息，参数含义分别为商品id（使用国标）、名称、单价（单位为分）、数量，如果需要添加商品类别，详见GoodsDetail
	                GoodsDetail goodsdetail = GoodsDetail.newInstance(goods.getGoodsId(), goods.getGoodsName(), 
	                		goods.getPrice(), goods.getQuantity());
	                // 创建好一个商品后添加至商品明细列表
	                goodsDetailList.add(goodsdetail);
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
	                	paySuccess(reqmsg);
	                    break;
	
	                case FAILED:
	                	logger.error("支付宝支付失败!!!");
	                	payFail(reqmsg, -1, "支付宝支付失败!!!");
	                    break;
	
	                case UNKNOWN:
	                	logger.error("系统异常，订单状态未知!!!");
	                	payFail(reqmsg, -2, "系统异常，订单状态未知!!!");
	                    break;
	
	                default:
	                	logger.error("不支持的交易状态，交易返回异常!!!");
	                	payFail(reqmsg, 99, "不支持的交易状态，交易返回异常!!!");
	                    break;
	            }
	        }
	    } catch(Exception e) {
	        logger.error("", e);
	    }
	}
	
	/**
	 * 支付成功后调用
	 */
	private void paySuccess(PaymentReqProto.PaymentReq req) {
		try {
			//支付成功，将订单存入数据库
			PaymentRespProto.PaymentResp.Builder respBuilder = PaymentRespProto.PaymentResp.newBuilder();
			respBuilder.setStationCode(req.getSeller().getStationId());
			respBuilder.setNozzleNumber(req.getSeller().getNozzleNumber());
			respBuilder.setPaymentState(0);
			respBuilder.setMsg("");
			//将信息返回客户端
			channel.writeAndFlush(respBuilder);
			//支付成功，返回支付成功信息
			Order order = new Order();
			//销售信息
			order.setMerchantName(req.getSeller().getProviderId());
			order.setMerchantId(req.getSeller().getStationId());
			order.setGenerator(req.getSeller().getNozzleNumber());
			order.setClerkId(req.getSeller().getSellerId());
			//订单金额
			order.setOriginalAmount(new BigDecimal(req.getPaymentAmount().getTotalAmount()));
			//订单时间
			order.setOrderId(req.getWorkOrder());
			order.setOrderTime(Integer.parseUnsignedInt(req.getOrderTime().getTimeStart().replaceAll("-", "")));
			order.setStatus(0);
			//订单详情
			Set<SaleItemEntity> items = new HashSet<SaleItemEntity>();
			for(PaymentReqProto.Goods goods : req.getGoodsDetailList())
			{
				SaleItemEntity item = new SaleItemEntity();
				item.setId(goods.getGoodsId());
				item.setItemName(goods.getGoodsName());
				item.setUnitPrice(new BigDecimal(goods.getPrice()));
				item.setQuantity(new BigDecimal(goods.getQuantity()));
				items.add(item);
			}
			order.setOrderItems(items);
			OrderService orderService = new OrderServiceImpl();
			orderService.addOrder(order);
		} catch (Exception e) {
			logger.error("", e);
		}
	}
	
	/**
	 * 支付失败后调用
	 */
	private void payFail(PaymentReqProto.PaymentReq req,int state, String msg) {
		try {
			//支付失败，将失败代码与错误信息返回
			PaymentRespProto.PaymentResp.Builder respBuilder = PaymentRespProto.PaymentResp.newBuilder();
			respBuilder.setStationCode(req.getSeller().getStationId());
			respBuilder.setNozzleNumber(req.getSeller().getNozzleNumber());
			respBuilder.setPaymentState(state);
			respBuilder.setMsg(msg);
			//将信息返回客户端
			channel.writeAndFlush(respBuilder);
		} catch (Exception e) {
			logger.error("", e);
		}
	}
}
