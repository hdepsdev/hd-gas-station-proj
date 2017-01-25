package com.bhz.eps.pdu.transpos;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

public class TPDUBody implements Serializable {
    
    private static final long serialVersionUID = 4L;
    
    @Getter @Setter
    private BizPDUChecker checker;
    @Getter @Setter
    private BizPDUData data;
    @Getter @Setter
    private BizPDUHeader header;
    
    public TPDUBody(){
        
    }
    
    
    @Override
    public String toString(){
        return "\n\theader --> " + header + "\n\tdata -->"+data;
    }
}
