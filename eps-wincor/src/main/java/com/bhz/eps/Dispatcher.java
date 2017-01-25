package com.bhz.eps;

import io.netty.channel.Channel;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.bhz.eps.entity.CardServiceRequest;
import com.bhz.eps.msg.BizMessageType;
import com.bhz.eps.processor.BizProcessor;
import com.bhz.eps.util.ClassUtil;

/**
 * 抽象了分发器
 * 多线程执行
 * 某个消息对象msgObject指定某个业务逻辑对象processor
 * submit到线程池中
 * @author yaoh
 *
 */

public class Dispatcher {
	
	private static final int MAX_THREAD_NUM = 100;
	
	private static ExecutorService executorService = Executors.newFixedThreadPool(MAX_THREAD_NUM);
	
	public static void submit(Channel channel, Object msgObject) throws InstantiationException, IllegalAccessException {
		
		if(msgObject instanceof CardServiceRequest){
//			CardServiceRequest csr = (CardServiceRequest)msgObject;
			Class<?> processorClass = ClassUtil.getProcessorClassByType(BizMessageType.CARDSVR_REQUEST);
			BizProcessor processor = (BizProcessor) processorClass.newInstance();
			processor.setChannel(channel);
			processor.setMsgObject(msgObject);
            Future f = executorService.submit(processor);
            try {
                f.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
		
	}
}