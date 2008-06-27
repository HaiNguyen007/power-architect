/*
 * Copyright (c) 2008, SQL Power Group Inc.
 *
 * This file is part of Power*Architect.
 *
 * Power*Architect is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Power*Architect is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */

package ca.sqlpower.architect.swingui;

import java.awt.BorderLayout;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.log4j.Logger;

import ca.sqlpower.swingui.DataEntryPanel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * The panel for the show/hide column panels
 */
public class ShowColumnsPanel extends JPanel
    implements DataEntryPanel {

    private static final Logger logger = Logger.getLogger(ShowColumnsPanel.class);

    private JCheckBox showPrimary;
    private JCheckBox showForeign;
    private JCheckBox showUnique;
    private JCheckBox showIndexed;
    private JCheckBox showTheRest;
    
    private final ArchitectSwingSession session;
    
    public ShowColumnsPanel(ArchitectSwingSession session) {
        super(new BorderLayout(12,12));
        this.session = session;
        logger.debug("ShowColumnsPanel called"); //$NON-NLS-1$
        buildUI();
    }
    
    private void buildUI() {
        
        CellConstraints cc = new CellConstraints();
        
        FormLayout outerLayout = new FormLayout("pref", "pref,pref"); //$NON-NLS-1$ //$NON-NLS-2$
        JPanel outerPanel = new JPanel(outerLayout);
        PanelBuilder outerpb = new PanelBuilder(outerLayout, outerPanel);
        
        outerpb.add(new JLabel(Messages.getString("ShowColumnsPanel.showOnlyColumnsWhichAre")), cc.xy(1, 1)); //$NON-NLS-1$
        FormLayout innerLayout = new FormLayout("20dlu, pref", //$NON-NLS-1$
                "4dlu,pref,2dlu,pref,2dlu,pref,2dlu,pref,2dlu,pref,8dlu"); //$NON-NLS-1$
        JPanel innerPanel = new JPanel(innerLayout);
        PanelBuilder innerpb = new PanelBuilder(innerLayout, innerPanel);
        
        innerpb.add(showPrimary = new JCheckBox(Messages.getString("ShowColumnsPanel.primaryKeysOption")), cc.xy(2, 2)); //$NON-NLS-1$
        innerpb.add(showForeign = new JCheckBox(Messages.getString("ShowColumnsPanel.foreignKeysOption")), cc.xy(2, 4)); //$NON-NLS-1$
        innerpb.add(showUnique = new JCheckBox(Messages.getString("ShowColumnsPanel.uniqueOption")), cc.xy(2, 6)); //$NON-NLS-1$
        innerpb.add(showIndexed = new JCheckBox(Messages.getString("ShowColumnsPanel.indexedOption")), cc.xy(2, 8)); //$NON-NLS-1$
        innerpb.add(showTheRest = new JCheckBox(Messages.getString("ShowColumnsPanel.noneOfTheAboveOption")), cc.xy(2, 10)); //$NON-NLS-1$
        
        outerpb.add(innerPanel, cc.xy(1, 2));
        
        showPrimary.setSelected(session.getPlayPen().isShowPrimary());
        showForeign.setSelected(session.getPlayPen().isShowForeign());
        showIndexed.setSelected(session.getPlayPen().isShowIndexed());
        showUnique.setSelected(session.getPlayPen().isShowUnique());
        showTheRest.setSelected(session.getPlayPen().isShowTheRest());
        
        add(outerPanel, BorderLayout.CENTER);
    }


    public boolean applyChanges() {
        
        session.getPlayPen().setShowPrimary(showPrimary.isSelected());
        session.getPlayPen().setShowForeign(showForeign.isSelected());
        session.getPlayPen().setShowIndexed(showIndexed.isSelected());
        session.getPlayPen().setShowUnique(showUnique.isSelected());
        session.getPlayPen().setShowTheRest(showTheRest.isSelected());
        
        // Refresh the playPen to hide/show columns
        session.getPlayPen().updateHiddenColumns();

        return true;
    }

    /**
     * Does nothing.
     */
    public void discardChanges() {
    }

    public JComponent getPanel() {
        return this;
    }

    /**
     * Always returns false.
     */
    public boolean hasUnsavedChanges() {
        return false;
    }
}