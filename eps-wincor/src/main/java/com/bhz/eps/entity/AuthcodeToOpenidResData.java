package com.bhz.eps.entity;

import lombok.Data;

/**
 * 通过授权码查询OPENID时返回的数据
 */
@Data
public class AuthcodeToOpenidResData {
	// 协议层
	private String return_code = "";
	private String return_msg = "";

	// 协议返回的具体数据（以下字段在return_code 为SUCCESS 的时候有返回）
	private String appid = "";
	private String mch_id = "";
	private String nonce_str = "";
	private String sign = "";
	private String result_code = "";
	private String err_code = "";

	// 业务返回的具体数据（以下字段在return_code 和result_code 都为SUCCESS 的时候有返回）
	private String openid = "";

}
