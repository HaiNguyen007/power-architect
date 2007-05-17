package ca.sqlpower.architect.swingui;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.log4j.Logger;

public class ProjectSettingsPanel extends JPanel implements ArchitectPanel {
    private static final Logger logger = Logger.getLogger(ProjectSettingsPanel.class);
	/**
	 * The project whose settings we're editting.
	 */
	private SwingUIProject proj;

	private JCheckBox saveEntireSource;
    private JCheckBox clearProfile;
    private JTextField numberOfFreqValue;

	public ProjectSettingsPanel(SwingUIProject proj) {
		this.proj = proj;
		setup();
		revertToProjectSettings();
	}

	public void setup() {
		setLayout(new FormLayout());
		add(new JLabel("Snapshot entire source database in project file?"));
		add(saveEntireSource = new JCheckBox());

        add(new JLabel("Don't save Profile results in the project?"));
        add(clearProfile=new JCheckBox());
        clearProfile.setToolTipText(
               "Warning: this only removes current profiles");

        add(new JLabel("Number of Most Frequent Value in Profile:"));
        add( numberOfFreqValue=new JTextField(String.valueOf(proj.getProfileManager().getProfileSettings().getTopNCount()),6));
	}

	protected void revertToProjectSettings() {
        logger.debug("Reverting project options");
		saveEntireSource.setSelected(proj.isSavingEntireSource());
	}

	public boolean applyChanges() {
        logger.debug("Setting snapshot option to:"+saveEntireSource.isSelected());
		proj.setSavingEntireSource(saveEntireSource.isSelected());
        logger.debug("Project "+proj.getName() +" snapshot option is:"+proj.isSavingEntireSource());

        // This is a mistake! This is an action, not a project setting.
        // It should be changed to a setting, and the Save code changed
        // to honor it.
        if ( clearProfile.isSelected() ) {
            proj.getProfileManager().clear();
        }

        if ( numberOfFreqValue.getText().length() > 0 ) {
            try {
                proj.getProfileManager().getProfileSettings().setTopNCount(Integer.valueOf(numberOfFreqValue.getText()));
            } catch ( NumberFormatException e ) {
                ASUtils.showExceptionDialogNoReport(this,
                        "Number Format Error", e);
            }
        }
		return true;
	}

	public void discardChanges() {

	}

	public JPanel getPanel() {
		return this;
	}

}
