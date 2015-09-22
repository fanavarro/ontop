package org.semanticweb.ontop.sql;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.Scanner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openrdf.model.Model;
import org.openrdf.query.Query;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.semanticweb.ontop.injection.OBDAProperties;
import org.semanticweb.ontop.owlrefplatform.core.QuestConstants;
import org.semanticweb.ontop.owlrefplatform.core.QuestPreferences;
import org.semanticweb.ontop.owlrefplatform.questdb.R2RMLQuestPreferences;
import org.semanticweb.ontop.r2rml.R2RMLManager;
import org.semanticweb.ontop.sesame.RepositoryConnection;
import org.semanticweb.ontop.sesame.SesameVirtualRepo;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
/**
 * Tests that user-applied constraints can be provided through 
 * sesameWrapper.SesameVirtualRepo 
 * with manually instantiated metadata.
 * 
 * This is quite similar to the setting in the optique platform
 * 
 * Some stuff copied from ExampleManualMetadata 
 * 
 * @author dhovl
 *
 */
public class TestSesameResultIteration {
	static String owlfile = "src/test/resources/userconstraints/uc.owl";
	static String obdafile = "src/test/resources/userconstraints/uc.obda";
	static String r2rmlfile = "src/test/resources/userconstraints/uc.ttl";

	static String uc_keyfile = "src/test/resources/userconstraints/keys.lst";
	static String uc_create = "src/test/resources/userconstraints/create.sql";

	private Connection sqlConnection;
	private RepositoryConnection conn;


	@Before
	public void init()  throws Exception {

		sqlConnection= DriverManager.getConnection("jdbc:h2:mem:countries","sa", "");
		java.sql.Statement s = sqlConnection.createStatement();

		try {
			Scanner sqlFile = new Scanner(new File(uc_create));
			String text = sqlFile.useDelimiter("\\A").next();
			sqlFile.close();

			s.execute(text);
			for(int i = 1; i <= 10; i++){
				s.execute("INSERT INTO TABLE1 VALUES (" + i + "," + i + ");");
			}

		} catch(SQLException sqle) {
			System.out.println("Exception in creating db from script");
		}

		s.close();

		Properties p = new Properties();
		p.setProperty(QuestPreferences.ABOX_MODE, QuestConstants.VIRTUAL);
		p.setProperty(OBDAProperties.DB_NAME, "countries");
		p.setProperty(OBDAProperties.JDBC_URL, "jdbc:h2:mem:countries");
		p.setProperty(OBDAProperties.DB_USER, "sa");
		p.setProperty(OBDAProperties.DB_PASSWORD, "");
		p.setProperty(OBDAProperties.JDBC_DRIVER, "org.h2.Driver");

		SesameVirtualRepo repo = new SesameVirtualRepo("", owlfile, r2rmlfile, new R2RMLQuestPreferences(p));
		repo.initialize();
		/*
		 * Prepare the data connection for querying.
		 */
		conn = repo.getConnection();



	}


	@After
	public void tearDown() throws Exception{
		if (!sqlConnection.isClosed()) {
			java.sql.Statement s = sqlConnection.createStatement();
			try {
				s.execute("DROP ALL OBJECTS DELETE FILES");
			} catch (SQLException sqle) {
				System.out.println("Table not found, not dropping");
			} finally {
				s.close();
				sqlConnection.close();
			}
		}
	}


	/**
	 * This tests that not extracting any rows from the result set is actually faster (in milliseconds resolution)
	 * than not extracting any.
	 * 
	 * Starts with querying, to warm up the system
	 * @throws Exception
	 */

	@Test
	public void testIteration() throws Exception {
		String queryString = "PREFIX : <http://www.semanticweb.org/ontologies/2013/7/untitled-ontology-150#> SELECT * WHERE {?x :hasVal1 ?v1. ?y :hasVal1 ?v2.}";

		// execute query
		Query query = conn.prepareQuery(QueryLanguage.SPARQL, queryString);

		TupleQuery tq = (TupleQuery) query;
		TupleQueryResult result = tq.evaluate();
		assertTrue(result.hasNext());
		result.next();
		result.close();
		
		long start = System.currentTimeMillis();
		result = tq.evaluate();
		result.close();
		long shortTime = System.currentTimeMillis() - start;
		
		start = System.currentTimeMillis();
		result = tq.evaluate();
		while(result.hasNext())
			result.next();
		result.close();
		long longTime = System.currentTimeMillis() - start;
		System.out.println("Long " + longTime + "Short: " + shortTime);
		
		assertTrue(longTime > shortTime);

	}

}
