/*
 * Copyright (c) 2007, SQL Power Group Inc.
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in
 *       the documentation and/or other materials provided with the
 *       distribution.
 *     * Neither the name of SQL Power Group Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package ca.sqlpower.architect.swingui;

import java.awt.BorderLayout;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.apache.log4j.Logger;

import ca.sqlpower.architect.ArchitectException;
import ca.sqlpower.architect.SQLRelationship;
import ca.sqlpower.architect.undo.UndoCompoundEvent;
import ca.sqlpower.architect.undo.UndoCompoundEventListener;
import ca.sqlpower.architect.undo.UndoCompoundEvent.EventTypes;
import ca.sqlpower.swingui.DataEntryPanel;

public class RelationshipEditPanel extends JPanel
	implements DataEntryPanel {

	private static final Logger logger = Logger.getLogger(RelationshipEditPanel.class);

	protected SQLRelationship relationship;

	protected JTextField relationshipName;

	protected ButtonGroup identifyingGroup;
	protected JRadioButton identifyingButton;
	protected JRadioButton nonIdentifyingButton;

	protected JLabel pkTableName;
	protected ButtonGroup pkTypeGroup;
	protected JRadioButton pkTypeZeroToMany;
	protected JRadioButton pkTypeOneToMany;
	protected JRadioButton pkTypeZeroOne;
	protected JRadioButton pkTypeOne;

	protected JLabel fkTableName;
	protected ButtonGroup fkTypeGroup;
	protected JRadioButton fkTypeZeroToMany;
	protected JRadioButton fkTypeOneToMany;
	protected JRadioButton fkTypeZeroOne;

	public RelationshipEditPanel(ArchitectSwingSession session) {
		super(new BorderLayout());
		addUndoEventListener(session.getArchitectFrame().getUndoManager().getEventAdapter());
		JPanel topPanel = new JPanel(new FormLayout());
		topPanel.add(new JLabel("Relationship Name"));
		topPanel.add(relationshipName = new JTextField());
		topPanel.add(new JLabel("Relationship Type"));
		JPanel typePanel = new JPanel();

		identifyingGroup = new ButtonGroup();
		typePanel.add(identifyingButton = new JRadioButton("Identifying"));
		identifyingGroup.add(identifyingButton);
		typePanel.add(nonIdentifyingButton = new JRadioButton("Non-Identifying"));
		identifyingGroup.add(nonIdentifyingButton);
		topPanel.add(typePanel);
		add(topPanel, BorderLayout.NORTH);
		
		JPanel pkPanel = new JPanel(new FormLayout());
		pkPanel.setBorder(new TitledBorder("Primary Key End"));
		pkPanel.add(new JLabel("PK Table"));
		pkPanel.add(pkTableName = new JLabel("Unknown"));
		pkTypeGroup = new ButtonGroup();
		pkPanel.add(new JLabel(""));
		pkPanel.add(pkTypeZeroToMany = new JRadioButton("Zero or More"));
		pkTypeGroup.add(pkTypeZeroToMany);
		pkPanel.add(new JLabel("Cardinality"));
		pkPanel.add(pkTypeOneToMany = new JRadioButton("One or More"));
		pkTypeGroup.add(pkTypeOneToMany);
		pkPanel.add(new JLabel(""));
		pkPanel.add(pkTypeZeroOne = new JRadioButton("Zero or One"));
		pkTypeGroup.add(pkTypeZeroOne);
		pkPanel.add(new JLabel(""));
		pkPanel.add(pkTypeOne = new JRadioButton("Exactly One"));
		pkTypeGroup.add(pkTypeOne);
		add(pkPanel, BorderLayout.WEST);

		JPanel fkPanel = new JPanel(new FormLayout());
		fkPanel.setBorder(new TitledBorder("Foreign Key End"));
		fkPanel.add(new JLabel("FK Table"));
		fkPanel.add(fkTableName = new JLabel("Unknown"));
		fkTypeGroup = new ButtonGroup();
		fkPanel.add(new JLabel(""));
		fkPanel.add(fkTypeZeroToMany = new JRadioButton("Zero or More"));
		fkTypeGroup.add(fkTypeZeroToMany);
		fkPanel.add(new JLabel("Cardinality"));
		fkPanel.add(fkTypeOneToMany = new JRadioButton("One or More"));
		fkTypeGroup.add(fkTypeOneToMany);
		fkPanel.add(new JLabel(""));
		fkPanel.add(fkTypeZeroOne = new JRadioButton("Zero or One"));
		fkTypeGroup.add(fkTypeZeroOne);
		add(fkPanel, BorderLayout.EAST);		
		relationshipName.selectAll();
	}


	public void setRelationship(SQLRelationship r) {
		this.relationship = r;
		relationshipName.setText(r.getName());
		pkTableName.setText(relationship.getPkTable().getName());
		fkTableName.setText(relationship.getFkTable().getName());
		if ( r.isIdentifying()){
			identifyingButton.setSelected(true);
		} else {
			nonIdentifyingButton.setSelected(true);
		}
		int pkc = r.getPkCardinality();
		if (pkc == (SQLRelationship.ZERO | SQLRelationship.ONE | SQLRelationship.MANY)){
			pkTypeZeroToMany.setSelected(true);
		} else if (pkc == (SQLRelationship.ZERO | SQLRelationship.ONE)){
			pkTypeZeroOne.setSelected(true);
		} else if (pkc == (SQLRelationship.ONE | SQLRelationship.MANY)){
			pkTypeOneToMany.setSelected(true);
		} else if (pkc == SQLRelationship.ONE) {
			pkTypeOne.setSelected(true);
		}
		int fkc = r.getFkCardinality();
		if (fkc == (SQLRelationship.ZERO | SQLRelationship.ONE | SQLRelationship.MANY)){
			fkTypeZeroToMany.setSelected(true);
		} else if (fkc == (SQLRelationship.ZERO | SQLRelationship.ONE)){
			fkTypeZeroOne.setSelected(true);
		} else if (fkc == (SQLRelationship.ONE | SQLRelationship.MANY)){
			fkTypeOneToMany.setSelected(true);
		}
		relationshipName.selectAll();
	}

	// ------------------ ARCHITECT PANEL INTERFACE ---------------------
	
	public boolean applyChanges() {
		startCompoundEdit("Relationship Properties Change");
		try {
			relationship.setName(relationshipName.getText());
			try {
				relationship.setIdentifying(identifyingButton.isSelected());
			} catch (ArchitectException ex) {
				logger.warn("Call to setIdentifying failed. Continuing with other properties.", ex);
			}
			
			if (pkTypeZeroOne.isSelected()) {
				relationship.setPkCardinality(SQLRelationship.ZERO | SQLRelationship.ONE);
			} else if (pkTypeZeroToMany.isSelected()) {
				relationship.setPkCardinality(SQLRelationship.ZERO | SQLRelationship.ONE | SQLRelationship.MANY);
			} else if (pkTypeOneToMany.isSelected()) {
				relationship.setPkCardinality(SQLRelationship.ONE | SQLRelationship.MANY);
			} else if (pkTypeOne.isSelected()) {
				relationship.setPkCardinality(SQLRelationship.ONE);
			}
			
			if (fkTypeZeroOne.isSelected()) {
				relationship.setFkCardinality(SQLRelationship.ZERO | SQLRelationship.ONE);
			} else if (fkTypeZeroToMany.isSelected()) {
				relationship.setFkCardinality(SQLRelationship.ZERO | SQLRelationship.ONE | SQLRelationship.MANY);
			} else if (fkTypeOneToMany.isSelected()) {
				relationship.setFkCardinality(SQLRelationship.ONE | SQLRelationship.MANY);
			}
		} finally {
			endCompoundEdit("Ending new compound edit event in relationship edit panel");
		}
		return true;
	}

	public void discardChanges() {
	  
	}
	
	/**
	 * The list of SQLObject property change event listeners
	 * used for undo
	 */
	protected LinkedList<UndoCompoundEventListener> undoEventListeners = new LinkedList<UndoCompoundEventListener>();

	
	public void addUndoEventListener(UndoCompoundEventListener l) {
		undoEventListeners.add(l);
	}

	public void removeUndoEventListener(UndoCompoundEventListener l) {
		undoEventListeners.remove(l);
	}
	
	protected void fireUndoCompoundEvent(UndoCompoundEvent e) {
		Iterator it = undoEventListeners.iterator();
		
		
		if (e.getType().isStartEvent()) {
			while (it.hasNext()) {
				((UndoCompoundEventListener) it.next()).compoundEditStart(e);
			}
		} else {
			while (it.hasNext()) {
				((UndoCompoundEventListener) it.next()).compoundEditEnd(e);
			}
		} 
		
	}
	
	public void startCompoundEdit(String message){
		fireUndoCompoundEvent(new UndoCompoundEvent(this,EventTypes.COMPOUND_EDIT_START,message));
	}
	
	public void endCompoundEdit(String message){
		fireUndoCompoundEvent(new UndoCompoundEvent(this,EventTypes.COMPOUND_EDIT_END,message));
	}

	public JPanel getPanel() {
		return this;
	}
	
}