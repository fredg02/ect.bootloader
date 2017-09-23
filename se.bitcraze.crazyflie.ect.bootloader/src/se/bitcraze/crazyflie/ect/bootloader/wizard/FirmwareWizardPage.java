package se.bitcraze.crazyflie.ect.bootloader.wizard;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import se.bitcraze.crazyflie.ect.bootloader.firmware.Firmware;
import se.bitcraze.crazyflie.ect.bootloader.firmware.FirmwareDownloader;

/**
 * @author Frederic Gurr
 * 
 */
public class FirmwareWizardPage extends WizardPage {

    private Label lblCfType;
    private String cfType;

    private Button officialFwRadioBtn;
    private Combo officialFwCombo;

    private Button customFwRadioBtn;
    private Text customFwFileText;
    private Button customFwBrowseButton;

    private List<Firmware> allFirmwares = new ArrayList<Firmware>();
    private List<Firmware> mFilteredFirmwares = new ArrayList<Firmware>();

    private Firmware mSelectedFirmware;
    private File mFirmwareFile;

    private Label officialFwReleaseInfoValueLabel;
    private Label officialFwReleaseDateValueLabel;
    private StyledText officialFwReleaseNotesText;

    /**
     * Create the wizard.
     */
    public FirmwareWizardPage() {
        super("wizardPage");
        setTitle("Firmware");
        setDescription("Choose firmware");
    }

    /**
     * Create contents of the wizard.
     * @param parent
     */
    public void createControl(Composite parent) {
        Composite container_1 = new Composite(parent, SWT.NULL);

        setControl(container_1);
        RowLayout rl_container_1 = new RowLayout(SWT.VERTICAL);
        container_1.setLayout(rl_container_1);

        lblCfType = new Label(container_1, SWT.NONE);
        lblCfType.setBounds(10, 10, 92, 15);
        lblCfType.setText("Crazyflie type: ");
        
        officialFwControls(container_1);
        customFwControls(container_1);

        // default selection
        officialFwRadioBtn.setSelection(true);
        setEnablement(true);

        setPageComplete(false);
    }

    private void officialFwControls(Composite container) {
        officialFwRadioBtn = new Button(container, SWT.RADIO);
        officialFwRadioBtn.setText("Official firmware");
        officialFwRadioBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                setEnablement(true);
                if (officialFwRadioBtn.getSelection() && officialFwCombo.getSelectionIndex() != -1) {
                    setPageComplete(true);
                } else {
                    setPageComplete(false);
                }
            }
        });

        Composite officialFwComposite = new Composite(container, SWT.NONE);
        officialFwComposite.setLayoutData(new RowData(500, SWT.DEFAULT));
        GridLayout gl_officialFwComposite = new GridLayout(2, false);
        gl_officialFwComposite.marginLeft = 10;
        officialFwComposite.setLayout(gl_officialFwComposite);

        Label officialFwReleaseNameLabel = new Label(officialFwComposite, SWT.NONE);
        officialFwReleaseNameLabel.setText("Name:");

        officialFwCombo = new Combo(officialFwComposite, SWT.READ_ONLY);
        GridData gd_officialFwCombo = new GridData(SWT.FILL, SWT.CENTER, true, true);
        gd_officialFwCombo.minimumWidth = 100;
        gd_officialFwCombo.minimumHeight = 25;
        officialFwCombo.setLayoutData(gd_officialFwCombo);

        officialFwCombo.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event event) {
                if (officialFwRadioBtn.getSelection() && officialFwCombo.getSelectionIndex() != -1) {
                    mSelectedFirmware = mFilteredFirmwares.get(officialFwCombo.getSelectionIndex());
                    officialFwReleaseInfoValueLabel.setText(mSelectedFirmware.getInfo());
                    officialFwReleaseDateValueLabel.setText(mSelectedFirmware.getCreatedAt());
                    if (!mSelectedFirmware.getReleaseNotes().isEmpty()) {
                        officialFwReleaseNotesText.setText(mSelectedFirmware.getReleaseNotes());
                    } else {
                        officialFwReleaseNotesText.setText("");
                    }
                    setPageComplete(true);
                } else {
                    setPageComplete(false);
                }
            }
        });

        Label officialFwReleaseInfoLabel = new Label(officialFwComposite, SWT.NONE);
        officialFwReleaseInfoLabel.setText("Info:");
        officialFwReleaseInfoValueLabel = new Label(officialFwComposite, SWT.NONE);
        officialFwReleaseInfoValueLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        officialFwReleaseInfoValueLabel.setText("");

        Label officialFwReleaseDateLabel = new Label(officialFwComposite, SWT.NONE);
        officialFwReleaseDateLabel.setText("Release date:");
        officialFwReleaseDateValueLabel = new Label(officialFwComposite, SWT.NONE);
        officialFwReleaseDateValueLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        officialFwReleaseDateValueLabel.setText("");

        Label officialFwReleaseNotesLabel = new Label(officialFwComposite, SWT.NONE);
        officialFwReleaseNotesLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
        officialFwReleaseNotesLabel.setText("Release notes:");

        officialFwReleaseNotesText = new StyledText(officialFwComposite, SWT.BORDER | SWT.READ_ONLY | SWT.V_SCROLL | SWT.WRAP);
        GridData gd_text = new GridData(SWT.FILL, SWT.CENTER, true, true);
        gd_text.minimumHeight = 150;
        officialFwReleaseNotesText.setLayoutData(gd_text);
        final int padding = 5;
        officialFwReleaseNotesText.setLeftMargin(padding);
        officialFwReleaseNotesText.setRightMargin(padding);
        officialFwReleaseNotesText.setTopMargin(padding);
        officialFwReleaseNotesText.setBottomMargin(padding);
        officialFwReleaseNotesText.setAlwaysShowScrollBars(false);
        officialFwReleaseNotesText.setCaret(null);
    }

    private void customFwControls(Composite container) {
        customFwRadioBtn = new Button(container, SWT.RADIO);
        customFwRadioBtn.setText("Custom firmware");
        customFwRadioBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                setEnablement(false);
                if (customFwRadioBtn.getSelection() && !customFwFileText.getText().isEmpty()) {
                    checkCustomFirmware();
                } else {
                    setPageComplete(false);
                }
            }
        });

        Composite customFwComposite = new Composite(container, SWT.NONE);
        customFwComposite.setLayoutData(new RowData(500, SWT.DEFAULT));
        GridLayout gl_customFwComposite = new GridLayout(4, false);
        gl_customFwComposite.marginLeft = 10;
        customFwComposite.setLayout(gl_customFwComposite);

        Label customFwLocationLabel = new Label(customFwComposite, SWT.NONE);
        GridData gd_customFwLocationLabel = new GridData(SWT.FILL, SWT.CENTER, true, true, 1, 1);
        gd_customFwLocationLabel.minimumWidth = 80;
        customFwLocationLabel.setLayoutData(gd_customFwLocationLabel);
        customFwLocationLabel.setText("Location:");

        customFwFileText = new Text(customFwComposite, SWT.BORDER | SWT.SINGLE);
        GridData gd_customFwFileText = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gd_customFwFileText.minimumWidth = 310;
        customFwFileText.setLayoutData(gd_customFwFileText);
        customFwFileText.setText(""); //?
        customFwFileText.addKeyListener(new KeyListener() {
                @Override
                public void keyPressed(KeyEvent e) {
                }
                @Override
                public void keyReleased(KeyEvent e) {
                    if (!customFwFileText.getText().isEmpty()) {
                        checkCustomFirmware();
                    }
                }
        });

        customFwBrowseButton = new Button(customFwComposite, SWT.NONE);
        GridData gd_customFwBrowseButton = new GridData(SWT.LEFT, SWT.CENTER, true, false);
        gd_customFwBrowseButton.minimumWidth = 80;
        customFwBrowseButton.setLayoutData(gd_customFwBrowseButton);
        customFwBrowseButton.setText("Browse...");
        new Label(customFwComposite, SWT.NONE);
        customFwBrowseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                FileDialog dialog = new FileDialog(getShell());
                //dialog.setFilterExtensions(new String[]{".bin"});
                String path = dialog.open();
                if (path != null) {
                    mFirmwareFile = new File(path);
                    // TODO: check that firmware file exists?)
                    customFwFileText.setText(path);
                }
                checkCustomFirmware();
            }
        });
    }

    private void fillOfficialFwComboBox() {
        FirmwareDownloader fwDownloader = new FirmwareDownloader();
        if (allFirmwares.isEmpty()) {
            //TODO: async!!
            fwDownloader.checkForFirmwareUpdate();
            allFirmwares = fwDownloader.getFirmwares();
        }
        //TODO: if empty or error => show error message

        // Filter firmwares according to selected Crazyflie type (CF1 or CF2)
        mFilteredFirmwares.clear();
        officialFwCombo.removeAll();
        for (Firmware fw : allFirmwares) {
            if (cfType.equalsIgnoreCase(fw.getType()) || "CF1 & CF2".equalsIgnoreCase(fw.getType())) {
                mFilteredFirmwares.add(fw);
            }
        }
        if (!mFilteredFirmwares.isEmpty()) {
            Collections.sort(mFilteredFirmwares);
            Collections.reverse(mFilteredFirmwares);
            for (Firmware fw : mFilteredFirmwares) {
                officialFwCombo.add(fw.getTagName());
            }
            officialFwCombo.select(0);
            officialFwCombo.notifyListeners(SWT.Selection, new Event());
            mSelectedFirmware = mFilteredFirmwares.get(0);
            setErrorMessage(null);
        } else {
            setErrorMessage("There is problem with the list of official firmwares.");
        }
    }

    private void checkCustomFirmware() {
        File customFwFile = new File(customFwFileText.getText());
        if (customFwFile.exists() && customFwFile.isFile()) {
            mFirmwareFile = customFwFile;
            setPageComplete(true);
        } else {
            // set error message (wizard page)
            setPageComplete(false);
        }
    }

    public Firmware getFirmware() {
        return mSelectedFirmware;
    }

    public boolean isCustomFirmware() {
        return customFwRadioBtn.getSelection();
    }

    public File getFirmwareFile() {
        return mFirmwareFile;
    }

    private void setEnablement(boolean officialEnabled) {
        officialFwCombo.setEnabled(officialEnabled);
        officialFwReleaseInfoValueLabel.setEnabled(officialEnabled);
        officialFwReleaseDateValueLabel.setEnabled(officialEnabled);
        officialFwReleaseNotesText.setEnabled(officialEnabled);
        customFwFileText.setEnabled(!officialEnabled);
        customFwBrowseButton.setEnabled(!officialEnabled);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            CfTypeWizardPage pageOne = (CfTypeWizardPage) getWizard().getPreviousPage(this);
            cfType = pageOne.getCfType();
            lblCfType.setText("Crazyflie type: " + cfType);

            fillOfficialFwComboBox();
        }
    }
}
