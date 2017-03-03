package com.bhz.eps.entity;

import com.tencent.common.Configure;
import com.tencent.common.RandomStringGenerator;
import com.tencent.common.Signature;

import lombok.Data;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * 通过授权码查询OPENID时传送的数据
 */
@Data
public class AuthcodeToOpenidReqData {

	// 每个字段具体的意思请查看API文档
	private String appid = "";
	private String mch_id = "";
	private String auth_code = "";
	private String nonce_str = "";
	private String sign = "";

	/**
	 * @param authCode 这个是扫码终端设备从用户手机上扫取到的支付授权号，这个号是跟用户用来支付的银行卡绑定的，有效期是1分钟
	 */
	public AuthcodeToOpenidReqData(String authCode) {

		// 微信分配的公众号ID（开通公众号之后可以获取到）
		setAppid(Configure.getAppid());

		// 微信支付分配的商户号ID（开通公众号的微信支付功能之后可以获取到）
		setMch_id(Configure.getMchid());

		// 这个是扫码终端设备从用户手机上扫取到的支付授权号，这个号是跟用户用来支付的银行卡绑定的，有效期是1分钟
		// 调试的时候可以在微信上打开“钱包”里面的“刷卡”，将扫码页面里的那一串14位的数字输入到这里来，进行提交验证
		setAuth_code(authCode);

		// 随机字符串，不长于32 位
		setNonce_str(RandomStringGenerator.getRandomStringByLength(32));

		// 根据API给的签名规则进行签名
		String sign = Signature.getSign(toMap());
		setSign(sign);// 把签名数据设置到Sign这个属性中

	}

	public Map<String, Object> toMap() {
		Map<String, Object> map = new HashMap<String, Object>();
		Field[] fields = this.getClass().getDeclaredFields();
		for (Field field : fields) {
			Object obj;
			try {
				obj = field.get(this);
				if (obj != null) {
					map.put(field.getName(), obj);
				}
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		return map;
	}

}
