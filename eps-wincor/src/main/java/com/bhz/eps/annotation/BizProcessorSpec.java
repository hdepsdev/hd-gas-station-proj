package com.bhz.eps.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 修饰消息类和业务逻辑执行类
 * msgType指定对应的类型，从1开始计数
 * @author yaoh
 *
 */

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface BizProcessorSpec {
	int msgType();
}