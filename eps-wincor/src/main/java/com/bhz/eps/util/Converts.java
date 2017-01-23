package com.bhz.eps.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

import java.io.*;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Converts {
    static Logger logger = LogManager.getLogger(Converts.class);
    public final static char[] BToA = "0123456789abcdef".toCharArray();
//    private final static byte[] KEY = new byte[] { 0x12, 0x23, 0x34, 0x45, 0x56, 0x67, 0x78, (byte) 0x89, (byte) 0x9A,
//            (byte) 0xAB, (byte) 0xBC, (byte) 0xCD, (byte) 0xDE, (byte) 0xEF, (byte) 0xF0, (byte) 0x01, 0x12, 0x23,
//            0x34, 0x45, 0x56, 0x67, 0x78, (byte) 0x89 };

    private Converts() {
    }

    /*
     * Java功能包-2进制，16进制，BCD，ascii转换 java二进制,字节数组,字符,十六进制,BCD编码转换
     */

    /**
     * 把16进制字符串转换成字节数组
     *
     * @param hex
     * @return
     */
    public static byte[] hexStringToByte(String hex) {
        int len = (hex.length() / 2);
        byte[] result = new byte[len];
        char[] achar = hex.toCharArray();
        for (int i = 0; i < len; i++) {
            int pos = i * 2;
            result[i] = (byte) (toByte(achar[pos]) << 4 | toByte(achar[pos + 1]));
        }
        return result;
    }

    private static byte toByte(char c) {
        byte b = (byte) "0123456789ABCDEF".indexOf(c);
        return b;
    }

    /** */
    /**
     * 把字节数组转换成16进制字符串
     *
     * @param bArray
     * @return
     */
    // 用于普通的hex
    public static final String bytesToHexString(byte[] bArray) {
        StringBuffer sb = new StringBuffer(bArray.length);
        String sTemp;
        for (int i = 0; i < bArray.length; i++) {
            sTemp = Integer.toHexString(0xFF & bArray[i]);
            if (sTemp.length() < 2)
                sb.append(0);
            sb.append(sTemp.toUpperCase()).append(" ");
        }
        return sb.toString();
    }

    /**
     * 把字节数组转换为对象
     *
     * @param bytes
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static final Object bytesToObject(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        ObjectInputStream oi = new ObjectInputStream(in);
        Object o = oi.readObject();
        oi.close();
        return o;
    }

    /**
     * 把可序列化对象转换成字节数组
     *
     * @param s
     * @return
     * @throws IOException
     */
    public static final byte[] objectToBytes(Serializable s) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream ot = new ObjectOutputStream(out);
        ot.writeObject(s);
        ot.flush();
        ot.close();
        return out.toByteArray();
    }

    public static final String objectToHexString(Serializable s) throws IOException {
        return bytesToHexString(objectToBytes(s));
    }

    public static final Object hexStringToObject(String hex) throws IOException, ClassNotFoundException {
        return bytesToObject(hexStringToByte(hex));
    }

    /** */
    /**
     * @输入参数: BCD码
     * @输出结果: 10进制串
     */
    public static String bcd2Str(byte[] bytes) {
        StringBuffer temp = new StringBuffer(bytes.length * 2);

        for (int i = 0; i < bytes.length; i++) {
            temp.append((byte) ((bytes[i] & 0xf0) >>> 4));
            temp.append((byte) (bytes[i] & 0x0f));
        }
        // return temp.toString().substring(0, 1).equalsIgnoreCase("0") ? temp.toString().substring(1) :
        // temp.toString();
        return temp.toString();
    }

    /**
     * @函数功能: BCD码转ASC码
     * @输入参数: BCD串
     * @输出结果: ASC码
     */
    public static String BCD2ASC(byte[] bytes) {
        StringBuffer temp = new StringBuffer(bytes.length * 2);

        for (int i = 0; i < bytes.length; i++) {
            int h = ((bytes[i] & 0xf0) >>> 4);
            int l = (bytes[i] & 0x0f);
            temp.append(BToA[h]).append(BToA[l]);
        }
        return temp.toString();
    }

    /** */
    /**
     * MD5加密字符串，返回加密后的16进制字符串
     *
     * @param origin
     * @return
     */
    public static String MD5EncodeToHex(String origin) {
        return bytesToHexString(MD5Encode(origin));
    }

    /** */
    /**
     * MD5加密字符串，返回加密后的字节数组
     *
     * @param origin
     * @return
     */
    public static byte[] MD5Encode(String origin) {
        return MD5Encode(origin.getBytes());
    }

    /** */
    /**
     * MD5加密字节数组，返回加密后的字节数组
     *
     * @param bytes
     * @return
     */
    public static byte[] MD5Encode(byte[] bytes) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
            return md.digest(bytes);
        } catch (NoSuchAlgorithmException e) {
            logger.error(e.getMessage());
            return new byte[0];
        }
    }

    /**
     * ASC
     *
     * @param in
     * @return
     */
    // 由字母、数字、符号和汉字等组成的字符串。
    // 与String的区别在于ASC类型为定长，没有0结束符，不足位的部分有可能补0x00，也有可能补0x20，根据不同的命令中的具体定义决定。
    public static String decodeASC(ByteBuf in) {
        byte[] byteArray = Converts.getRawByteArray(in);
        return new String(byteArray,Charset.forName("GB2312"));
    }

    public static byte[] encodeASC(String asc) {
        return asc.getBytes(Charset.forName("GB2312"));
    }

    // 右补0x00，固定长度
    public static byte[] encodeASC(String asc, int fixedLength) {
        byte[] arr = asc.getBytes(Charset.forName("GB2312"));
        if (arr.length < fixedLength) {
            return addZeroInRightSide(arr,fixedLength);
        } else if (arr.length == fixedLength) {
            return arr;
        } else {
            throw new RuntimeException("asc长度不合法，超出范围：" + fixedLength);
        }
    }

    /**
     * BCD
     *
     * @param in
     * @return
     */
    public static String decodeBCD(ByteBuf in) {
        return bcd2Str(getByteArray(in));
    }

    /**
     * @函数功能: 10进制串转为BCD码
     * @输入参数: 10进制串
     * @输出结果: BCD码
     */
    public static byte[] str2Bcd(String asc) {
        int len = asc.length();
        int mod = len % 2;

        if (mod != 0) {
            asc = "0" + asc;
            len = asc.length();
        }

        byte abt[] = new byte[len];
        if (len >= 2) {
            len = len / 2;
        }

        byte bbt[] = new byte[len];
        abt = asc.getBytes();
        int j, k;

        for (int p = 0; p < asc.length() / 2; p++) {
            if ((abt[2 * p] >= '0') && (abt[2 * p] <= '9')) {
                j = abt[2 * p] - '0';
            } else if ((abt[2 * p] >= 'a') && (abt[2 * p] <= 'z')) {
                j = abt[2 * p] - 'a' + 0x0a;
            } else {
                j = abt[2 * p] - 'A' + 0x0a;
            }

            if ((abt[2 * p + 1] >= '0') && (abt[2 * p + 1] <= '9')) {
                k = abt[2 * p + 1] - '0';
            } else if ((abt[2 * p + 1] >= 'a') && (abt[2 * p + 1] <= 'z')) {
                k = abt[2 * p + 1] - 'a' + 0x0a;
            } else {
                k = abt[2 * p + 1] - 'A' + 0x0a;
            }

            int a = (j << 4) + k;
            byte b = (byte) a;
            bbt[p] = b;
        }
        return bbt;
    }

    public static byte[] encodeMoneyOrAmount(BigDecimal in) {
        return long2U32(transformMoneyFromRealToPosNeed(in));
    }

    public static BigDecimal decodeMoneyOrAmount(ByteBuf in) {
        return transformMoneyToReal(U32ToLong(in));
    }

    public static byte[] encodeTime(long time, String format) {
        return str2Bcd(transformLongToPosNeed(time,format));
    }

    public static long decodeTime(ByteBuf in, String format) {
        return transformTimeIntoLong(bcd2Str(getRawByteArray(in)),format);
    }

    // 从毫秒数变成20140529103922
    public static String transformLongToPosNeed(Long millisecond, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        Date dt = new Date(millisecond);
        return sdf.format(dt);
    }

    // 从20140529103922变成毫秒数
    public static Long transformTimeIntoLong(String time, String format) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            Date date = sdf.parse(time);
            return date.getTime();
        } catch (ParseException e) {
            throw new RuntimeException("由日期格式数据变成毫秒数出错");
        }
    }

    /**
     * HEX,16进制值存储 用于密码,卡授权密文 345678 --> {54, 56, 53, 53, 54, 68, 70, 56, 54, 67, 52, 51, 68, 70, 68, 51}
     *
     * @param in
     * @return
     */
    public static String decodePwd(ByteBuf in) {
        return new String(getRawByteArray(in));
    }

    /**
     * 终端编号，随机数
     *
     * @param in
     * @return
     */
    public static String decodeHex(ByteBuf in) {
        return bytesToHexString(getRawByteArray(in));
    }

    public static byte[] encodeHex(String in) {
        return hexStringToByte(in);
    }

    /**
     * U16<-->int 相互转换
     *
     * @param in
     * @return
     */
    public static int decodeU16(ByteBuf in) {
        return in.readUnsignedShort();
    }

    public static byte[] int2U16(int intValue) {
        byte[] result = new byte[2];
        result[0] = (byte) (intValue >> 8 & 0x000000FF);
        result[1] = (byte) (intValue & 0x000000FF);
        return result;
    }

    // private static byte[] int2Byte(int u) {
    // byte[] b = new byte[4];
    // b[3] = (byte) (u);
    // b[2] = (byte) (u >> 8);
    // b[1] = (byte) (u >> 16);
    // b[0] = (byte) (u >> 24);
    // return b;
    // }

    /**
     * String，只在班次名称和错误描述里面使用
     */

    public static String decodeStr(String msg) {
        // 去除最后的0x00
        return msg.substring(0,msg.length() - 1);
    }

    public static String decodeStr(ByteBuf in) {
        // 去除最后的0x00
        String result = decodeASC(in.readBytes(in.readableBytes() - 1));
        in.skipBytes(1);
        return result;
    }

    public static byte[] encodeStr(String errorDescription) {
        if (errorDescription.isEmpty()) {
            // 后面补上0x00
            return new byte[] { 0x00 };
        } else {
            byte[] arr = new byte[0];
            try {
                arr = errorDescription.getBytes("GB2312");
            } catch (UnsupportedEncodingException e) {
                logger.error(e.getMessage());
            }
            return Arrays.copyOf(arr,arr.length + 1);
        }

    }

    public static String addZeroInLeft2Str(String src, int fixedLength) {
        // String不足位左补0
        int len = src.length();
        if (len == 0 || fixedLength < 1 || len > fixedLength) {
            throw new RuntimeException("illegal String: " + src + " , fixLength: " + fixedLength);
        }
        StringBuilder result = new StringBuilder("");
        if (len < fixedLength) {
            for (int i = 0; i < fixedLength - len; i++) {
                result.append("0");
            }
            result.append(src);
            return result.toString();
        } else if (len == fixedLength) {
            return src;
        } else {
            throw new RuntimeException("String Length exceed " + fixedLength);
        }
    }

    /**
     * U32<--> Long
     *
     * @param in
     * @return
     */
    public static long U32ToLong(ByteBuf in) {
        return in.readUnsignedInt();
    }

    public static byte[] long2U32(long longValue) {
        // 将long变成u32格式,直接截取头4位
        byte[] result2 = new byte[4];
        result2[0] = (byte) (longValue >> 24);
        result2[1] = (byte) (longValue >> 16 & 0x00000000000000FF);
        result2[2] = (byte) (longValue >> 8 & 0x00000000000000FF);
        result2[3] = (byte) (longValue);
        return result2;
    }
    
    public static int bytes2Int(byte[] byteNum) {  
        int num = 0;  
        for (int ix = 0; ix < 4; ++ix) {  
            num <<= 8;  
            num |= (byteNum[ix] & 0xff);  
        }  
        return num;  
    }
    public static int bytesToInt(byte[] bytes){
    	byte[] dest = new byte[4];
        if(bytes.length <= 4){
        	for(int j=0;j<bytes.length;j++){
        		dest[3-j] = bytes[bytes.length-1-j];
        	}
        }
        return bytes2Int(dest);
    }
    
	public static byte[] int2bytes(int n)
	{
		byte[] result = new byte[4];
		result[0] = (byte)((n >>>24) & 0xff);
		result[1] = (byte)((n >>>16) & 0xff);
		result[2] = (byte)((n >>>8) & 0xff);
		result[3] = (byte)((n >>>0) & 0xff);
		return result;
	}
    
    public static long U32ToLong(byte[] bytes) {
        byte[] dest = new byte[8];
        if(bytes.length < 8){
        	for(int j=0;j<bytes.length;j++){
        		dest[7-j] = bytes[bytes.length-1-j];
        	}
        }
        return bytes2Long(dest);
    }
    
    public static long bytes2Long(byte[] bytes){
    	ByteBuffer buffer = ByteBuffer.allocate(8);
    	buffer.put(bytes, 0, bytes.length);
        buffer.flip();
        return buffer.getLong();
    }

    // public static byte[] long2Byte(long x) {
    // byte[] bb = new byte[8];
    // bb[0] = (byte) (x >> 56);
    // bb[1] = (byte) (x >> 48);
    // bb[2] = (byte) (x >> 40);
    // bb[3] = (byte) (x >> 32);
    // bb[4] = (byte) (x >> 24);
    // bb[5] = (byte) (x >> 16);
    // bb[6] = (byte) (x >> 8);
    // bb[7] = (byte) (x >> 0);
    // return bb;
    // }

    /**
     * 0x1B, 0x10 --> "2716"
     *
     * @param in
     * @return
     */
    public static String buffer2NormalStr(ByteBuf in) {
        StringBuilder result = new StringBuilder("");
        byte[] byteArray = getByteArray(in);
        for (byte b : byteArray) {
            result.append(b);
        }
        return result.toString();
    }

    /**
     * 用于员工编号，剔除pos发送的右补0的byte[]
     *
     * @param in
     * @return
     */
    public static String decodeWorkNum(ByteBuf in) {
        int index = ByteBufUtil.indexOf(in,0,5,(byte) 0x00);
        return index == -1 ? decodeASC(in.readBytes(5)) : decodeASC(in.readBytes(index));
    }

    /**
     * 转换金额，放大缩小100倍
     *
     * @param amount
     * @return
     */
    public static Long transformMoneyFromRealToPosNeed(BigDecimal amount) {
        // 将实际金额转换成为pos所需要的放大100倍的 long型
        // double d = amount.doubleValue();
        // BigDecimal doubleValue;
        // if (d > 0) {
        // doubleValue = amount.multiply(new BigDecimal("100"));
        // } else {
        // logger.error("Error : Amount < 0");
        // doubleValue = new BigDecimal("0");
        // }
        //
        // return doubleValue.longValue();

        BigDecimal doubleValue;
        doubleValue = amount.multiply(new BigDecimal("100"));

        return doubleValue.longValue();
    }

    public static BigDecimal transformMoneyToReal(Long amount) {
        // 从pos读出来的long缩小100倍转换成BigDecimal,对数量，单价，金额都有效
        BigDecimal in = new BigDecimal(amount + "");
        in = in.divide(new BigDecimal("100"));
        return in;
    }

    /**
     * ByteBuf <--> byte[]
     *
     * @param input
     * @return
     */
    public static byte[] getByteArray(ByteBuf input) {
        // 显示全部
        byte[] output = new byte[input.writerIndex()];
        for (int i = 0; i < input.writerIndex(); i++) {
            output[i] = input.getByte(i);
        }
        return output;
    }

    public static byte[] getRawByteArray(ByteBuf in) {
        // 显示可读部分
        ByteBuf input = in.copy();
        byte[] result = new byte[input.readableBytes()];
        for (int i = 0; i < result.length; i++) {
            result[i] = input.readByte();
        }
        return result;
    }

    /**
     * byte[] utility
     *
     * @param arr
     * @param fixedLength
     * @return
     */
    public static byte[] addZeroInLeftSide(byte[] arr, int fixedLength) {
        // 不足位的左补0
        int len = arr.length;
        byte[] result = new byte[fixedLength];
        if (len == fixedLength) {
            return arr;
        } else if (len < fixedLength) {
            for (int i = 0; i < fixedLength - len - 1; i++) {
                result[i] = 0;
            }
            for (int i = fixedLength - len; i < fixedLength; i++) {
                result[i] = arr[i - fixedLength + len];
            }
        }
        return result;
    }
    
    
    public static byte[] intToByte(int number) { 
        int temp = number; 
        byte[] b = new byte[4]; 
        for (int i = 0; i < b.length; i++) { 
            b[i] = new Integer(temp & 0xff).byteValue();// 将最低位保存在最低位 
            temp = temp >> 8; // 向右移8位 
        } 
        return b; 
    }
    
    public static int byteToInt(byte[] b) { 
        int s = 0; 
        int s0 = b[0] & 0xff;// 最低位 
        int s1 = b[1] & 0xff; 
        int s2 = b[2] & 0xff; 
        int s3 = b[3] & 0xff; 
        s3 <<= 24; 
        s2 <<= 16; 
        s1 <<= 8; 
        s = s0 | s1 | s2 | s3; 
        return s; 
    }

    /**
     * 不足位右补0x00
     *
     * @param arr
     * @param fixedLength
     * @return
     */
    public static byte[] addZeroInRightSide(byte[] arr, int fixedLength) {
        int len = arr.length;
        byte[] result = new byte[fixedLength];
        if (len == fixedLength) {
            return arr;
        } else if (len < fixedLength) {
            for (int i = fixedLength - 1; i > fixedLength - len - 1; i--) {
                result[i] = 0;
            }
            for (int i = 0; i < len; i++) {
                result[i] = arr[i];
            }
        } else {
            throw new RuntimeException("byte长度不合法");
        }
        return result;
    }

    public static byte[] trimFromEnd(byte[] arr, int num) {
        return Arrays.copyOf(arr,arr.length - num);
    }

    public static String trimFieldForJson(String src, String firstUnusedFiled) {
        StringBuilder result = new StringBuilder();
        src = src.substring(0,src.indexOf(firstUnusedFiled) - 2);
        result.append(src).append("}");
        return result.toString();
    }

    /**
     * FAT Date
     *
     * @param in
     * @return
     */
    public static long decodeFAT(ByteBuf in) {
    	String formatter = "yyyyMMddHHmmss";
        return transformTimeIntoLong(String.valueOf(new Date(Long.parseLong(decodeFAT16Time(in))).getTime()),formatter);
    }

    public static String decodeFAT16Time(ByteBuf in) {

        byte[] datetime = getRawByteArray(in);
        if (datetime.length != 4)
            return "";
        int datePart = (int) (((datetime[0] & 0x000000FF) << 8) | (0x000000FF & datetime[1]));// yyyyMMdd
        int timePart = (int) (((datetime[2] & 0x000000FF) << 8) | (0x000000FF & datetime[3]));// HHmmss
        StringBuffer sb = new StringBuffer();
        // cal year
        int year = (datePart >> 9) + 1980;
        sb.append(year);
        // cal month
        int month = (datePart & 0x01FF) >> 5;
        if (month < 10) {
            sb.append(0).append(month);
        } else {
            sb.append(month);
        }

        // cal day
        int day = (datePart & 0x001F);
        if (day < 10) {
            sb.append(0).append(day);
        } else {
            sb.append(day);
        }

        // cal hour
        int hour = timePart >> 11;
        if (hour < 10) {
            sb.append(0).append(hour);
        } else {
            sb.append(hour);
        }
        // cal Minute
        int minute = (timePart & 0x07E0) >> 5;
        if (minute < 10) {
            sb.append(0).append(minute);
        } else {
            sb.append(minute);
        }
        // cal second
        int second = (timePart & 0x1F) << 1;
        if (second < 10) {
            sb.append(0).append(second);
        } else {
            sb.append(second);
        }

        return sb.toString();
    }

    public static void showResult(byte[] arr) {
        logger.info(Arrays.toString(arr));
    }

    public static void showResult(String s) {
        logger.info(s);
    }

    public static void showResult(BigDecimal b) {
        logger.info(b);
    }
}
