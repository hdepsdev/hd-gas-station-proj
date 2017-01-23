package com.bhz.eps.util;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.DESedeKeySpec;
/**
 * 
 * @author yaoh
 *
 */
public class TripleDES {

//    private static final String Algorithm = "DESede/CBC/NOPADDING";
    private static final String Algorithm = "DESede/ECB/NOPADDING";
    public SecretKey getKey(byte[] key){
        try {
            DESedeKeySpec desKeySpec = new DESedeKeySpec(key);
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("TripleDES");
            SecretKey secretKey = keyFactory.generateSecret(desKeySpec);
            return secretKey;
        } catch (Exception e) {
            throw new RuntimeException("Init key error, Cause: " + e);
        }
    }
   

    public byte[] encrypt(byte[] msg, byte[] key) throws Exception{
        
        try {
            Cipher cipher = Cipher.getInstance(Algorithm);
            cipher.init(Cipher.ENCRYPT_MODE, getKey(key));

            if (msg.length % 8 != 0) {
                byte[] padded = new byte[msg.length + 8 - (msg.length % 8)];
                System.arraycopy(msg, 0, padded, 0, msg.length);
                msg = padded;
            }
            byte[] cipherContent = new byte[cipher.getOutputSize(msg.length)];
            int ctLength = cipher.update(msg, 0, msg.length, cipherContent, 0);
            ctLength += cipher.doFinal(cipherContent, ctLength);
            return cipherContent;
        } catch (ShortBufferException ex) {
            throw new Exception(ex.getMessage());
        } catch (InvalidKeyException ex) {
            throw new Exception(ex.getMessage());
        } catch (NoSuchAlgorithmException ex) {
            throw new Exception(ex.getMessage());
        } catch (NoSuchPaddingException ex) {
            throw new Exception(ex.getMessage());
        } catch (javax.crypto.BadPaddingException ex) {
            throw new Exception(ex.getMessage());
        } catch (IllegalBlockSizeException ex) {
            throw new Exception(ex.getMessage());
        }catch(Exception ex){
            ex.printStackTrace();
            throw new Exception("!!Cipher content contains invalid charactor!!");
        }
    }
    
    public byte[] decrypt(byte[] msg,byte[] key) throws Exception{
        try{
            Cipher cipher = Cipher.getInstance(Algorithm);
            cipher.init(Cipher.DECRYPT_MODE, getKey(key));
            byte[] buffer = cipher.doFinal(msg);
            return buffer;
        }catch (InvalidKeyException ex) {
            throw new Exception(ex.getMessage());
        } catch (NoSuchAlgorithmException ex) {
            throw new Exception(ex.getMessage());
        } catch (NoSuchPaddingException ex) {
            throw new Exception(ex.getMessage());
        } catch (javax.crypto.BadPaddingException ex) {
            throw new Exception(ex.getMessage());
        } catch (IllegalBlockSizeException ex) {
            throw new Exception(ex.getMessage());
        }catch(Exception ex){
            ex.printStackTrace();
            throw new Exception("!!Cipher content contains invalid charactor!!");
        }
    }
   
}
