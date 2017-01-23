package com.bhz.eps.util;

import java.util.Vector;

/**
 * 计算POS MAC工具类
 * @author yaoh
 *
 */

public class MacCalculator {
    /**
     * 
     * @param key 密钥
     * @param content 加密内容
     * @param offset 加密内容起始位置
     * @param length 所要加密内容的长度，如果小于0，则需要加密整个内容。
     * @return MAC码
     * @throws Exception
     */
    public static byte[] calcMAC(byte[] key, byte[] content, int offset, int length) throws Exception{
        TripleDES tdes = new TripleDES();
        byte[]  result = null;
        Vector<byte[]>  dataBlockContainer = new Vector<byte[]>();
        byte[]  tmp, dataBlock, xor, algDES;
        int dataBlockSize, position;
        
        if (key == null || content == null) return result;
        
        if (offset < 0) offset = 0;
        if (length < 0) length = content.length - offset;
        
        //Split data (8byte per block)
        int len = 0;
        position = offset;
        while (len < length && position < content.length)
        {
            dataBlock = new byte[8];
            for (int i = 0; i < dataBlock.length; i ++) {
                dataBlock[i] = (byte)0;
            }
            for (int i = 0; i < dataBlock.length && len < length && position < content.length; i ++){
                dataBlock[i] = content[position++];
                len ++;
            }
            dataBlockContainer.addElement(dataBlock);
        }
        
        // Loop calculate: XOR + DESede
        // Initialize data
        algDES = new byte[8];
        for (int i = 0; i < algDES.length; i ++) {
            algDES[i] = (byte)0;
        }
        
        dataBlockSize = dataBlockContainer.size();
        for (int n = 0; n < dataBlockSize; n ++){
            dataBlock = (byte[])dataBlockContainer.elementAt(n);
            if (dataBlock == null) continue;
            
            xor = new byte[Math.min(algDES.length,dataBlock.length)];
            for (int i = 0; i < xor.length; i ++) xor[i] = (byte)(algDES[i] ^ dataBlock[i]);//xor calculate
            //DESede cipher
            tmp = tdes.encrypt(xor,key);
            
            for (int i = 0; i < algDES.length; i ++){
                algDES[i] = (byte)0;
            }
            //Transfer
            for (int i = 0; i < Math.min(algDES.length,tmp.length); i ++) {
                algDES[i] = tmp[i];
            }
        }
        dataBlockContainer.removeAllElements();
        result = algDES;
        return result;
    }
    
    /**
     * 
     * @param key 密钥
     * @param content 加密内容
     * @return MAC码
     * @throws Exception
     */
    public static byte[] calcMAC(byte[] key, byte[] content) throws Exception{
        return calcMAC(key,content,0,-1);
    }
    
}
