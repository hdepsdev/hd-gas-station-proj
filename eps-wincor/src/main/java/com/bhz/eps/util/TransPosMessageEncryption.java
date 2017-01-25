package com.bhz.eps.util;


/**
 * 加密pos交易消息
 * @author yaoh
 *
 */
public class TransPosMessageEncryption {
    private final static byte[] SALT = new byte[]{0x21, 0x32, 0x43, 0x54, 0x65, 0x76, (byte)0x87, (byte)0x98, (byte)0xA9, (byte)0xBA, (byte)0xCB, (byte)0xDC, (byte)0xED, (byte)0xFE, 0x0F, 0x10};
    private final static byte[] KEY = new byte[]{0x12, 0x23, 0x34, 0x45, 0x56, 0x67, 0x78, (byte)0x89, (byte)0x9A, (byte)0xAB, (byte)0xBC, (byte)0xCD, (byte)0xDE, (byte)0xEF, (byte)0xF0, (byte)0x01,0x12, 0x23, 0x34, 0x45, 0x56, 0x67, 0x78, (byte)0x89};
    
    //tools; concatenate two array be one array.
    private static byte[] concateArray(byte[] arr1,byte[] arr2){
        byte[] result = new byte[arr1.length + arr2.length];
        System.arraycopy(arr1,0,result,0,arr1.length);
        System.arraycopy(arr2,0,result,arr1.length,arr2.length);
        return result;
    }
    
    //Calculate transaction data MAC
    private static byte[] getMessageMac(byte[] message) throws Exception{
        //Attach salt
        byte[] contentWithSalt = concateArray(message,SALT);
        //Calculate MD5
        byte[] contentWithSaltMD5 = MD5.getMD5Bytes(contentWithSalt);
        //Calculate transaction data's Message Authentication Codes.
        byte[] md5MAC = MacCalculator.calcMAC(KEY,contentWithSaltMD5);
        
        return md5MAC;
    }
    
    //Add MAC to the tail of transaction data
    public static byte[] getEncryptionMessage(byte[] message) throws Exception{
        byte[] messageMac = getMessageMac(message);
        byte[] truncatedMac = new byte[4];
        //truncate higher 4 bytes
        System.arraycopy(messageMac,0,truncatedMac,0,4);
        
        return concateArray(message,truncatedMac);
    }
    public static byte[] getPOSMac(byte[] message) throws Exception{
        byte[] messageMac = getMessageMac(message);
        byte[] truncatedMac = new byte[4];
        //truncate higher 4 bytes
        System.arraycopy(messageMac,0,truncatedMac,0,4);
        return truncatedMac;
    }

}
