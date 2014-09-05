package org.semanticweb.ontop.sql;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Scanner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openrdf.model.Model;
import org.openrdf.query.Query;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.semanticweb.ontop.owlrefplatform.core.QuestConstants;
import org.semanticweb.ontop.owlrefplatform.core.QuestPreferences;
import org.semanticweb.ontop.r2rml.R2RMLManager;
import org.semanticweb.ontop.sesame.RepositoryConnection;
import org.semanticweb.ontop.sesame.SesameVirtualRepo;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
/**
 * Tests the timeout on sesame queries
 * 
 * Some stuff copied from ExampleManualMetadata 
 * 
 * @author dhovl
 *
 */
public class TestSesameTimeout {
	static String owlfile = "src/test/resources/userconstraints/uc.owl";
	static String obdafile = "src/test/resources/userconstraints/uc.obda";
	static String r2rmlfile = "src/test/resources/userconstraints/uc.ttl";

	static String uc_keyfile = "src/test/resources/userconstraints/keys.lst";
	static String uc_create = "src/test/resources/userconstraints/create.sql";

	private Connection sqlConnection;
	private RepositoryConnection conn;
	

	@Before
	public void init()  throws Exception {

		QuestPreferences preference;
		OWLOntology ontology;
		Model model;

		sqlConnection= DriverManager.getConnection("jdbc:h2:mem:countries","sa", "");
		java.sql.Statement s = sqlConnection.createStatement();

		try {
			Scanner sqlFile = new Scanner(new File(uc_create));
			String text = sqlFile.useDelimiter("\\A").next();
			sqlFile.close();
			
			s.execute(text);
			for(int i = 1; i <= 100; i++){
				s.execute("INSERT INTO TABLE1 VALUES (" + i + "," + i + ");");
			}

		} catch(SQLException sqle) {
			System.out.println("Exception in creating db from script");
		}

		s.close();

		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		ontology = manager.loadOntologyFromOntologyDocument(new File(owlfile));

		R2RMLManager rmanager = new R2RMLManager(r2rmlfile);
		model = rmanager.getModel();
		
		preference = new QuestPreferences();
		preference.setCurrentValueOf(QuestPreferences.ABOX_MODE, QuestConstants.VIRTUAL);
		preference.setCurrentValueOf(QuestPreferences.DBNAME, "countries");
		preference.setCurrentValueOf(QuestPreferences.JDBC_URL, "jdbc:h2:mem:countries");
		preference.setCurrentValueOf(QuestPreferences.DBUSER, "sa");
		preference.setCurrentValueOf(QuestPreferences.DBPASSWORD, "");
		preference.setCurrentValueOf(QuestPreferences.JDBC_DRIVER, "org.h2.Driver");

		SesameVirtualRepo repo = new SesameVirtualRepo("", ontology, model, preference);
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



	@Test
	public void testTimeout() throws Exception {
		String queryString = "PREFIX : <http://www.semanticweb.org/ontologies/2013/7/untitled-ontology-150#> SELECT * WHERE {?x :hasVal1 ?v1; :hasVal2 ?v2.}";
        
        // execute query
        Query query = conn.prepareQuery(QueryLanguage.SPARQL, queryString);

        TupleQuery tq = (TupleQuery) query;
        tq.setMaxQueryTime(1);
        boolean exceptionThrown = false;
        long start = System.currentTimeMillis();
        try {
        	TupleQueryResult result = tq.evaluate();
        	result.close();
        } catch (QueryEvaluationException e) {
        	long end = System.currentTimeMillis();
        	assertTrue(e.toString().indexOf("SesameTupleQuery timed out. More than 1 seconds passed") >= 0);
        	assertTrue(end - start >= 1000);
        	exceptionThrown = true;
        } 
		assertTrue(exceptionThrown);
		
	}
	
	@Test
	public void testNoTimeout() throws Exception {
		String queryString = "PREFIX : <http://www.semanticweb.org/ontologies/2013/7/untitled-ontology-150#> SELECT * WHERE {?x :hasVal1 ?v1; :hasVal2 ?v2.}";
        
        // execute query
        Query query = conn.prepareQuery(QueryLanguage.SPARQL, queryString);

        TupleQuery tq = (TupleQuery) query;
        TupleQueryResult result = tq.evaluate();
		assertTrue(result.hasNext());
		result.close();
	
	}
}
