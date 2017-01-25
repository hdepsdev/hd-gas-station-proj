package com.bhz.eps.pdu.transpos;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

public class BizPDUHeader implements Serializable {

    private static final long serialVersionUID = 4L;
    
    @Getter @Setter
    private String stationID;
    @Getter @Setter
    private Integer cashier;
    @Getter @Setter
    private Long magicNo;
    @Getter @Setter
    private byte[] cmd;
    @Getter @Setter
    private byte[] tag;
    @Getter @Setter
    private byte[] originalContent;
    
    public BizPDUHeader(){}

    

    @Override
    public String toString(){
        return ":stationID:" + stationID+",cashier:"+cashier+",cmd:"+new String(cmd) + ",tag" + new String(tag);
    }

}
