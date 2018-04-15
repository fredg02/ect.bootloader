package se.bitcraze.crazyflie.ect.bootloader.wizard;

import java.io.File;

import org.junit.Test;

public class FirmwareWizardPageTest {

    @Test
    public void firmwareIdentificationTest() {
        File customFw = new File("crazyflie-firmware", "cf2-2017.06.bin");
        FirmwareWizardPage.getCustomFwType(customFw);

        File customFw2 = new File("crazyflie-firmware", "cf2_nrf-2017.05.bin");
        FirmwareWizardPage.getCustomFwType(customFw2);
    }

}
