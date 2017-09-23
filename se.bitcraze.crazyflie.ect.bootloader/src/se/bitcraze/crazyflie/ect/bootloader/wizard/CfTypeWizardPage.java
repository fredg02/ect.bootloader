package se.bitcraze.crazyflie.ect.bootloader.wizard;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

/**
 * Wizard page where CF type is selected
 * 
 * @author Frederic Gurr
 *
 * TODO: store type in preference
 *
 */
public class CfTypeWizardPage extends WizardPage {

    public static final String CF1 = "CF1";
    public static final String CF2 = "CF2";

    private Button radioCf1Button;
    private Button radioCf2Button;

    /**
     * Create the wizard.
     */
    public CfTypeWizardPage() {
        super("cfTypeWizardPage");
        setTitle("Crazyflie type");
        setDescription("Choose the type of Crazyflie");
    }

    /**
     * Create contents of the wizard.
     * @param parent
     */
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);

        setControl(container);
        container.setLayout(new RowLayout(SWT.VERTICAL));
        
        radioCf1Button = new Button(container, SWT.RADIO);
        radioCf1Button.setText("Crazyflie 1.0 (CF1)");
        
        radioCf2Button = new Button(container, SWT.RADIO);
        radioCf2Button.setSelection(true);
        radioCf2Button.setText("Crazyflie 2.0 (CF2)");
    }

    public String getCfType() {
        return radioCf2Button.getSelection() ? CF2 : CF1;
    }
}
