package se.bitcraze.crazyflie.ect.bootloader.wizard;

import java.io.File;

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

public class BootloaderWizardPage extends WizardPage {
    private Text text;
    private String cfType;

    private File customFirmwareFile;
    private Firmware selectedOfficialFirmware;
    private Label lblCfType;
    private Label lblFwImage;

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
        
        ProgressBar progressBar = new ProgressBar(container, SWT.NONE);
        progressBar.setBounds(10, 123, 566, 20);
        
        text = new Text(container, SWT.BORDER);
        text.setBounds(10, 152, 566, 141);
        
        Button btnFlashFirmware = new Button(container, SWT.NONE);
        btnFlashFirmware.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
            }
        });
        btnFlashFirmware.setBounds(471, 312, 105, 24);
        btnFlashFirmware.setText("Flash firmware");
        
    }
    
    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            CfTypeWizardPage pageOne = (CfTypeWizardPage) getWizard().getStartingPage();
            cfType = pageOne.getCfType();
            lblCfType.setText("Crazyflie type: " + cfType);
            FirmwareWizardPage pageTwo = (FirmwareWizardPage) getWizard().getPreviousPage(this);
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
        }
    }
}
