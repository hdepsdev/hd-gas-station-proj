package com.bhz.eps.processor.manager;

import com.bhz.eps.annotation.BizProcessorSpec;
import com.bhz.eps.msg.ManageMessageProto;
import com.bhz.eps.msg.ManageMessageProto.MsgType;
import com.bhz.eps.processor.BizProcessor;

@BizProcessorSpec(msgType=ManageMessageProto.MsgType.Login_Request_VALUE)
public class LoginProcessor extends BizProcessor{

	@Override
	public void process() {
		ManageMessageProto.ManageMessage mm = (ManageMessageProto.ManageMessage)msgObject;
		System.out.println(mm.getRequest().getLoginRequest().getUsername());
		ManageMessageProto.Response.Builder respB = ManageMessageProto.Response.newBuilder();
		respB.setResult(true);
		ManageMessageProto.ManageMessage.Builder mb = ManageMessageProto.ManageMessage.newBuilder();
		mb.setType(MsgType.Login_Response);
		mb.setResponse(respB.build());
		mb.setSeqence(mm.getSeqence());
		ManageMessageProto.ManageMessage m1 = mb.build();
		channel.writeAndFlush(m1);
	}
	
}
