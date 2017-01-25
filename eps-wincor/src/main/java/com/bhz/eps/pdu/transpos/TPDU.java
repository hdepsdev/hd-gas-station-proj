package com.bhz.eps.pdu.transpos;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

public class TPDU implements Serializable {

    private static final long serialVersionUID = 5L;
    
    @Getter @Setter
    private TPDUHeader header;
    @Getter @Setter
    private TPDUBody body;
    
    public TPDU(){}
    public TPDU(BizPDUHeader bizHeader,BizPDUData bizData){
        body = new TPDUBody();
        body.setData(bizData);
        body.setHeader(bizHeader);
    }
    
    public TPDU(TPDUHeader tpduHeader,BizPDUHeader bizHeader,BizPDUData bizData,BizPDUChecker bizCheck){
        body = new TPDUBody();
        body.setChecker(bizCheck);
        body.setData(bizData);
        body.setHeader(bizHeader);
        this.header = tpduHeader;
    }
    @Override
    public String toString(){
        return  ""+body;
    }
}
