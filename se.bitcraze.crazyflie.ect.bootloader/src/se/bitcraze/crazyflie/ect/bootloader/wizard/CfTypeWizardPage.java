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

    private static final String CF1 = "CF1";
    private static final String CF2 = "CF2";

    private Button btnRadioCf1;
    private Button btnRadioCf2;

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
        
        btnRadioCf1 = new Button(container, SWT.RADIO);
        btnRadioCf1.setText("Crazyflie 1.0");
        
        btnRadioCf2 = new Button(container, SWT.RADIO);
        btnRadioCf2.setSelection(true);
        btnRadioCf2.setText("Crazyflie 2.0");
    }

    public String getCfType() {
        return btnRadioCf2.getSelection() ? CF2 : CF1;
    }
}
