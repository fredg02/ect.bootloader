package se.bitcraze.crazyflie.ect.bootloader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Utils {

    public static byte[] readBytes(File customFw) {
        FileInputStream fis = null;
        byte fileContent[] = new byte[20];
        try {
            fis = new FileInputStream(customFw);
            fis.read(fileContent);
//            System.out.println("File hexes: " + getHexString(fileContent));
            return fileContent;
        } catch (FileNotFoundException e) {
            System.out.println("File not found " + e);
            return fileContent;
        } catch (IOException ioe) {
            System.out.println("Exception while reading file " + ioe);
            return fileContent;
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException ioe) {
                System.out.println("Error while closing stream: " + ioe);
            }
        }
    }

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String getHexString(byte[] bytes) {
        StringBuffer sb = new StringBuffer();
        for (byte b : bytes) {
            sb.append(bytesToHex(new byte[] { b }));
            sb.append(",");
        }
        return sb.toString();
    }
}
