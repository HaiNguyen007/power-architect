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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import org.apache.log4j.Logger;

import ca.sqlpower.architect.ArchitectException;
import ca.sqlpower.architect.SQLCatalog;
import ca.sqlpower.architect.SQLDatabase;
import ca.sqlpower.architect.SQLObject;
import ca.sqlpower.architect.SQLSchema;
import ca.sqlpower.architect.SQLTable;
import ca.sqlpower.architect.ddl.DDLGenerator;
import ca.sqlpower.architect.ddl.DDLUtils;
import ca.sqlpower.architect.diff.CompareSQL;
import ca.sqlpower.architect.diff.DiffChunk;
import ca.sqlpower.architect.swingui.CompareDMPanel.SourceOrTargetStuff.CatalogPopulator;
import ca.sqlpower.architect.swingui.CompareDMPanel.SourceOrTargetStuff.SchemaPopulator;
import ca.sqlpower.architect.swingui.CompareDMSettings.DatastoreType;
import ca.sqlpower.architect.swingui.CompareDMSettings.SourceOrTargetSettings;
import ca.sqlpower.sql.DataSourceCollection;
import ca.sqlpower.sql.SPDataSource;
import ca.sqlpower.swingui.ConnectionComboBoxModel;
import ca.sqlpower.swingui.MonitorableWorker;
import ca.sqlpower.swingui.ProgressWatcher;
import ca.sqlpower.swingui.SPSUtils;
import ca.sqlpower.swingui.SPSwingWorker;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.debug.FormDebugPanel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * The user interface for setting up the comparison between two databases,
 * whether they are housed in Architect project files, SQL databases, or
 * just the current project in the session.
 * <p>
 * This class should not actually extend JPanel.. it should have a JPanel
 * member instead.
 */
public class CompareDMPanel extends JPanel {

	/**
	 * This listener helps with restoring the selected catalog and schema from the user
	 * settings.
	 */
	private static class RestoreSettingsListener implements ListDataListener {

		private JComboBox box;
		private String selectItemName;

		public RestoreSettingsListener(JComboBox box, String selectItemName) {
			this.box = box;
			this.selectItemName = selectItemName;
		}

		public void intervalAdded(ListDataEvent e) {
			tryToSelectTheItem(e.getIndex0(), e.getIndex1());
		}

		public void intervalRemoved(ListDataEvent e) {
			// don't care
		}

		public void contentsChanged(ListDataEvent e) {
			tryToSelectTheItem(e.getIndex0(), e.getIndex1());
		}

		/**
		 * Searches the combo box list data from index low to high (inclusive) and selects
		 * the first item it finds whose name matches selectItemName.  If a match
		 * if found and selected, this listener is also removed from the list data
		 * listener list (because it's no longer needed).
		 *
		 * @param low The index to start the search at
		 * @param high One past the index to end the search at
		 */
		private void tryToSelectTheItem(int low, int high) {
			if (logger.isDebugEnabled()) {
				logger.debug("Looking for '"+selectItemName+"' from index "+low+" to "+high);
			}

			for (int i = low; i <= high; i++) {
				SQLObject o = (SQLObject) box.getItemAt(i);
				if (o != null && o.getName().equals(selectItemName)) {
					box.setSelectedIndex(i);
					box.getModel().removeListDataListener(this);
					return;
				}
			}
		}
	}

	private static final Logger logger = Logger.getLogger(CompareDMPanel.class);

	private static final String OUTPUT_ENGLISH = "OUTPUT_ENGLISH";

	private static final String OUTPUT_SQL = "OUTPUT_SQL";

	public static final String DBCS_DIALOG_TITLE = "New Database Connection";

	private JProgressBar progressBar;

	private JPanel buttonPanel;

    /**
     * The list of all DDL Generators available.  The items stored in this
     * combo box are of type <tt>Class&lt;? extends DDLGenerator&gt;</tt>.
     */
	private JComboBox sqlTypeDropdown;

	private JRadioButton sqlButton;

	private JRadioButton englishButton;
    
    private JCheckBox showNoChanges;

	private JLabel statusLabel;

	private StartCompareAction startCompareAction;
	
	private SwapSourceTargetAction swapSourceTargetAction;

	private SourceOrTargetStuff source = new SourceOrTargetStuff();
	
	private SourceOrTargetStuff target = new SourceOrTargetStuff();
	
    /**
     * Since we can create new DB connections from this panel, we need a reference
     * to the session so we can retrieve the datasource collection.
     */
    private ArchitectSwingSession session;
    
	/**
	 * Contains all of the properties and GUI components that relate to the
	 * source or target system. The idea is, the panel will have two instances
	 * of this class: One for the "source" system, and the other for the
	 * "target" system.
	 *
	 * <p>
	 * Note: this class is not private because the test needs to refer to it. :(
	 */
	public class SourceOrTargetStuff {

		private JComboBox databaseDropdown;

		private JComboBox catalogDropdown;

		private JComboBox schemaDropdown;

		private JButton newConnButton;

		private JButton loadFileButton;

		private JTextField loadFilePath;

		/** The group for the source/target type (playpen, file, or database) */
		private ButtonGroup buttonGroup = new ButtonGroup();

		private JRadioButton playPenRadio;

		private JRadioButton physicalRadio;

		private JRadioButton loadRadio;

		private JDialog newConnectionDialog;

		private JLabel catalogLabel;
		private JLabel schemaLabel;
		
		private SchemaPopulator schemaPop;
		private CatalogPopulator catalogPop;
		
		private boolean isSource;

		/**
		 * The last database returned by getDatabase(). Never access this
		 * directly; always use getDatabase().
		 */
		private SQLDatabase cachedDatabase;

		private Action newConnectionAction = new AbstractAction("New...") {
			public void actionPerformed(ActionEvent e) {

                final DataSourceCollection plDotIni = session.getContext().getPlDotIni();
                final SPDataSource dataSource = new SPDataSource(plDotIni);
                Runnable onAccept = new Runnable() {
                    public void run() {
                        plDotIni.addDataSource(dataSource);
                        databaseDropdown.setSelectedItem(dataSource);
                    }
                };
                ASUtils.showDbcsDialog(SPSUtils.getWindowInHierarchy(CompareDMPanel.this), dataSource, onAccept);
			}
		};

		private Action chooseFileAction = new AbstractAction("Choose...") {
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser(session.getRecentMenu().getMostRecentFile());
				chooser.addChoosableFileFilter(SPSUtils.ARCHITECT_FILE_FILTER);
				int returnVal = chooser.showOpenDialog(CompareDMPanel.this);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					final File file = chooser.getSelectedFile();
					loadFilePath.setText(file.getPath());
				}
			}
		};

		/**
		 * Finds all the children of a database and puts them in the GUI.
		 */
		public class CatalogPopulator extends PopulateProgressMonitorableWorker implements
				ActionListener {
		    
		    String catalogSelect;
		    String schemaSelect;
		    
		    private SQLDatabase db;
		    
            public CatalogPopulator(ArchitectSwingSession session) {
                super(session);
                catalogSelect = null;
                schemaSelect = null;
            }
            
            /**
             * If select is a valid catalog of the database this sets a 
             * string that will be used as a default item to select 
             * for the catalog next time the list will be redrawn, (this 
             * will happen only once).
             * 
             *  Else does nothing
             *  
             * @param select The name of the item to select
             */
            public void setDefaultCatalog(String def) {
                this.catalogSelect = def;
            }
            
            /**
             * If select is a valid schema of the database and there is 
             * no catalogs for this database this sets a 
             * string that will be used as a default item to select 
             * for the schema next time the list will be redrawn, (this 
             * will happen only once).
             *  
             *  Else does nothing
             *  
             * @param select The name of the item to select
             */
            public void setDefaultSchema(String def) {
                this.schemaSelect = def;
            }
            
			/**
			 * Checks the datasource selected in the databaseDropdown, and
			 * starts a worker thread to read its contents if it exists.
			 *
			 * <p>
			 * Otherwise, clears out the catalog and schema dropdowns and does
			 * not start a worker thread.
			 */
			public void actionPerformed(ActionEvent e) {
				startCompareAction.setEnabled(false);
				db = getDatabase();
				if (db != null) {
					// disable start button (listers will reenable it when
					// finished)
					if (((JComboBox) (e.getSource())).getSelectedIndex() == 0) {
						startCompareAction.setEnabled(false);
					}
					new Thread(this).start();

				} else {
					catalogDropdown.removeAllItems();
					catalogDropdown.setEnabled(false);

					schemaDropdown.removeAllItems();
					schemaDropdown.setEnabled(false);
				}
			}

			/**
			 * Populates the database <tt>db</tt> which got set up in
			 * actionPerformed().
			 */
			@Override
			public void doStuff() throws Exception {

				try {
					ProgressWatcher.watchProgress(progressBar, this);

					started = true;
					db.populate();

				} catch (ArchitectException e) {
					logger.debug(
						"Unexpected architect exception in ConnectionListener",	e);
                    ASUtils.showExceptionDialogNoReport(CompareDMPanel.this,
                            "Unexpected architect exception in ConnectionListener", e);
				}
			}

			
			/**
			 * Does GUI cleanup work on the Swing EDT once the worker is done.
			 *
			 * <p>
			 * This work involves:
			 * <ul>
			 * <li>Check which child type the database has
			 * <li>Populate the catalog and schema boxes accordingly
			 * <li>Enable or disable the catalog and schema boxes accordingly
			 * </ul>
			 */
			@Override
			public void cleanup() throws ArchitectException {
			    setCleanupExceptionMessage("Could not populate catalog dropdown!");

				catalogDropdown.removeAllItems();
				catalogDropdown.setEnabled(false);
				catalogLabel.setText("");
				schemaLabel.setText("");

				// This is either a database, a catalog, or null depending on
				// how db is structured
				SQLObject schemaParent;

				if (db.isCatalogContainer()) {
					for (SQLObject item : (List<SQLObject>) db.getChildren()) {
						// Note: if you change the way this works, also update the RestoreSettingsListener
						catalogDropdown.addItem(item);
						// did you read the note?
					}

					// check if we need to do schemas
					SQLCatalog cat = (SQLCatalog) catalogDropdown
							.getSelectedItem();
					if ( cat != null && cat.getNativeTerm() !=null )
						catalogLabel.setText(cat.getNativeTerm());
					schemaParent = null;
					if (cat == null) {
						// there are no catalogs (database is completely empty)
						catalogDropdown.setEnabled(false);
					}  else {
						// there are catalogs, but they don't contain schemas
						catalogDropdown.setEnabled(true);
					}

				} else if (db.isSchemaContainer()) {
					schemaParent = db;
					catalogDropdown.setEnabled(false);
				} else {
					// database contains tables directly
					schemaParent = null;
					catalogDropdown.setEnabled(false);
				}

				schemaDropdown.removeAllItems();
				schemaDropdown.setEnabled(false);

				if (schemaParent == null) {
					startCompareAction.setEnabled(isStartable());
				} else {
					// need a final reference to this so we can use it in the
					// inner class
					// we only get here if the database is a schema container not
					// a catalog container.

					final SQLObject finalSchemaParent = schemaParent;

					new Thread(new PopulateProgressMonitorableWorker(session) {

						@Override
						public void doStuff() throws Exception {
							ProgressWatcher.watchProgress(progressBar, this);
							// this populates the schema parent (populate is not
							// visible here)
							started = true;
							finalSchemaParent.getChildren();
							finished = true;
						}

						/**
						 * Populates the schema dropdown box from the schema
						 * parent that doStuff() populated.
						 *
						 * @throws ArchitectException
						 */
						@Override
						public void cleanup() throws ArchitectException {
							setCleanupExceptionMessage("Could not populate schema dropdown!");

							for (SQLObject item : (List<SQLObject>) finalSchemaParent
									.getChildren()) {
								schemaDropdown.addItem(item);
							}

							if (schemaDropdown.getItemCount() > 0) {
								schemaDropdown.setEnabled(true);
								if ( ((SQLSchema)(finalSchemaParent.getChild(0))).getNativeTerm() != null )
									schemaLabel.setText(((SQLSchema)
											(finalSchemaParent.getChild(0))).getNativeTerm());
							}

							startCompareAction.setEnabled(isStartable());
							
				             //sets to the default schema, iff catalog is null
			                logger.debug("default schema is: " + schemaSelect);
			                if ( CatalogPopulator.this.schemaSelect != null) {      
			                    for (int x = 0; x < schemaDropdown.getItemCount(); x++) {
			                        SQLObject curr = (SQLObject)(schemaDropdown.getItemAt(x));
			                        if (curr != null && curr.getName().equals(schemaSelect)) {
			                            schemaDropdown.setSelectedIndex(x);
			                            break;
			                        }
			                    }
			                }
			                schemaSelect = null;
							
							
						}
					}).start();
				}
				
				//sets to the default catalog
				logger.debug("default catalog selected " + catalogSelect);
		        if (catalogSelect != null) {      
		            for (int x = 0; x < catalogDropdown.getItemCount(); x++) {
		                SQLObject curr = (SQLObject)(catalogDropdown.getItemAt(x));
		                if (curr != null && curr.getName().equals(catalogSelect)) {
		                    catalogDropdown.setSelectedIndex(x);
		                    break;
		                }
		            }
		        }
		        catalogSelect = null;
			}

			// Overriding isFinished to be based on the state of db.
            public boolean isFinished() {
                if (db != null) {
                    return db.isPopulated();
                }
                return true;
            }
		}

		/**
		 * Finds all the children of a catalog and puts them in the GUI.
		 */
		public class SchemaPopulator extends PopulateProgressMonitorableWorker implements
				ActionListener {
		    
		    String select;

			public SchemaPopulator(ArchitectSwingSession session) {
                super(session);
                select = null;
            }

			
            /**
             * If select is a valid schema of the catalog this sets a 
             * string that will be used as a default item to select 
             * for the schema next time the list will be redrawn, (this 
             * will happen only once).
             *  
             *  Else does nothing
             *  
             * @param select The name of the item to select
             */
            public void setDefaultSelect(String select) {
                this.select = select;
            }
			
			
            /**
			 * Clears the schema dropdown, and starts a worker thread to
			 * repopulate it (if possible).
			 */
			public void actionPerformed(ActionEvent e) {
				logger.debug("SCHEMA POPULATOR IS ABOUT TO START...");
				schemaDropdown.removeAllItems();
				schemaDropdown.setEnabled(false);

				SQLCatalog catToPopulate = (SQLCatalog) catalogDropdown
						.getSelectedItem();

				if (catToPopulate != null) {
					startCompareAction.setEnabled(false);
					Thread t = new Thread(this);
					t.start();
				}
			}

			@Override
			public void doStuff() throws ArchitectException {
				logger.debug("SCHEMA POPULATOR IS STARTED...");
				ProgressWatcher.watchProgress(progressBar, this);
				started = true;
				SQLCatalog catToPopulate = (SQLCatalog) catalogDropdown
						.getSelectedItem();
				catToPopulate.getChildren(); // this might take a while
				finished = true;
			}

			/**
			 * Examines the newly-populated catalog and adds its schemas to the
			 * GUI. If the catalog doesn't contain schemas, cleanup just checks
			 * if the comparison action is startable.
			 *
			 * @throws ArchitectException
			 */
			@Override
			public void cleanup() throws ArchitectException {
			    logger.debug("SCHEMA POPULATOR IS ABOUT TO CLEAN UP...");
				schemaLabel.setText("");
				SQLCatalog populatedCat = (SQLCatalog) catalogDropdown
						.getSelectedItem();

				if (populatedCat.isSchemaContainer()) {
					for (SQLObject item : (List<SQLObject>) populatedCat
							.getChildren()) {
						schemaDropdown.addItem(item);
					}

					if (schemaDropdown.getItemCount() > 0) {
						schemaDropdown.setEnabled(true);
						if ( ((SQLSchema)(populatedCat.getChild(0))).getNativeTerm() != null )
							schemaLabel.setText(((SQLSchema)(populatedCat.getChild(0))).getNativeTerm());
					}
				}
				startCompareAction.setEnabled(isStartable());
				
				//sets the default schema
                logger.debug("Default Schema: " + select);
                if (select != null) {      
                    for (int x = 0; x < schemaDropdown.getItemCount(); x++) {
                        SQLObject curr = (SQLObject)(schemaDropdown.getItemAt(x));
                        if (curr != null && curr.getName().equals(select)) {
                            schemaDropdown.setSelectedIndex(x);
                            break;
                        }
                    }
                }
                select = null;
			}
			
			
		}
		    
	    // -------------- Small class for monitoring populate progress -----------------
	    // TODO Document this class!!!!
	    private abstract class PopulateProgressMonitorableWorker extends MonitorableWorker {
	        
	        boolean started = false;
	        boolean finished = false;
	        
	        public PopulateProgressMonitorableWorker(ArchitectSwingSession session) {
                super(session);
            }
	        	        
	        /**
	         * Returns null, which will keep the monitor in indeterminate mode.
	         */
	        public Integer getJobSize() {
	            return null;
	        }
	        
	        /**
	         * Cannot measure progress of connecting to the database, so always return 0
	         */
	        public int getProgress() {
	            return 0;
	        }
	        
	        public boolean isFinished() {
	            return finished;
	        }

	        public String getMessage() {
	            return "Connecting to database...";
	        }

	        public boolean hasStarted() {
	            return started;
	        }
	    }
		
		public synchronized JDialog getNewConnectionDialog() {
			return newConnectionDialog;
		}

		/**
		 * Creates the GUI components associated with this object, and appends
		 * them to the given builder.
		 */
		private void buildPartialUI(DefaultFormBuilder builder,	boolean defaultPlayPen, SchemaPopulator schemaPop, CatalogPopulator catalogPop) {
			String prefix;
			if (defaultPlayPen == true) {
				prefix = "source";
			} else {
				prefix = "target";
			}
			
			this.isSource = defaultPlayPen;
			
			this.schemaPop = schemaPop;
			this.catalogPop = catalogPop;
			
			CellConstraints cc = new CellConstraints();
			
			playPenRadio = new JRadioButton();
			playPenRadio.setName(prefix + "PlayPenRadio");
			physicalRadio = new JRadioButton();
			physicalRadio.setName(prefix + "PhysicalRadio");
			loadRadio = new JRadioButton();
			loadRadio.setName(prefix + "LoadRadio");

			buttonGroup.add(playPenRadio);
			buttonGroup.add(physicalRadio);
			buttonGroup.add(loadRadio);

			schemaDropdown = new JComboBox();
			schemaDropdown.setEnabled(false);
			schemaDropdown.setName(prefix + "SchemaDropdown");

			catalogDropdown = new JComboBox();
			catalogDropdown.setEnabled(false);
			catalogDropdown.setName(prefix + "CatalogDropdown");

			databaseDropdown = new JComboBox();
			databaseDropdown.setName(prefix + "DatabaseDropdown");
			databaseDropdown.setModel(new ConnectionComboBoxModel(session.getContext().getPlDotIni()));
			databaseDropdown.setEnabled(false);
			databaseDropdown.setRenderer(dataSourceRenderer);

			newConnButton = new JButton();
			newConnButton.setName(prefix + "NewConnButton");
			newConnButton.setAction(newConnectionAction);
			newConnectionAction.setEnabled(false);

			loadFilePath = new JTextField();
			loadFilePath.setName(prefix + "LoadFilePath");

			loadFilePath.setEnabled(false);
			loadFilePath.getDocument().addDocumentListener(
					new DocumentListener() {
						public void insertUpdate(DocumentEvent e) {
							startCompareAction.setEnabled(isStartable());
						}

						public void removeUpdate(DocumentEvent e) {
							startCompareAction.setEnabled(isStartable());
						}

						public void changedUpdate(DocumentEvent e) {
							startCompareAction.setEnabled(isStartable());
						}
					});
			loadFileButton = new JButton();
			loadFileButton.setName(prefix + "LoadFileButton");
			loadFileButton.setAction(chooseFileAction);
			chooseFileAction.setEnabled(false);

            catalogDropdown.addActionListener(schemaPop);
            databaseDropdown.addActionListener(catalogPop);
            databaseDropdown.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    if (!isSource) {
                        return;
                    }
                    SPDataSource dataSource = (SPDataSource)((JComboBox)e.getSource()).getSelectedItem();  
                    if (dataSource != null) {
                        try {
                            sqlTypeDropdown.setSelectedItem(Class.forName(dataSource.getParentType().getDDLGeneratorClass()));
                        } catch (ClassNotFoundException e1) {
                            logger.error("Error when finding the DDLGenerator class for the selected database!", e1);
                        }
                    }
                }
                
            });
            
			ActionListener listener = new OptionGroupListener();
			playPenRadio.addActionListener(listener);
			physicalRadio.addActionListener(listener);
			loadRadio.addActionListener(listener);

			if (defaultPlayPen) {
				playPenRadio.doClick();
			} else {
				physicalRadio.doClick();
			}

			// now give all our shiny new components to the builder
			builder.append(playPenRadio);
			builder.append("Current Project [" + session.getName() + "]");
			builder.nextLine();

			builder.append(""); // takes up blank space
			builder.append(physicalRadio);
			builder.append("Physical Database");
			// builder.nextColumn(2);
			builder.append(catalogLabel = new JLabel("Catalog"));
			builder.append(schemaLabel = new JLabel("Schema"));
			builder.appendRow(builder.getLineGapSpec());
			builder.appendRow("pref");
			builder.nextLine(2);
			builder.nextColumn(4);
			builder.append(databaseDropdown);
			builder.append(catalogDropdown, schemaDropdown, newConnButton);
			builder.nextLine();

			builder.append("");
			builder.append(loadRadio);
			builder.append("From File:");
			builder.nextLine();
			builder.append(""); // takes up blank space
			builder.add(loadFilePath, cc.xyw(5, builder.getRow(), 5));
			builder.nextColumn(8);
			builder.append(loadFileButton);
			builder.nextLine();

		}

		/**
		 * Figures out which SQLObject holds the tables we want to compare, and
		 * returns it.
		 *
		 * @throws ArchitectException
		 * @throws IOException
		 * @throws IOException
		 */
		public SQLObject getObjectToCompare() throws ArchitectException,
				IOException {
			SQLObject o;
			if (playPenRadio.isSelected()) {
				o = session.getTargetDatabase();
			} else if (physicalRadio.isSelected()) {
				if (schemaDropdown.getSelectedItem() != null) {
					o = (SQLObject) schemaDropdown.getSelectedItem();
				} else if (catalogDropdown.getSelectedItem() != null) {
					o = (SQLObject) catalogDropdown.getSelectedItem();
				} else if (databaseDropdown.getSelectedItem() != null) {
					o = getDatabase();
				} else {
					throw new IllegalStateException(
							"You elected to compare a physical database, "
									+ "but have not selected a "
									+ "schema, catalog, or database to compare.");
				}

			} else if (loadRadio.isSelected()) {
				File f = new File(loadFilePath.getText());
				InputStream in = new BufferedInputStream(new FileInputStream(f));
                
                // XXX: this will take a non-trivial amount of time, so ideally would be done with a progress bar.
                // we might be able to use OpenProjectAction.loadAsynchronously() for this, but it would need a flag for not showing the GUI
                // or better yet, set o=f, and do the load itself in the compare worker, because this approach would share the progress bar with the comparison activity itself
				ArchitectSwingSession newSession = session.getContext().createSession(in, false);
				
                o = newSession.getTargetDatabase();
                
			} else {
				throw new IllegalStateException(
						"Do not know which source to compare from");
			}

			return o;
		}

		/**
		 * The public isStartable() method uses this to check source and target
		 * readiness.
		 *
		 * XXX: this is really similar to the getObjectToCompare() method,
		 * except that it doesn't try to load the file (so it runs quicker)
		 */
		private boolean isThisPartStartable() {
			if (playPenRadio.isSelected()) {
				return true;
			} else if (physicalRadio.isSelected()) {
				return databaseDropdown.getSelectedItem() != null;
			} else if (loadRadio.isSelected()) {
				return new File(loadFilePath.getText()).canRead();
			} else {
				throw new IllegalStateException(
						"None of the radio buttons are selected!");
			}
		}

		/**
		 * Returns the currently selected database. Only creates a new
		 * SQLDatabase instance if necessary.
		 */
		public synchronized SQLDatabase getDatabase() {
			SPDataSource ds = (SPDataSource) databaseDropdown
					.getSelectedItem();
			if (ds == null) {
				cachedDatabase = null;
			} else if (cachedDatabase == null
					|| !cachedDatabase.getDataSource().equals(ds)) {
				cachedDatabase = new SQLDatabase(ds);
			}
			return cachedDatabase;
		}

		/**
		 * This listener is used to enable/disable JComponents when one of the
		 * database choosing options is choosen (for both source and target
		 * selections).
		 */
		public class OptionGroupListener implements ActionListener {

			public void actionPerformed(ActionEvent e) {
				enableDisablePhysicalComps();

				boolean enableLoadComps = e.getSource() == loadRadio;
				loadFilePath.setEnabled(enableLoadComps);
				loadFileButton.setEnabled(enableLoadComps);
			}
		}

		/**
		 * For the special case of enabling and disabling the Physical database
		 * Dropdown Components.
		 */
		void enableDisablePhysicalComps() {
			boolean enable = physicalRadio.isSelected();

			databaseDropdown.setEnabled(enable);

			if (enable && catalogDropdown.getItemCount() > 0) {
				catalogDropdown.setEnabled(true);
			} else {
				catalogDropdown.setEnabled(false);
			}

			if (enable && schemaDropdown.getItemCount() > 0) {
				schemaDropdown.setEnabled(true);
			} else {
				schemaDropdown.setEnabled(false);
			}

			newConnectionAction.setEnabled(enable);
		}

        boolean isSource() {
            return isSource;
        }

	}

	/**
	 * Renders list cells which have a value that is an SPDataSource.
	 */
	private ListCellRenderer dataSourceRenderer = new DataSourceRenderer();

	/**
	 * Returns true iff the comparison process can start given the current state
	 * of the GUI form.
	 */
	public boolean isStartable() {
		logger.debug("isStartable is checking...");
		boolean startable = true;
		if (sqlButton.isSelected()) {
		    startable = source.physicalRadio.isSelected() && sqlTypeDropdown.getSelectedItem() != null;
		}
	    return source.isThisPartStartable() && target.isThisPartStartable() && startable;
	}

	public Action getStartCompareAction() {
		return startCompareAction;
	}
	
	public Action getSwapSourceTargetAction() {
	    return swapSourceTargetAction;
	}

	public JPanel getButtonPanel() {
		return buttonPanel;
	}

	public CompareDMPanel(ArchitectSwingSession session) {
        this.session = session;
		buildUI(target.new SchemaPopulator(session),target.new CatalogPopulator(session),
		        source.new SchemaPopulator(session),source.new CatalogPopulator(session));
	}
	
	

	private void buildUI(SchemaPopulator targetSchemaPop, CatalogPopulator targetCatalogPop, 
	        SchemaPopulator sourceSchemaPop, CatalogPopulator sourceCatalogPop) {

		progressBar = new JProgressBar();
		progressBar.setIndeterminate(true);
		progressBar.setVisible(false);

		sqlTypeDropdown = new JComboBox(DDLUtils.getDDLTypes(session.getContext().getPlDotIni()));
        sqlTypeDropdown.setRenderer(new DDLGeneratorListCellRenderer());
		sqlTypeDropdown.setName("sqlTypeDropDown");
		OutputChoiceListener listener = new OutputChoiceListener(sqlTypeDropdown);
        sqlTypeDropdown.setEnabled(false);
		sqlButton = new JRadioButton();
		sqlButton.setName(OUTPUT_SQL);
		sqlButton.setActionCommand(OUTPUT_SQL);
		sqlButton.setSelected(false);
		sqlButton.addActionListener(listener);

		englishButton = new JRadioButton();
		englishButton.setName("englishButton");
		englishButton.setActionCommand(OUTPUT_ENGLISH);
		englishButton.setSelected(true);
		englishButton.addActionListener(listener);
        
        showNoChanges = new JCheckBox();
        showNoChanges.setName("showNoChanges");
        showNoChanges.setSelected(false);

		// Group the radio buttons.
		ButtonGroup outputGroup = new ButtonGroup();
		outputGroup.add(sqlButton);
		outputGroup.add(englishButton);

		startCompareAction = new StartCompareAction();
		startCompareAction.setEnabled(false);
		
		swapSourceTargetAction = new SwapSourceTargetAction();
		swapSourceTargetAction.setEnabled(true);

		buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		FormLayout formLayout = new FormLayout("20dlu, 2dlu, pref, 4dlu," + // 1-4
				"0:grow, 4dlu, 0:grow, 4dlu," + // 5-8
				"0:grow, 4dlu, pref", // 9-11
				"");
		formLayout.setColumnGroups(new int[][] { { 5, 7, 9, } });
		JPanel panel = logger.isDebugEnabled() ? new FormDebugPanel()
				: new JPanel();
		DefaultFormBuilder builder = new DefaultFormBuilder(formLayout, panel);
		builder.setDefaultDialogBorder();

		CellConstraints cc = new CellConstraints();

		builder.appendSeparator("Compare Older");
		builder.nextLine();
		builder.append(""); // takes up blank space

		source.buildPartialUI(builder, true, sourceSchemaPop, sourceCatalogPop);

		builder.appendSeparator("With Newer");
		builder.appendRow(builder.getLineGapSpec());
		builder.appendRow("pref");
		builder.nextLine(2);
		builder.append("");

		target.buildPartialUI(builder, false, targetSchemaPop, targetCatalogPop);

		ActionListener radioButtonActionEnabler = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				startCompareAction.setEnabled(isStartable());
			}
		};
		source.playPenRadio.addActionListener(radioButtonActionEnabler);
		source.physicalRadio.addActionListener(radioButtonActionEnabler);
		source.loadRadio.addActionListener(radioButtonActionEnabler);

		target.playPenRadio.addActionListener(radioButtonActionEnabler);
		target.physicalRadio.addActionListener(radioButtonActionEnabler);
		target.loadRadio.addActionListener(radioButtonActionEnabler);

		builder.appendSeparator("Output Format");
		builder.appendRow(builder.getLineGapSpec());
		builder.appendRow("pref");
		builder.nextLine(2);
		builder.nextColumn(2);
		builder.append(sqlButton);

		JPanel ddlTypePanel = new JPanel(new BorderLayout(3, 3));
		ddlTypePanel.add(new JLabel("SQL for "), BorderLayout.WEST);
		ddlTypePanel.add(sqlTypeDropdown, BorderLayout.CENTER); // ddl generator
																// type list
        ddlTypePanel.add(new JLabel(" to make Older look like Newer"), BorderLayout.EAST);
		builder.append(ddlTypePanel, 3);

		builder.appendRow(builder.getLineGapSpec());
		builder.appendRow("pref");
		builder.nextLine(2);
		builder.nextColumn(2);
		builder.append(englishButton);
		builder.append("English descriptions");

        builder.appendRow(builder.getLineGapSpec());
        builder.appendRow("pref");
        builder.nextLine(2);
        builder.nextColumn(2);
		builder.append(showNoChanges);
        builder.append("Suppress similarities");
        builder.nextLine();

		builder.appendSeparator("Status");
		builder.appendRow(builder.getLineGapSpec());
		builder.appendRow("pref");
		builder.nextLine(2);
		statusLabel = new JLabel("");
		builder.add(statusLabel, cc.xy(5, builder.getRow()));
		builder.add(progressBar, cc.xyw(7, builder.getRow(), 5));

		setLayout(new BorderLayout());
		add(builder.getPanel());
		setPreferredSize(new Dimension(800,600));
		try {
			restoreSettingsFromProject();
		} catch (ArchitectException e) {
			logger.warn("Failed to save user CompareDM preferences!", e);
		}
	}


	/**
	 * Handles disabling and enabling the "DDL Type" dropdown box and 
     * the no-change suppression checkbox.
	 */
	public class OutputChoiceListener implements ActionListener {

		JComboBox cb;

		public OutputChoiceListener(JComboBox cb) {
			this.cb = cb;
		}

		public void actionPerformed(ActionEvent e) {
			if (e.getActionCommand().equals(OUTPUT_SQL)) {
				cb.setEnabled(true);
                showNoChanges.setEnabled(false);
			} else {
				cb.setEnabled(false);
                showNoChanges.setEnabled(true);
			}
			startCompareAction.setEnabled(isStartable());
		}

	}

	public class StartCompareAction extends AbstractAction   {

		private Collection<SQLTable> sourceTables;

		private Collection<SQLTable> targetTables;

		public StartCompareAction() {
			super("Start");
		}

		public void actionPerformed(ActionEvent e) {
			startCompareAction.setEnabled(false);
			sqlButton.setEnabled(false);
			englishButton.setEnabled(false);
			if (sqlButton.isSelected()) {
			    sqlTypeDropdown.setEnabled(false);
			} else {
			    showNoChanges.setEnabled(false);
			}

			copySettingsToProject();

			// XXX: should do most or all of this work in a worker thread

			final CompareSQL sourceComp;
			final CompareSQL targetComp;
			final SQLObject left;
			final SQLObject right;
			try {
				left = source.getObjectToCompare();
				if (left.getChildType() == SQLTable.class) {
					sourceTables = left.getChildren();
				} else {
					sourceTables = new ArrayList<SQLTable>();
				}

				right = target.getObjectToCompare();
				if (right.getChildType() == SQLTable.class) {
					targetTables = right.getChildren();
				} else {
					targetTables = new ArrayList<SQLTable>();
				}

				sourceComp = new CompareSQL(sourceTables,
						targetTables);
				targetComp = new CompareSQL(targetTables,
						sourceTables);
			} catch (ArchitectException ex) {
			    ASUtils.showExceptionDialog(session,
			            "Could not begin diff process", ex);
			    return;
			} catch (Exception ex) {
			    ASUtils.showExceptionDialogNoReport(CompareDMPanel.this, "Couldn't read file.", ex);
				logger.error("Could not read file", ex);
				return;
			} finally {
	             reenableGUIComponents();
			}
			
			SPSwingWorker compareWorker = new SPSwingWorker(session) {

				private List<DiffChunk<SQLObject>> diff;
				private List<DiffChunk<SQLObject>> diff1;

				public void doStuff() throws ArchitectException {
					diff = sourceComp.generateTableDiffs();
					diff1 = targetComp.generateTableDiffs();
				}

				public void cleanup() {
				    reenableGUIComponents();
                    if (getDoStuffException() != null) {
                        Throwable exc = getDoStuffException();
                        logger.error("Error in doStuff()", exc);
                        ASUtils.showExceptionDialog(session,
                                "Database Comparison Failed!", exc);
                        return;
                    }
					logger.debug("cleanup starts");
                    CompareDMFormatter dmFormat = new CompareDMFormatter(session, CompareDMPanel.this, session.getCompareDMSettings());
                    dmFormat.format(diff, diff1, left, right);
                    logger.debug("cleanup finished");
				}

			};

			new Thread(compareWorker).start();
			ProgressWatcher.watchProgress(progressBar,sourceComp);
		}
		
		private void reenableGUIComponents() {
		    sqlButton.setEnabled(true);
            englishButton.setEnabled(true);
            if (sqlButton.isSelected()) {
                sqlTypeDropdown.setEnabled(true);
            } else {
                showNoChanges.setEnabled(true);
            }
            startCompareAction.setEnabled(isStartable());
		}

	}

	public SourceOrTargetStuff getSourceStuff() {
		return source;
	}

	public void copySettingsToProject() {
		CompareDMSettings s = session.getCompareDMSettings();
		s.setSaveFlag(true);
		s.setOutputFormat(englishButton.isSelected()?CompareDMSettings.OutputFormat.ENGLISH:CompareDMSettings.OutputFormat.SQL);
		s.setSuppressSimilarities(showNoChanges.isSelected());
        
        Class<? extends DDLGenerator> selectedGenerator = 
            (Class<? extends DDLGenerator>) sqlTypeDropdown.getSelectedItem();
        s.setDdlGenerator(selectedGenerator);
        
		SourceOrTargetSettings sourceSetting = s.getSourceSettings();
		copySourceOrTargetSettingsToProject(sourceSetting,source);
        s.setSourceStuff(source);

		SourceOrTargetSettings targetSetting = s.getTargetSettings();
		copySourceOrTargetSettingsToProject(targetSetting,target);
        s.setTargetStuff(target);

	}

	public void copySourceOrTargetSettingsToProject(SourceOrTargetSettings setting,
													SourceOrTargetStuff stuff) {

		if ( stuff.databaseDropdown.getItemCount() > 0 &&
			 stuff.databaseDropdown.getSelectedIndex() >= 0 &&
			 stuff.databaseDropdown.getSelectedItem() != null )
			setting.setConnectName( ((SPDataSource)stuff.databaseDropdown.getSelectedItem()).getName() );
		else
			setting.setConnectName( null );


		if ( stuff.catalogDropdown.getItemCount() > 0 &&
				 stuff.catalogDropdown.getSelectedIndex() >= 0 &&
				 stuff.catalogDropdown.getSelectedItem() != null )
			setting.setCatalogObject( stuff.catalogDropdown.getSelectedItem() );
		else setting.setCatalog(null);

		if ( stuff.schemaDropdown.getItemCount() > 0 &&
				 stuff.schemaDropdown.getSelectedIndex() >= 0 &&
				 stuff.schemaDropdown.getSelectedItem() != null )
			setting.setSchemaObject( stuff.schemaDropdown.getSelectedItem() );
		else
			setting.setSchema(null);

		setting.setFilePath(stuff.loadFilePath.getText());

		if ( stuff.loadRadio.isSelected() )
			setting.setDatastoreType(CompareDMSettings.DatastoreType.FILE);
		if ( stuff.physicalRadio.isSelected() )
			setting.setDatastoreType(CompareDMSettings.DatastoreType.DATABASE);
		if ( stuff.playPenRadio.isSelected() )
			setting.setDatastoreType(CompareDMSettings.DatastoreType.PROJECT);
	}

	private void restoreSettingsFromProject() throws ArchitectException {
		CompareDMSettings s = session.getCompareDMSettings();

		restoreSourceOrTargetSettingsFromProject(source,s.getSourceSettings());
		restoreSourceOrTargetSettingsFromProject(target,s.getTargetSettings());
		if ( s.getOutputFormat() == CompareDMSettings.OutputFormat.ENGLISH )
			englishButton.doClick();

		if ( s.getOutputFormat() == CompareDMSettings.OutputFormat.SQL)
			sqlButton.doClick();
        
        showNoChanges.setSelected(s.getSuppressSimilarities());

        sqlTypeDropdown.setSelectedItem(s.getDdlGenerator());
	}


	private void restoreSourceOrTargetSettingsFromProject(SourceOrTargetStuff stuff,
			SourceOrTargetSettings set) throws ArchitectException {

		DatastoreType rbs = set.getDatastoreType();
		if ( rbs == CompareDMSettings.DatastoreType.PROJECT )
			stuff.playPenRadio.doClick();
		else if ( rbs == CompareDMSettings.DatastoreType.DATABASE )
			stuff.physicalRadio.doClick();
		else if ( rbs == CompareDMSettings.DatastoreType.FILE )
			stuff.loadRadio.doClick();

		List<SPDataSource> lds = session.getContext().getConnections();
		for (SPDataSource ds : lds){
			if (ds.getDisplayName().equals(set.getConnectName())){
				stuff.databaseDropdown.setSelectedItem(ds);
				if (set.getCatalog() != null) {
					stuff.catalogDropdown.getModel().addListDataListener(
							new RestoreSettingsListener(stuff.catalogDropdown, set.getCatalog()));
				}
				if (set.getSchema() != null) {
					stuff.schemaDropdown.getModel().addListDataListener(
							new RestoreSettingsListener(stuff.schemaDropdown, set.getSchema()));
				}
				if ( stuff.catalogDropdown.getItemCount() == 0 &&
					 stuff.schemaDropdown.getItemCount() > 0 &&
					 set.getSchema() != null &&
					set.getSchema().length() > 0 ) {
					for ( int j=0; j<stuff.schemaDropdown.getItemCount(); j++ ) {
						SQLObject o2 = (SQLObject)stuff.schemaDropdown.getItemAt(j);
						if ( o2.getName().equals(set.getSchema())) {
							stuff.schemaDropdown.setSelectedIndex(j);
							break;
						}
					}
				}
				break;
			}
		}
		if (set.getFilePath() != null)
			stuff.loadFilePath.setText(set.getFilePath());
	}

	public SourceOrTargetStuff getTargetStuff() {
		return target;
	}
	
	
	/**
	 * Sets the values for the database, schema and catalog in the panel 
	 * for the source set, and set the target to look in the play pen
	 */
	public void compareCurrentWithOrig(SQLSchema schema, SQLCatalog catalog, SQLDatabase db) {
	    
	    
	    //catalog may be null for some dbs (at least in Oracle)
	    if (catalog != null) {
	        source.catalogPop.setDefaultCatalog(catalog.getName());
	    }
	    
	    //schema can be null in a MYSQL Database
	    if (schema != null) {
    	    //this needs to be set because if there is no catalog 
    	    //then the catalog populator is responsible for the schemas
    	    source.catalogPop.setDefaultSchema(schema.getName());
    	    source.schemaPop.setDefaultSelect(schema.getName());
	    }
	    
	    source.physicalRadio.doClick();
	    
        //selects the correct data base, this only looks at 
	    for (int x = 1; x < source.databaseDropdown.getItemCount(); x++) {
            SPDataSource curr = (SPDataSource)(source.databaseDropdown.getItemAt(x));
            if (curr != null && curr.getName().equals(db.getName())) {
                source.databaseDropdown.setSelectedIndex(x);
                break;
            }
        }
        
        target.playPenRadio.doClick();    
	    
	}	
	
	/**
	 *  A simple action to swap the settings for older and newer. 
	 */
	public class SwapSourceTargetAction extends AbstractAction   {
	    public SwapSourceTargetAction() {
	        super("Swap");
	    }

        public void actionPerformed(ActionEvent e) {
            boolean sourcePlayPen = source.playPenRadio.isSelected();
            boolean sourcePhysical = source.physicalRadio.isSelected();
            String sourceLoadFilePath = source.loadFilePath.getText();
            String targetLoadFilePath = target.loadFilePath.getText();
            
            SPDataSource soDBObj = null;
            SQLObject soCatObj  = null; 
            SQLObject soSchemaObj = null;
            
            SPDataSource taDBObj = null; 
            SQLObject taCatObj  = null; 
            SQLObject taSchemaObj = null;
            
            //gets the data from the drop down menus
            //the objects are only loaded as needed because changing an option 
            //in the DB drop down menu if it is disabled will still enable
            //the catalog and or schema dropdowns
            if (source.physicalRadio.isSelected()) {
                soDBObj = (SPDataSource)source.databaseDropdown.getSelectedItem();
                soCatObj  = (SQLObject)source.catalogDropdown.getSelectedItem();
                soSchemaObj = (SQLObject)source.schemaDropdown.getSelectedItem();
            }
            
            if (target.physicalRadio.isSelected()) {
                taDBObj = (SPDataSource)target.databaseDropdown.getSelectedItem();
                taCatObj  = (SQLObject)target.catalogDropdown.getSelectedItem();
                taSchemaObj = (SQLObject)target.schemaDropdown.getSelectedItem();
                
            }
            
            target.loadFilePath.setText(sourceLoadFilePath);
            source.loadFilePath.setText(targetLoadFilePath);
            
            //select the db connection in the other list
            //then set the defaults for the catalog and schema so they
            //will be updated by the db connection dropdown change event
            if (soDBObj != null) {
                for (int x = 1; x < target.databaseDropdown.getItemCount(); x++) {
                    if (target.databaseDropdown.getItemAt(x).equals(soDBObj)) {
                        target.databaseDropdown.setSelectedIndex(x);
                    }
                }
            }
            if (soCatObj != null) {
                target.catalogPop.setDefaultCatalog(soCatObj.getName());
            }
            if (soSchemaObj != null) {
                target.catalogPop.setDefaultSchema(soSchemaObj.getName());
                target.schemaPop.setDefaultSelect(soSchemaObj.getName());
            }

            if (taDBObj != null) {
                for (int x = 1; x < source.databaseDropdown.getItemCount(); x++) {
                    if (source.databaseDropdown.getItemAt(x).equals(taDBObj)) {
                        source.databaseDropdown.setSelectedIndex(x);
                    }
                }
            }
            if (taCatObj != null) {
                source.catalogPop.setDefaultCatalog(taCatObj.getName());
            }
            if (taSchemaObj != null) {
                source.catalogPop.setDefaultSchema(taSchemaObj.getName());
                source.schemaPop.setDefaultSelect(taSchemaObj.getName());
            }
            
            if (target.playPenRadio.isSelected()) {
                source.playPenRadio.doClick();
            } else if (target.physicalRadio.isSelected()) {
                source.physicalRadio.doClick();
            } else {
                source.loadRadio.doClick();
            }
            
            if (sourcePlayPen) {
                target.playPenRadio.doClick();
            } else if (sourcePhysical) {
                target.physicalRadio.doClick();
            } else {
                target.loadRadio.doClick();
            }
            
        }
	}
}