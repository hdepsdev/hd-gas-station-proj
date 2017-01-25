package com.bhz.eps.pdu.transpos;

import java.io.Serializable;
import java.util.Arrays;

import lombok.Getter;
import lombok.Setter;

public class BizPDUChecker implements Serializable {

    private static final long serialVersionUID = 3L;
    
    @Getter @Setter
    private byte[] mac;
    
    public BizPDUChecker(){}

    @Override
    public String toString(){
        return "mac:" + Arrays.toString(mac);
    }

}
