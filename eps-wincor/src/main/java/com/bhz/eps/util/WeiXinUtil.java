package com.bhz.eps.util;

import java.util.*;

/**
 * Created by summer on 2017/02/09.
 */
public class WeiXinUtil {

    private static String Key = Utils.systemConfiguration.getProperty("weixin.key");

    /**
     * 微信支付签名算法sign
     * @param characterEncoding
     * @param parameters
     * @return
     */
    @SuppressWarnings("unchecked")
    public static String createSign(String characterEncoding,SortedMap<Object,Object> parameters){
        StringBuffer sb = new StringBuffer();
        Set es = parameters.entrySet();//所有参与传参的参数按照accsii排序（升序）
        Iterator it = es.iterator();
        while(it.hasNext()) {
            Map.Entry entry = (Map.Entry)it.next();
            String k = (String)entry.getKey();
            Object v = entry.getValue();
            if(null != v && !"".equals(v)
                    && !"sign".equals(k) && !"key".equals(k)) {
                sb.append(k + "=" + v + "&");
            }
        }
        sb.append("key=" + Key);
        String sign = MD5.MD5Encode(sb.toString(), characterEncoding).toUpperCase();
        return sign;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        //微信api提供的参数
        String appid = "wxd930ea5d5a258f4f";
        String mch_id = "10000100";
        String device_info = "1000";
        String body = "test";
        String nonce_str = "ibuaiVcKdpRxkhJA";

        SortedMap<Object,Object> parameters = new TreeMap<Object,Object>();
        parameters.put("appid", appid);
        parameters.put("mch_id", mch_id);
        parameters.put("device_info", device_info);
        parameters.put("body", body);
        parameters.put("nonce_str", nonce_str);

        String characterEncoding = "UTF-8";
        String weixinApiSign = "564B3178EE8A797ACD5272EF5FFE3DAA";//key为xxx时的sign
        System.out.println("微信的签名是：" + weixinApiSign);
        String mySign = createSign(characterEncoding,parameters);
        System.out.println("我  的签名是："+mySign);
        if (weixinApiSign.equals(mySign)) {
            System.out.println("签名正确！");
        }
    }
}
