package com.bhz.eps.pay;

/**
 * Created by summer on 2017/03/16.
 */
public interface MemberPayCallbackInterface {
    void success();
    void fail(String msg, Throwable e);
}
