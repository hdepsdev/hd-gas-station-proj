package com.bhz.eps.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Properties;
import java.util.UUID;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class Utils {

	private final static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");

	public final static Properties systemConfiguration = new Properties();

	static {
		try {
			systemConfiguration.load(Utils.class.getClassLoader().getResourceAsStream("conf/sys.conf"));

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String generateUUID(String sep) {
		if (sep != null) {
			return com.fasterxml.uuid.Generators.randomBasedGenerator(new java.util.Random()).generate().toString()
					.replace("-", sep);
		} else {
			return com.fasterxml.uuid.Generators.randomBasedGenerator(new java.util.Random()).generate().toString();
		}
	}

	public static String generateCompactUUID() {
		return generateUUID("");
	}

	// UUID Dictionary (Alpha + Number)
	private final static String[] chars = new String[] { "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l",
			"m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "0", "1", "2", "3", "4", "5", "6",
			"7", "8", "9", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R",
			"S", "T", "U", "V", "W", "X", "Y", "Z" };
	// UUID Dictionary (Only Number)
	private final static String[] uuidNumbers = new String[] { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9" };

	public static String generate8BitNumberUUID() {
		StringBuffer shortBuffer = new StringBuffer();
		String uuid = UUID.randomUUID().toString().replace("-", "");
		for (int i = 0; i < 8; i++) {
			String str = uuid.substring(i * 4, i * 4 + 4);
			int x = Integer.parseInt(str, 16);
			shortBuffer.append(uuidNumbers[x % 0x0A]);
		}
		return shortBuffer.toString();
	}

	public static String generate8BitUUID() {
		StringBuffer shortBuffer = new StringBuffer();
		String uuid = UUID.randomUUID().toString().replace("-", "");
		for (int i = 0; i < 8; i++) {
			String str = uuid.substring(i * 4, i * 4 + 4);
			int x = Integer.parseInt(str, 16);
			shortBuffer.append(chars[x % 0x3E]);
		}
		return shortBuffer.toString();
	}

	public static byte getSysVersion() {
		return 0x01;
	}

	public static String getServerTime() {
		return sdf.format(System.currentTimeMillis());
	}

	public static byte[] genTPDUHeader(long tpduLength) {
		ByteBuf bb = Unpooled.buffer(9);
		bb.writeByte(0x1b).writeByte(0x10);
		bb.order(ByteOrder.LITTLE_ENDIAN).writeInt((int)tpduLength);
		bb.writeByte(0x01);
		bb.writeShort(0x0000);
		byte[] data = bb.array();
		byte crcValue = CRC8.calc(data);
		byte[] ret = new byte[10];
		System.arraycopy(data, 0, ret, 0, data.length);
		ret[9] = crcValue;
		return ret;
	}

	public static byte[] concatTwoByteArray(byte[] b1, byte[] b2) {
		byte[] result = new byte[b1.length + b2.length];
		System.arraycopy(b1, 0, result, 0, b1.length);
		System.arraycopy(b2, 0, result, b1.length, b2.length);
		return result;
	}

	public static void initByteArray(byte[] ba, byte b) {
		for (int i = 0; i < ba.length; i++) {
			ba[i] = b;
		}
	}

	public static void setHeaderForHHT(ByteBuf hhtByte, String hexLength, String version, String terminal,
			String messageType) {
		byte[] length = hexStringToByteAndAddZeroInLeftSide(hexLength, 4);
		hhtByte.writeBytes(length);
		hhtByte.writeByte(0x00);
		hhtByte.writeByte(0x00);
		try {
			hhtByte.writeBytes(Converts.addZeroInLeft2Str(version, 2).getBytes("utf-8"));
			hhtByte.writeBytes(Converts.addZeroInLeft2Str(terminal, 3).getBytes("utf-8"));
			hhtByte.writeBytes(Converts.addZeroInLeft2Str(messageType, 4).getBytes("utf-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	public static byte[] hexStringToByteAndAddZeroInLeftSide(String hexLength, int fixedLength) {
		return Converts.addZeroInLeftSide(
				Converts.hexStringToByte(Converts.addZeroInLeft2Str(hexLength.toUpperCase(), 2)), fixedLength);
	}

	public static String rightPad(String text, int length, char c) {
		char[] array = new char[length];
		Arrays.fill(array, text.length(), length, c);
		System.arraycopy(text.toCharArray(), 0, array, 0, text.length());
		return new String(array);
	}
}
