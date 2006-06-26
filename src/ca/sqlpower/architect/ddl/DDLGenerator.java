/*
 * Created on May 11, 2005
 *
 * This code belongs to SQL Power Group Inc.
 */
package ca.sqlpower.architect.ddl;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import ca.sqlpower.architect.ArchitectException;
import ca.sqlpower.architect.SQLColumn;
import ca.sqlpower.architect.SQLDatabase;
import ca.sqlpower.architect.SQLRelationship;
import ca.sqlpower.architect.SQLTable;

/**
 * The DDLGenerator interface is a generic API for turning a SQLObject
 * hierarchy into a series of SQL statements which create the corresponding
 * data model in a physical database.
 * 
 * @author fuerth
 * @version $Id$
 */
public interface DDLGenerator {
    
    /**
     * Creates a list of DDLStatement objects which create all of the tables,
     * their columns and primary keys, and the foreign key relationships of the 
     * given database.
     * 
     * @param source The database to generate a DDL representation of.
     * @return The list of DDL statements that can create the given database, in the
     * order they should be executed.
     * @throws SQLException If there is a problem getting type info from the target DB.
     * @throws ArchitectException If there are problems with the Architect objects.
     */
    public abstract List<DDLStatement> generateDDLStatements(SQLDatabase source)
            throws SQLException, ArchitectException;
    
    /**
     * Appends the DDL statement for dropping the given column from its parent
     * table in this DDL Generator's target schema/catalog.
     * 
     * @param c The column to create a DROP statement for.
     */
    public void dropColumn(SQLColumn c);

    /**
     * Appends the DDL statement for adding the given column to its parent
     * table in this DDL Generator's target schema/catalog.
     * 
     * @param c The column to create a ADD statement for.
     */
    public void addColumn(SQLColumn c);

    /**
     * Appends the DDL statement for modifying the given column's datatype and
     * nullability in its parent table in this DDL Generator's target schema/catalog.
     * 
     * @param c The column to create a MODIFY or ALTER COLUMN statement for.
     */
    public void modifyColumn(SQLColumn c);

    /**
     * Appends the DDL statement for creating the given FK relationship
     * in this DDL Generator's target schema/catalog.
     * 
     * @param c The relationship to create a FOREIGN KEY statement for.
     */
    public void addRelationship(SQLRelationship r);
    
    /**
     * Appends the DDL statement for dropping the given FK relationship
     * in this DDL Generator's target schema/catalog.
     * 
     * @param c The relationship to create a DROP FOREIGN KEY statement for.
     */
    public void dropRelationship(SQLRelationship r);

    /** 
     * Appends the DDL statements for dropping the table in this DDL Generator's
     * current catalog and schema using SQLTable t's physical name.
     */
    public void dropTable(SQLTable t);
    
    /** 
     * Appends the DDL statements for creating a table in this DDL Generator's
     * current catalog and schema using SQLTable t as a template.
     */
    public void writeTable(SQLTable t) throws SQLException, ArchitectException;
    
    /**
     * Returns the list of DDL statements that have been created so far.  Call
     * {@link #generateDDLStatements(SQLDatabase)} to populate this list.
     */
    public List<DDLStatement> getDdlStatements();

    /**
     * Converts an arbitrary string (which may contain spaces, mixed case, 
     * punctuation, and so on) into a valid identifier in the target
     * database system.  The implementation should mangle the input string
     * to the minimum degree necessary to make the given string into a valid
     * identifier on the target system.
     */
    public abstract String toIdentifier(String name);

    /**
     * Creates and returns a DDL statement which will drop the given table in this
     * DDL Generator's current catalog and schema.
     * 
     * @param table The name of the table to be dropped.
     * @return A SQL statement which will drop the table. 
     */
    public abstract String makeDropTableSQL(String table);

    /**
     * Creates and returns a DDL statement which will drop a foreign key relationship in this
     * DDL Generator's current catalog and schema.
     * 
     * @param fkTable The name of the FK table whose relationship should be dropped.
     * @param fkName The name of the key to drop.
     * @return a SQL statement which will drop the key.
     */
    public abstract String makeDropForeignKeySQL(String fkTable, String fkName);

    // ---------------------- accessors and mutators ----------------------

    /**
     * Tells the generator whether or not it can connect to the target 
     * database and ask for additional information during the generation
     * process. For instance, to populate the type map.
     */
    public abstract boolean getAllowConnection();

    /**
     * See {@link #getAllowConnection()}.
     */
    public abstract void setAllowConnection(boolean argAllowConnection);

    /**
     * Gets the value of typeMap
     *
     * @return the value of typeMap
     */
    public abstract Map getTypeMap();

    /**
     * Sets the value of typeMap
     *
     * @param argTypeMap Value to assign to this.typeMap
     */
    public abstract void setTypeMap(Map argTypeMap);

    /**
     * Returns {@link #warnings}.
     */
    public abstract List getWarnings();

    /**
     * See {@link #targetCatalog}.
     *
     * @return the value of targetCatalog
     */
    public abstract String getTargetCatalog();

    /**
     * See {@link #targetCatalog}.
     *
     * @param argTargetCatalog Value to assign to this.targetCatalog
     */
    public abstract void setTargetCatalog(String argTargetCatalog);

    /**
     * See {@link #targetSchema}.
     *
     * @return the value of targetSchema
     */
    public abstract String getTargetSchema();

    /**
     * See {@link #targetSchema}.
     *
     * @param argTargetSchema Value to assign to this.targetSchema
     */
    public abstract void setTargetSchema(String argTargetSchema);

    /**
     * The name that the target database gives to the JDBC idea of
     * "catalog."  For Oracle, this would be null (no catalogs) and
     * for SQL Server it would be "Database".
     */
    public abstract String getCatalogTerm();

    /**
     * The name that the target database gives to the JDBC idea of
     * "schema."  For Oracle, this would be "Schema" and for SQL
     * Server it would be "Owner".
     */
    public abstract String getSchemaTerm();

	public abstract void dropPrimaryKey(SQLTable t);

	public abstract void addPrimaryKey(SQLTable t) throws ArchitectException;
}