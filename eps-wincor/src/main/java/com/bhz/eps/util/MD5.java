package com.bhz.eps.util;
import java.security.*;
/*
 * 
MD5 ("") = d41d8cd98f00b204e9800998ecf8427e
MD5 ("a") = 0cc175b9c0f1b6a831c399e269772661
MD5 ("abc") = 900150983cd24fb0d6963f7d28e17f72
MD5 ("message digest") = f96b697d7cb7938d525a2f31aaf161d0
MD5 ("abcdefghijklmnopqrstuvwxyz") = c3fcd3d76192e4007dfb496cca67e13b
 */
public class MD5 {
	public MD5() {

	}
	public static byte[] getMD5Bytes(byte[] b){
		//byte[] input = b;
		byte[] tmp =new byte[16];
		try {
			// 获得一个MD5摘要算法的对象,还可以是SHA等
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(b);			
			tmp = md.digest();// 获得MD5的摘要结果
		}catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return tmp;
	}
	public static byte[] getMD5Bytes(String s){
		byte[] input = s.getBytes();
		byte[] tmp =new byte[16];
		try {
			// 获得一个MD5摘要算法的对象,还可以是SHA等
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(input);			
			tmp = md.digest();// 获得MD5的摘要结果
		}catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return tmp;
	}
	
	public static String md5Bytes2String(byte[] tmp){
		String output = null;
		// 声明一个16进制字母
		char[] hexChar = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
				'a', 'b', 'c', 'd', 'e', 'f' };				
			char[] str = new char[32];
			byte b = 0;
			for (int i = 0; i < 16; i++) {
				b = tmp[i];
				str[2 * i] = hexChar[b >>> 4 & 0xf];// 取每一个字节的低四位换成16进制字母
				str[2 * i + 1] = hexChar[b & 0xf];// 取每一个字节的高四位换成16进制字母
			}
			output = new String(str);
		
		return output;
	}
	public static String getMD5(String s) {

		byte[] input = s.getBytes();
		String output = null;
		// 声明一个16进制字母
		char[] hexChar = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
				'a', 'b', 'c', 'd', 'e', 'f' };
		try {
			// 获得一个MD5摘要算法的对象,还可以是SHA等
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(input);
			/*
			 * MD5算法的结果是128位一个整数,在这里javaAPI已经把结果转换成字节数组了
			 */
			byte[] tmp = md.digest();// 获得MD5的摘要结果
			char[] str = new char[32];
			byte b = 0;
			for (int i = 0; i < 16; i++) {
				b = tmp[i];
				str[2 * i] = hexChar[b >>> 4 & 0xf];// 取每一个字节的低四位换成16进制字母
				str[2 * i + 1] = hexChar[b & 0xf];// 取每一个字节的高四位换成16进制字母
			}
			output = new String(str);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return output;
	}

    private static String byteArrayToHexString(byte b[]) {
        StringBuffer resultSb = new StringBuffer();
        for (int i = 0; i < b.length; i++)
            resultSb.append(byteToHexString(b[i]));

        return resultSb.toString();
    }

    private static String byteToHexString(byte b) {
        int n = b;
        if (n < 0)
            n += 256;
        int d1 = n / 16;
        int d2 = n % 16;
        return hexDigits[d1] + hexDigits[d2];
    }

    public static String MD5Encode(String origin, String charsetname) {
        String resultString = null;
        try {
            resultString = new String(origin);
            MessageDigest md = MessageDigest.getInstance("MD5");
            if (charsetname == null || "".equals(charsetname))
                resultString = byteArrayToHexString(md.digest(resultString
                        .getBytes()));
            else
                resultString = byteArrayToHexString(md.digest(resultString
                        .getBytes(charsetname)));
        } catch (Exception exception) {
        }
        return resultString;
    }

    private static final String hexDigits[] = { "0", "1", "2", "3", "4", "5",
            "6", "7", "8", "9", "a", "b", "c", "d", "e", "f" };
}
