package se.bitcraze.crazyflie.ect.bootloader.wizard;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import se.bitcraze.crazyflie.ect.bootloader.firmware.Firmware;
import se.bitcraze.crazyflie.ect.bootloader.firmware.FirmwareDownloader;
import se.bitcraze.crazyflie.lib.bootloader.Bootloader;
import se.bitcraze.crazyflie.lib.bootloader.Utilities.BootVersion;
import se.bitcraze.crazyflie.lib.crazyradio.Crazyradio;
import se.bitcraze.crazyflie.lib.crazyradio.RadioDriver;
import se.bitcraze.crazyflie.lib.usb.UsbLinkJava;

/**
 * Wizard page where the firmware is flashed
 * 
 * @author Frederic Gurr
 *
 */
public class BootloaderWizardPage extends WizardPage {
    private FirmwareWizardPage pageTwo;
    private Firmware selectedOfficialFirmware;
    private File customFirmwareFile;

    private Label cfTypeValueLabel;
    private Label fwImageValueLabel;
    private StyledText console;
//    private ProgressBar progressBar;
    private Button flashFirmwareButton;

    private Bootloader bootloader;
    private Job downloadJob;

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
        container.setLayout(new GridLayout(2, false));

        setControl(container);

        Label cfTypeLabel = new Label(container, SWT.NONE);
        GridData gd_cfTypeLabel = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        gd_cfTypeLabel.widthHint = 110;
        cfTypeLabel.setLayoutData(gd_cfTypeLabel);
        cfTypeLabel.setText("Crazyflie type:");

        cfTypeValueLabel = new Label(container, SWT.NONE);
        GridData gd_cfTypeValueLabel = new GridData(SWT.LEFT, SWT.CENTER, true, false);
        gd_cfTypeValueLabel.minimumWidth = 200;
        cfTypeValueLabel.setLayoutData(gd_cfTypeValueLabel);
        cfTypeValueLabel.setText("");

        Label fwImageLabel = new Label(container, SWT.NONE);
        GridData gd_fwImageLabel = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        gd_fwImageLabel.widthHint = 110;
        fwImageLabel.setLayoutData(gd_fwImageLabel);
        fwImageLabel.setText("Firmware file:");

        fwImageValueLabel = new Label(container, SWT.NONE);
        GridData gd_fwImageValueLabel = new GridData(SWT.LEFT, SWT.CENTER, true, false);
        gd_fwImageValueLabel.minimumWidth = 400;
        fwImageValueLabel.setLayoutData(gd_fwImageValueLabel);
        fwImageValueLabel.setText("");

        console = new StyledText(container, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
        GridData gd_console = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
        gd_console.verticalIndent = 30;
        gd_console.heightHint = 150;
        gd_console.minimumHeight = 100;
        console.setLayoutData(gd_console);
        int padding = 5;
        console.setMargins(padding, padding, padding, padding);
        console.setAlwaysShowScrollBars(false);
        console.setCaret(null);

        //FIXME
//        progressBar = new ProgressBar(container, SWT.SMOOTH);
//        progressBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

        flashFirmwareButton = new Button(container, SWT.NONE);
        GridData gd_flashFirmwareButton = new GridData(SWT.RIGHT, SWT.CENTER, true, false, 2, 1);
        gd_flashFirmwareButton.minimumWidth = 110;
        flashFirmwareButton.setLayoutData(gd_flashFirmwareButton);
        flashFirmwareButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                console.setText("");
                if (!pageTwo.isCustomFirmware()) {
                    downloadFirmware();
                    if (downloadJob.getResult() == Status.OK_STATUS) {
                        flashFirmware();
                    }
                } else {
                    flashFirmware();
                }
            }
        });
        flashFirmwareButton.setText("Flash firmware");
    }

    protected void downloadFirmware() {
        downloadJob = new Job("Downloading Crazyflie firmware...") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                FirmwareDownloader fwDownloader = new FirmwareDownloader();
                appendConsole("Downloading firmware " + selectedOfficialFirmware.getTagName() + "... ");
                boolean successfulFirmwareDownload = false;
                try {
                     successfulFirmwareDownload = fwDownloader.downloadFirmware(selectedOfficialFirmware);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (!successfulFirmwareDownload) {
                    appendConsole("Failed.\n");
                    return Status.CANCEL_STATUS;
                }
                appendConsole("Done.\n");
                return Status.OK_STATUS;
            }
        };
        downloadJob.setUser(true);
        downloadJob.schedule();
        try {
            downloadJob.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void flashFirmware() {
        FirmwareWizardPage pageTwo = (FirmwareWizardPage) getWizard().getPreviousPage(BootloaderWizardPage.this);
        boolean isCustomFw = pageTwo.isCustomFirmware();

        Job flashJob = new Job("Flashing Crazyflie firmware...") {
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
        flashJob.setUser(true);
        flashJob.addJobChangeListener(new JobChangeAdapter() {
            
            @Override
            public void running(IJobChangeEvent event) {
                flashFirmwareButton.getDisplay().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        flashFirmwareButton.setEnabled(false);
                    }
                });
            }
            
            @Override
            public void done(IJobChangeEvent event) {
                flashFirmwareButton.getDisplay().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        flashFirmwareButton.setEnabled(true);
                    }
                });
            }
        });
        flashJob.schedule();
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
        if ((CfTypeWizardPage.CF2.equalsIgnoreCase(selectedOfficialFirmware.getType()) && !cfType2) ||
            (CfTypeWizardPage.CF1.equalsIgnoreCase(selectedOfficialFirmware.getType()) && cfType2)) {
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
        console.getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                if(!console.isDisposed() ) {
                    console.append(text);
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
            String cfType = pageOne.getCfType();
            cfTypeValueLabel.setText(cfType);
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
            fwImageValueLabel.setText(firmwareText);
            console.setText("");
            flashFirmwareButton.setEnabled(true);
            //TODO: reset progressbar?
        }
    }
}
