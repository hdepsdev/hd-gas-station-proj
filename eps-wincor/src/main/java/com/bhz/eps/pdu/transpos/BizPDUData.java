package com.bhz.eps.pdu.transpos;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

public class BizPDUData implements Serializable{

    private static final long serialVersionUID = 2L;
    
    @Getter @Setter
    private byte[] content;
    
    public BizPDUData(){
    	
    }
    
    public BizPDUData(byte[] content){
        this.content = content;
    }
    
    @Override
    public String toString(){
        return content.toString() + "\n";
    }
}
