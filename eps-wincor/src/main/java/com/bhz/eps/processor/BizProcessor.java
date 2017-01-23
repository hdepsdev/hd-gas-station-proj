package com.bhz.eps.processor;

import lombok.Getter;
import lombok.Setter;
import io.netty.channel.Channel;

/**
 * 执行业务逻辑的基类
 * @author yaoh
 *
 */
public abstract class BizProcessor implements Runnable {
	
	@Getter @Setter
	protected Channel channel;
	@Getter @Setter
	protected Object msgObject;
	
	@Override
	public void run() {
		process();
	}
	
	public abstract void process();
}
