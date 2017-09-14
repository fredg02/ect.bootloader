package se.bitcraze.crazyflie.ect.bootloader.firmware;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
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
    public void testDownloadFirmware() throws IOException {
        Firmware testFw = new Firmware("2017.06", "2017.06", "2017-06-29");
        testFw.setAsset("crazyflie-2017.06.zip", 355883, "https://github.com/bitcraze/crazyflie-release/releases/download/2017.06/crazyflie-2017.06.zip");

        FirmwareDownloader fwDownloader = new FirmwareDownloader();
        fwDownloader.downloadFirmware(testFw);

        File firmwareFile = new File(FirmwareDownloader.RELEASES_DIR, testFw.getAssetName());
        assertTrue("Firmware file is missing.", firmwareFile.exists());
        assertTrue("Firmware file is empty.", firmwareFile.length() > 0);
    }

}
