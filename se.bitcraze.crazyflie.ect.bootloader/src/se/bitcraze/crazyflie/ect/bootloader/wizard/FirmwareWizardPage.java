package se.bitcraze.crazyflie.ect.bootloader.wizard;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
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
 * Wizard page where the firmware is selected 
 * 
 * @author Frederic Gurr
 * 
 */
public class FirmwareWizardPage extends WizardPage {

    private String cfType;
    private Label cfTypeValueLabel;

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
        Composite container = new Composite(parent, SWT.NULL);
        container.setLayout(new GridLayout(3, false));

        setControl(container);

        Label cfTypeLabel = new Label(container, SWT.NONE);
        GridData gd_cfTypeLabel = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        gd_cfTypeLabel.widthHint = 110;
        cfTypeLabel.setLayoutData(gd_cfTypeLabel);
        cfTypeLabel.setText("Crazyflie type:");

        cfTypeValueLabel = new Label(container, SWT.NONE);
        GridData gd_cfTypeValueLabel = new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1);
        gd_cfTypeValueLabel.minimumWidth = 200;
        cfTypeValueLabel.setLayoutData(gd_cfTypeValueLabel);
        cfTypeValueLabel.setText("");

        officialFwControls(container);
        customFwControls(container);

        // default selection
        officialFwRadioBtn.setSelection(true);
        setEnablement(true);

        setPageComplete(false);
    }

    private void officialFwControls(Composite pContainer) {
        officialFwRadioBtn = new Button(pContainer, SWT.RADIO);
        GridData gd_officialFwRadioBtn = new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1);
        gd_officialFwRadioBtn.verticalIndent = 10;
        officialFwRadioBtn.setLayoutData(gd_officialFwRadioBtn);
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

        Label officialFwReleaseNameLabel = new Label(pContainer, SWT.NONE);
        GridData gd_officialFwReleaseNameLabel = new GridData(SWT.LEFT, SWT.CENTER, true, false);
        gd_officialFwReleaseNameLabel.horizontalIndent = 10;
        officialFwReleaseNameLabel.setLayoutData(gd_officialFwReleaseNameLabel);
        officialFwReleaseNameLabel.setText("Name:");

        officialFwCombo = new Combo(pContainer, SWT.READ_ONLY);
        GridData gd_officialFwCombo = new GridData(SWT.FILL, SWT.CENTER, true, true, 2, 1);
        gd_officialFwCombo.minimumWidth = 100;
        gd_officialFwCombo.minimumHeight = 15;
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

        Label officialFwReleaseInfoLabel = new Label(pContainer, SWT.NONE);
        GridData gd_officialFwReleaseInfoLabel = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        gd_officialFwReleaseInfoLabel.horizontalIndent = 10;
        officialFwReleaseInfoLabel.setLayoutData(gd_officialFwReleaseInfoLabel);
        officialFwReleaseInfoLabel.setText("Info:");
        officialFwReleaseInfoValueLabel = new Label(pContainer, SWT.NONE);
        officialFwReleaseInfoValueLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        officialFwReleaseInfoValueLabel.setText("");

        Label officialFwReleaseDateLabel = new Label(pContainer, SWT.NONE);
        GridData gd_officialFwReleaseDateLabel = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        gd_officialFwReleaseDateLabel.horizontalIndent = 10;
        officialFwReleaseDateLabel.setLayoutData(gd_officialFwReleaseDateLabel);
        officialFwReleaseDateLabel.setText("Release date:");
        officialFwReleaseDateValueLabel = new Label(pContainer, SWT.NONE);
        officialFwReleaseDateValueLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        officialFwReleaseDateValueLabel.setText("");

        Label officialFwReleaseNotesLabel = new Label(pContainer, SWT.NONE);
        GridData gd_officialFwReleaseNotesLabel = new GridData(SWT.LEFT, SWT.TOP, false, false);
        gd_officialFwReleaseNotesLabel.horizontalIndent = 10;
        officialFwReleaseNotesLabel.setLayoutData(gd_officialFwReleaseNotesLabel);
        officialFwReleaseNotesLabel.setText("Release notes:");

        officialFwReleaseNotesText = new StyledText(pContainer, SWT.BORDER | SWT.READ_ONLY | SWT.V_SCROLL | SWT.WRAP);
        GridData gd_text = new GridData(SWT.FILL, SWT.CENTER, true, true, 2, 1);
        gd_text.minimumHeight = 150;
        officialFwReleaseNotesText.setLayoutData(gd_text);
        final int padding = 5;
        officialFwReleaseNotesText.setMargins(padding, padding, padding, padding);
        officialFwReleaseNotesText.setAlwaysShowScrollBars(false);
        officialFwReleaseNotesText.setCaret(null);
    }

    private void customFwControls(Composite pContainer) {
        customFwRadioBtn = new Button(pContainer, SWT.RADIO);
        GridData gd_customFwRadioBtn = new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1);
        gd_customFwRadioBtn.verticalIndent = 10;
        customFwRadioBtn.setLayoutData(gd_customFwRadioBtn);
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

        Label customFwLocationLabel = new Label(pContainer, SWT.NONE);
        GridData gd_customFwLocationLabel = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gd_customFwLocationLabel.horizontalIndent = 10;
        gd_customFwLocationLabel.minimumWidth = 80;
        customFwLocationLabel.setLayoutData(gd_customFwLocationLabel);
        customFwLocationLabel.setText("Location:");

        customFwFileText = new Text(pContainer, SWT.BORDER | SWT.SINGLE);
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

        customFwBrowseButton = new Button(pContainer, SWT.NONE);
        GridData gd_customFwBrowseButton = new GridData(SWT.LEFT, SWT.CENTER, true, false);
        gd_customFwBrowseButton.minimumWidth = 80;
        customFwBrowseButton.setLayoutData(gd_customFwBrowseButton);
        customFwBrowseButton.setText("Browse...");
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

    private void checkForFirmwareUpdates() {
        FirmwareDownloader fwDownloader = new FirmwareDownloader();
        if (allFirmwares.isEmpty()) {
            Job job = new Job("Checking available firmware updates...") {
                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    fwDownloader.checkForFirmwareUpdate();
                    allFirmwares = fwDownloader.getFirmwares();
                    if (allFirmwares.isEmpty()) {
                        return Status.CANCEL_STATUS;
                    }
                    officialFwCombo.getDisplay().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            fillOfficialFwComboBox();
                            
                        }
                    } );
                    return Status.OK_STATUS;
                }
            };
            job.setUser(true);
            job.schedule();
        }
    }

    /**
     * Filter firmwares according to selected Crazyflie type (CF1 or CF2)
     */
    private void filterFirmwares() {
        mFilteredFirmwares.clear();
        for (Firmware fw : allFirmwares) {
            if (cfType.equalsIgnoreCase(fw.getType()) || "CF1 & CF2".equalsIgnoreCase(fw.getType())) {
                mFilteredFirmwares.add(fw);
            }
        }
    }

    private void fillOfficialFwComboBox() {
        filterFirmwares();
        officialFwCombo.removeAll();
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
            cfTypeValueLabel.setText(cfType);
            checkForFirmwareUpdates();
        }
    }
}
