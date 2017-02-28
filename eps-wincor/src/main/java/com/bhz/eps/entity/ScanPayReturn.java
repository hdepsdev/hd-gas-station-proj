package com.bhz.eps.entity;

import com.thoughtworks.xstream.XStream;
import lombok.Data;

/**
 * Created by summer on 2017/02/28.
 */
@Data
public class ScanPayReturn {
    private String return_code;//SUCCESS/FAIL 此字段是通信标识，非交易标识，交易是否成功需要查看result_code来判断
    private String return_msg;//返回信息，如非空，为错误原因
    //当return_code为SUCCESS的时候，还会包括以下字段：
    private String appid;//公众账号ID
    private String mch_id;//商户号
    private String device_info;//设备号
    private String nonce_str;//随机字符串
    private String sign;//签名
    private String result_code;//业务结果 SUCCESS/FAIL
    private String err_code;//错误代码
    private String err_code_des;//错误代码描述
    //当return_code 和result_code都为SUCCESS的时，还会包括以下字段：
    private String openid;//用户在商户appid 下的唯一标识
    private String is_subscribe;//用户是否关注公众账号，仅在公众账号类型支付有效，取值范围：Y或N;Y-关注;N-未关注
    private String trade_type;//支付类型为MICROPAY(即扫码支付)
    private String bank_type;//银行类型，采用字符串类型的银行标识
    private String fee_type;//货币类型 符合ISO 4217标准的三位字母代码，默认人民币：CNY
    private String total_fee;//订单金额，单位为分，只能为整数
    private String settlement_total_fee;//应结订单金额=订单金额-非充值代金券金额，应结订单金额<=订单金额
    private String coupon_fee;//“代金券”金额<=订单金额，订单金额-“代金券”金额=现金支付金额
    private String cash_fee_type;//现金支付货币类型 符合ISO 4217标准的三位字母代码，默认人民币：CNY
    private String cash_fee;//订单现金支付金额
    private String transaction_id;//微信支付订单号
    private String out_trade_no;//商户系统的订单号，与请求一致
    private String attach;//商家数据包，原样返回
    private String time_end;//支付完成时间，格式为yyyyMMddHHmmss

    public static void main(String[] args) {
        String xml = "<xml>\n" +
                "   <return_code><![CDATA[SUCCESS]]></return_code>\n" +
                "   <return_msg><![CDATA[OK]]></return_msg>\n" +
                "   <appid><![CDATA[wx2421b1c4370ec43b]]></appid>\n" +
                "   <mch_id><![CDATA[10000100]]></mch_id>\n" +
                "   <device_info><![CDATA[1000]]></device_info>\n" +
                "   <nonce_str><![CDATA[GOp3TRyMXzbMlkun]]></nonce_str>\n" +
                "   <sign><![CDATA[D6C76CB785F07992CDE05494BB7DF7FD]]></sign>\n" +
                "   <result_code><![CDATA[SUCCESS]]></result_code>\n" +
                "   <openid><![CDATA[oUpF8uN95-Ptaags6E_roPHg7AG0]]></openid>\n" +
                "   <is_subscribe><![CDATA[Y]]></is_subscribe>\n" +
                "   <trade_type><![CDATA[MICROPAY]]></trade_type>\n" +
                "   <bank_type><![CDATA[CCB_DEBIT]]></bank_type>\n" +
                "   <total_fee>1</total_fee>\n" +
                "   <coupon_fee>0</coupon_fee>\n" +
                "   <fee_type><![CDATA[CNY]]></fee_type>\n" +
                "   <transaction_id><![CDATA[1008450740201411110005820873]]></transaction_id>\n" +
                "   <out_trade_no><![CDATA[1415757673]]></out_trade_no>\n" +
                "   <attach><![CDATA[订单额外描述]]></attach>\n" +
                "   <time_end><![CDATA[20141111170043]]></time_end>\n" +
                "</xml> ";
        XStream xstream = new XStream();
        xstream.autodetectAnnotations(false);//停止自动扫描
        xstream.ignoreUnknownElements();//忽略多余节点
        xstream.setMode(XStream.NO_REFERENCES);
        xstream.alias("xml", ScanPayReturn.class);//设置根节点名
        xstream.processAnnotations(ScanPayReturn.class);//加载类
        ScanPayReturn spr = (ScanPayReturn)xstream.fromXML(xml);
        System.out.print(spr.toString());
    }
}
