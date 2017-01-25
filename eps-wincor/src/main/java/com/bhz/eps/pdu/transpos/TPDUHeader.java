package com.bhz.eps.pdu.transpos;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

public class TPDUHeader implements Serializable {

    private static final long serialVersionUID = 4L;
    
    @Getter @Setter
    private byte[] syncCode;

    @Getter @Setter
    private long length;

    @Getter @Setter
    private int type;

    @Getter @Setter
    private Short pkgNum;

    @Getter @Setter
    private byte crc8;
    
    @Getter @Setter
    private byte[] originalContent;

    public TPDUHeader() {

    }

    @Override
    public String toString() {
        return "syncCode:" + new String(syncCode) + ",length:" + length + ",type:" + type + ",pkgNum:" + pkgNum + ",crc8:" + crc8 ;
    }

}
