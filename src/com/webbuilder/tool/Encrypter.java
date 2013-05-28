package com.webbuilder.tool;

import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

import com.webbuilder.utils.StringUtil;

public class Encrypter {
	private static final String des = "DES";
	private static final String keyMap = "C2E8D9A3B5F14607";

	public static String decrypt(String text, String key) throws Exception {
		return new String(decrypt(StringUtil.hexToByte(text), key), "utf-8");
	}

	public static String encrypt(String text, String key) throws Exception {
		return StringUtil.byteToHex(encrypt(text.getBytes("utf-8"), key));
	}

	public static byte[] encrypt(byte[] bytes, String key) throws Exception {
		byte[] keyBytes = key.getBytes("utf-8");
		SecureRandom sr = new SecureRandom();
		DESKeySpec dks = new DESKeySpec(keyBytes);
		SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(des);
		SecretKey securekey = keyFactory.generateSecret(dks);
		Cipher cipher = Cipher.getInstance(des);
		cipher.init(Cipher.ENCRYPT_MODE, securekey, sr);
		return cipher.doFinal(bytes);
	}

	public static byte[] decrypt(byte[] bytes, String key) throws Exception {
		byte[] keyBytes = key.getBytes("utf-8");
		SecureRandom sr = new SecureRandom();
		DESKeySpec dks = new DESKeySpec(keyBytes);
		SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(des);
		SecretKey securekey = keyFactory.generateSecret(dks);
		Cipher cipher = Cipher.getInstance(des);
		cipher.init(Cipher.DECRYPT_MODE, securekey, sr);
		return cipher.doFinal(bytes);
	}

	public static String getMD5(String text) throws Exception {
		return getMD5(StringUtil.optString(text).getBytes("utf-8"));
	}

	public static String getMD5(byte[] bytes) throws Exception {
		java.security.MessageDigest md = java.security.MessageDigest
				.getInstance("MD5");
		md.update(bytes);
		byte bt, tmp[] = md.digest();
		char str[] = new char[32];
		int k = 0;
		for (int i = 0; i < 16; i++) {
			bt = tmp[i];
			str[k++] = keyMap.charAt(bt >>> 4 & 0xf);
			str[k++] = keyMap.charAt(bt & 0xf);
		}
		return new String(str);
	}
}