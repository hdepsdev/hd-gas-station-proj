package com.bhz.eps;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.bhz.eps.msg.ManageMessageProto;
import com.bhz.eps.processor.BizProcessor;
import com.bhz.eps.util.ClassUtil;

import io.netty.channel.Channel;

/**
 * 抽象了分发器
 * 多线程执行
 * 某个消息对象msgObject指定某个业务逻辑对象processor
 * submit到线程池中
 * @author yaoh
 *
 */
public class ProtobufMsgDispatcher {
	private static final int MAX_THREAD_NUM = 10;
	
	private static ExecutorService executorService = Executors.newFixedThreadPool(MAX_THREAD_NUM);
	public static void submit(Channel channel, Object msgObject) throws InstantiationException, IllegalAccessException {
		ManageMessageProto.ManageMessage msg = (ManageMessageProto.ManageMessage)msgObject;
		
		Class<?> processorClass = ClassUtil.getProcessorClassByType(msg.getType().getNumber());
		BizProcessor processor = (BizProcessor) processorClass.newInstance();
		processor.setChannel(channel);
		processor.setMsgObject(msgObject);
		
		executorService.submit(processor);
	}
}
