package se.bitcraze.crazyflie.ect.bootloader.wizard;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Text;

import se.bitcraze.crazyflie.ect.bootloader.firmware.Firmware;
import se.bitcraze.crazyflie.ect.bootloader.firmware.FirmwareDownloader;
import se.bitcraze.crazyflie.lib.bootloader.Bootloader;
import se.bitcraze.crazyflie.lib.bootloader.Utilities.BootVersion;
import se.bitcraze.crazyflie.lib.crazyradio.Crazyradio;
import se.bitcraze.crazyflie.lib.crazyradio.RadioDriver;
import se.bitcraze.crazyflie.lib.usb.UsbLinkJava;

public class BootloaderWizardPage extends WizardPage {
    private String cfType;
    private ProgressBar progressBar; 
    private Text textBox;

    private File customFirmwareFile;
    private Firmware selectedOfficialFirmware;
    private Label lblCfType;
    private Label lblFwImage;
    private FirmwareWizardPage pageTwo;
    protected Bootloader bootloader;
    /**
     * Create the wizard.
     */
    public BootloaderWizardPage() {
        super("wizardPage");
        setTitle("Bootloader");
        setDescription("Upload the firmware to the Crazyflie");
    }

    /**
     * Create contents of the wizard.
     * @param parent
     */
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);

        setControl(container);

        lblCfType = new Label(container, SWT.NONE);
        lblCfType.setBounds(10, 10, 192, 15);
        lblCfType.setText("Crazyflie type: ");

        lblFwImage = new Label(container, SWT.NONE);
        lblFwImage.setBounds(10, 37, 305, 15);
        lblFwImage.setText("Firmware image: ");
        
        Label lblCfTypeValue = new Label(container, SWT.NONE);
        lblCfTypeValue.setBounds(122, 10, 57, 15);
        lblCfTypeValue.setText("");
        
        Label lblCfImageValue = new Label(container, SWT.NONE);
        lblCfImageValue.setBounds(122, 37, 57, 15);
        lblCfImageValue.setText("");
        
        progressBar = new ProgressBar(container, SWT.SMOOTH);
        progressBar.setBounds(10, 123, 566, 20);
        
        textBox = new Text(container, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
        textBox.setBounds(10, 152, 566, 141);
        
        Button btnFlashFirmware = new Button(container, SWT.NONE);
        btnFlashFirmware.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                textBox.setText("");
                if (!pageTwo.isCustomFirmware()) {
                    downloadFirmware();
                }
                flashFirmware();
            }

        });
        btnFlashFirmware.setBounds(471, 312, 105, 24);
        btnFlashFirmware.setText("Flash firmware");
    }

    protected void downloadFirmware() {
        Job job = new Job("Downloading Crazyflie firmware...") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                FirmwareDownloader fwDownloader = new FirmwareDownloader();
                appendConsole("Downloading firmware " + selectedOfficialFirmware.getTagName() + "... ");
                try {
                    fwDownloader.downloadFirmware(selectedOfficialFirmware);
                } catch (IOException e) {
                    e.printStackTrace();
                    appendConsole("Failed.\n");
                    return Status.CANCEL_STATUS;
                }
                appendConsole("Done.\n");
                return Status.OK_STATUS;
            }
        };
        job.setUser(true);
        job.schedule();
        try {
            job.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void flashFirmware() {
        FirmwareWizardPage pageTwo = (FirmwareWizardPage) getWizard().getPreviousPage(BootloaderWizardPage.this);
        boolean isCustomFw = pageTwo.isCustomFirmware();
        
        Job job = new Job("Flashing Crazyflie firmware...") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                //fail quickly, when Crazyradio is not connected
                //TODO: extract this to RadioDriver class?
                if (!isCrazyradioAvailable()) {
                    appendConsole("Please make sure that a Crazyradio (PA) is connected.\n");
                    return Status.CANCEL_STATUS;
                }
                appendConsole("Found a Crazyradio (PA).\n");

                bootloader = new Bootloader(new RadioDriver(new UsbLinkJava()));

                appendConsole("Searching Crazyflie in bootloader mode...\n");
                appendConsole("Restart the Crazyflie you want to bootload in the next 10 seconds ...\n");

                //TODO: in message dialog?
                boolean result = bootloader.startBootloader(false);
                if (!result) {
                    appendConsole("No Crazyflie found in bootloader mode.\n");
                    return Status.CANCEL_STATUS;
                }

                boolean firmwareCompatible = isFirmwareCompatible();
                if (!firmwareCompatible) {
                    return Status.CANCEL_STATUS;
                }

                File firmwareFile;
                if (!isCustomFw) {
                    firmwareFile = new File(FirmwareDownloader.RELEASES_DIR, selectedOfficialFirmware.getAssetName());
                } else {
                    firmwareFile = customFirmwareFile;
                    //TODO: does anything need to be checked??
                }

                long startTime = System.currentTimeMillis();
                boolean flashSuccessful;
                try {
                    flashSuccessful = bootloader.flash(firmwareFile);
                } catch (IOException ioe) {
                    //Log.e(LOG_TAG, ioe.getMessage());
                    flashSuccessful = false;
                }
                String flashTime = "Flashing took " + (System.currentTimeMillis() - startTime)/1000 + " seconds.\n";
                //Log.d(LOG_TAG, flashTime);
                appendConsole(flashSuccessful ? ("Flashing successful. " + flashTime) : "Flashing not successful.\n");
                
                stopFlashProcess(true);
                
                return Status.OK_STATUS;
            }
        };
        job.setUser(true);
        job.schedule();
    }

    /**
     * Check if firmware is compatible with Crazyflie
     * 
     * @return true if compatible, false otherwise
     */
    private boolean isFirmwareCompatible() {
        int protocolVersion = bootloader.getProtocolVersion();
        boolean cfType2 = !(protocolVersion == BootVersion.CF1_PROTO_VER_0 ||
                            protocolVersion == BootVersion.CF1_PROTO_VER_1);

        String cfversion = "Found Crazyflie " + (cfType2 ? "2.0" : "1.0") + ".\n";
        appendConsole(cfversion);

        // check if firmware and CF are compatible
        if (("CF2".equalsIgnoreCase(selectedOfficialFirmware.getType()) && !cfType2) ||
            ("CF1".equalsIgnoreCase(selectedOfficialFirmware.getType()) && cfType2)) {
            appendConsole("Incompatible firmware version.\n");
            return false;
        }
        return true;
    }

    private void stopFlashProcess(boolean reset) {
        if (bootloader != null) {
            if (reset) {
                appendConsole("Resetting Crazyflie to firmware mode...\n");
                bootloader.resetToFirmware();
            }
            bootloader.close();
        }
    }

    private static boolean isCrazyradioAvailable() {
        UsbLinkJava usbLinkJava = new UsbLinkJava();
        try {
            usbLinkJava.initDevice(Crazyradio.CRADIO_VID, Crazyradio.CRADIO_PID);
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return usbLinkJava.isUsbConnected();
    }

    private void appendConsole(String text) {
        textBox.getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                if(!textBox.isDisposed() ) {
                    textBox.append(text);
                }
            }
        } );
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            CfTypeWizardPage pageOne = (CfTypeWizardPage) getWizard().getStartingPage();
            pageTwo = (FirmwareWizardPage) getWizard().getPreviousPage(this);
            cfType = pageOne.getCfType();
            lblCfType.setText("Crazyflie type: " + cfType);
            String firmwareText = "";
            if (pageTwo.isCustomFirmware()) {
                customFirmwareFile = pageTwo.getFirmwareFile();
                firmwareText = customFirmwareFile.getPath();
            } else {
                selectedOfficialFirmware = pageTwo.getFirmware();
                if (selectedOfficialFirmware != null) {
                    firmwareText = selectedOfficialFirmware.getAssetName();
                }
            }
            
            lblFwImage.setText("Firmware image: " + firmwareText);
            
            textBox.setText("");
            //TODO: reset progressbar?
        }
    }
}
