package se.bitcraze.crazyflie.ect.bootloader.firmware;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.List;

import org.junit.Test;

public class FirmwareDownloaderTest {

    @Test
    public void testCheckForFirmwareUpdate() {
        // delete current releases file
        File fwFile = new File(FirmwareDownloader.RELEASES_DIR, FirmwareDownloader.RELEASES_JSON);
        if (fwFile.exists()) {
            fwFile.delete();
        }
        assertTrue(!fwFile.exists());
        
        // download firmware - 1st time
        FirmwareDownloader fwDownloader = new FirmwareDownloader();
        fwDownloader.checkForFirmwareUpdate();
        
        assertTrue(!fwDownloader.getFirmwares().isEmpty());
        
        System.out.println();

        // download firmware - 2nd time
        fwDownloader.checkForFirmwareUpdate();
        
        System.out.println();
        // list firmwares
        List<Firmware> firmwares = fwDownloader.getFirmwares();
        for (Firmware fw : firmwares) {
            System.out.println(fw);
        }
        assertTrue(!firmwares.isEmpty());
    }

    @Test
    public void testDownloadFile() {
        fail("Not yet implemented");
    }

}
