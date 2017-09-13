package se.bitcraze.crazyflie.ect.bootloader.wizard;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
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
 * TODO: 
 * - Fill combo
 * - Fix Layout
 * - Set image (top right corner)
 * 
 *
 */
public class FirmwareWizardPage extends WizardPage {

    private Label lblCfType;
    private String cfType;

    private Button officialFwRadioBtn;
    private Combo officialFwCombo;
    private Button officialFwReleaseNotesBtn;

    private Button customFwRadioBtn;
    private Text customFwFileText;
    private Button customFwBrowseButton;

    private List<Firmware> allFirmwares = new ArrayList<Firmware>();
    private List<Firmware> mFilteredFirmwares = new ArrayList<Firmware>();

    private Firmware mSelectedFirmware;
    private File mFirmwareFile;

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
        rl_container_1.fill = true;
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

//        Composite composite = new Composite(container, SWT.NONE);
//        RowLayout rl_composite = new RowLayout(SWT.VERTICAL);
//        rl_composite.fill = true;
//        composite.setLayout(rl_composite);

        officialFwCombo = new Combo(container, SWT.READ_ONLY);

        officialFwCombo.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event event) {
                if (officialFwRadioBtn.getSelection() && officialFwCombo.getSelectionIndex() != -1) {
                    mSelectedFirmware = mFilteredFirmwares.get(officialFwCombo.getSelectionIndex());
                    if (mSelectedFirmware.getReleaseNotes().isEmpty()) {
                        officialFwReleaseNotesBtn.setEnabled(false);
                    } else {
                        officialFwReleaseNotesBtn.setEnabled(true);
                    }
                    setPageComplete(true);
                } else {
                    setPageComplete(false);
                }
            }
        });
        
        officialFwReleaseNotesBtn = new Button(container, SWT.NONE);
        officialFwReleaseNotesBtn.setText("Show release notes");
        officialFwReleaseNotesBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String releaseNotes = "";
                if (mSelectedFirmware != null) {
                    releaseNotes = mSelectedFirmware.getReleaseNotes();
                }
                MessageDialog.openInformation(getShell(), "Release notes", releaseNotes);
            }
        });
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

        Composite customComposite = new Composite(container, SWT.NONE);
        GridLayout layout = new GridLayout(2, false);
        customComposite.setLayout(layout);

        customFwFileText = new Text(customComposite, SWT.BORDER | SWT.SINGLE);
        customFwFileText.setText(""); //?
        customFwFileText.setLayoutData(new RowData(200, SWT.DEFAULT));
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
        customFwFileText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL, SWT.NONE, true, false));

        customFwBrowseButton = new Button(customComposite, SWT.NONE);
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

    private void fillOfficialFwComboBox() {
        FirmwareDownloader firmwareDownloader = new FirmwareDownloader();
        if (allFirmwares.isEmpty()) {
            //TODO: async!!
            firmwareDownloader.checkForFirmwareUpdate();
            allFirmwares = firmwareDownloader.getFirmwares();
        }
        //TODO: if empty or error => show error message

        // Filter firmwares according to selected Crazyflie type (CF1 or CF2)
        mFilteredFirmwares.clear();
        for (Firmware fw : allFirmwares) {
            if (cfType.equalsIgnoreCase(fw.getType()) || "CF1 & CF2".equalsIgnoreCase(fw.getType())) {
                mFilteredFirmwares.add(fw);
            }
        }
        // TODO: sort?
        Collections.sort(mFilteredFirmwares);
        Collections.reverse(mFilteredFirmwares);
        officialFwCombo.removeAll();
        for (Firmware fw : mFilteredFirmwares) {
            String info = fw.getInfo() + "\t";
            officialFwCombo.add(fw.getTagName() + "\t\t" + info + fw.getCreatedAt());
        }
        officialFwCombo.select(0);
        mSelectedFirmware = mFilteredFirmwares.get(0);
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
        officialFwReleaseNotesBtn.setEnabled(officialEnabled);
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
