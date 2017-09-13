package se.bitcraze.crazyflie.ect.bootloader.wizard;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;

public class BootloaderWizard extends Wizard {

    private CfTypeWizardPage cfTypePage;
    private FirmwareWizardPage fwPage;
    private BootloaderWizardPage bootloaderPage;

    public BootloaderWizard() {
        super();
        setNeedsProgressMonitor(true);
        setWindowTitle("Crazyflie Bootloader Wizard");
    }

    public void addPages() {
        cfTypePage = new CfTypeWizardPage();
        fwPage = new FirmwareWizardPage();
        bootloaderPage = new BootloaderWizardPage();
        addPage(cfTypePage);
        addPage(fwPage);
        addPage(bootloaderPage);
    }

    @Override
    public boolean performFinish() {
        IRunnableWithProgress op = new IRunnableWithProgress() {
            public void run(IProgressMonitor monitor) throws InvocationTargetException {
                try {
                    doFinish(monitor);
                } finally {
                    monitor.done();
                }
            }
        };
        MessageDialog.openInformation(getShell(), "Bootloader", "CF type? " + cfTypePage.getCfType() + "\nIs Custom FW? " + fwPage.isCustomFirmware() + "\n");
        try {
            getContainer().run(true, false, op);
        } catch (InterruptedException e) {
            return false;
        } catch (InvocationTargetException e) {
            Throwable realException = e.getTargetException();
            MessageDialog.openError(getShell(), "Error", realException.getMessage());
            return false;
        }
        return true;
    }

    private void doFinish(IProgressMonitor monitor) {
        // create a sample file
        monitor.beginTask("Starting bootloader", 2);
        monitor.worked(1);
        monitor.setTaskName("BlaBlaBla");
        monitor.worked(1);
    }

    public boolean canFinish() {
        if(getContainer().getCurrentPage() == cfTypePage || getContainer().getCurrentPage() == fwPage) {
            return false;
        }
        return true;
    }
}
