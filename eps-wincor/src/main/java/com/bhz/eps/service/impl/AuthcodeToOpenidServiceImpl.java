package com.bhz.eps.service.impl;

import com.bhz.eps.entity.AuthcodeToOpenidReqData;
import com.bhz.eps.entity.AuthcodeToOpenidResData;
import com.tencent.common.Util;
import com.tencent.service.BaseService;

/**
 * 通过授权码查询OPENID
 */
public class AuthcodeToOpenidServiceImpl extends BaseService {

	public AuthcodeToOpenidServiceImpl() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
		super("https://api.mch.weixin.qq.com/tools/authcodetoopenid");
	}

	/**
	 * 通过授权码查询OPENID
	 * 
	 * @param reqData 其中包含授权码
	 * @return 其中包含openid
	 * @throws Exception
	 */
	public AuthcodeToOpenidResData request(AuthcodeToOpenidReqData reqData) throws Exception {
		String xml = sendPost(reqData);
		AuthcodeToOpenidResData resData = (AuthcodeToOpenidResData) Util.getObjectFromXML(xml,
				AuthcodeToOpenidResData.class);
		return resData;
	}

}
