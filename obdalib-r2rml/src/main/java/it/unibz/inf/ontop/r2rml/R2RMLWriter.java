package it.unibz.inf.ontop.r2rml;

/*
 * #%L
 * ontop-obdalib-sesame
 * %%
 * Copyright (C) 2009 - 2014 Free University of Bozen-Bolzano
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
/**
 * @author timea bagosi
 * Class responsible to write an r2rml turtle file given an obda model
 */

import eu.optique.api.mapping.R2RMLMappingManager;
import eu.optique.api.mapping.TriplesMap;
import eu.optique.api.mapping.impl.rdf4j.RDF4JR2RMLMappingManagerFactory;
import it.unibz.inf.ontop.io.PrefixManager;
import it.unibz.inf.ontop.model.OBDAMappingAxiom;
import it.unibz.inf.ontop.model.OBDAModel;
import org.eclipse.rdf4j.model.Graph;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.GraphImpl;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.semanticweb.owlapi.model.OWLOntology;

import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;


public class R2RMLWriter {
	
	private BufferedWriter out;
	private List<OBDAMappingAxiom> mappings;
	private URI sourceUri;
	private PrefixManager prefixmng;
	private OWLOntology ontology;
	
	public R2RMLWriter(File file, OBDAModel obdamodel, URI sourceURI, OWLOntology ontology)
	{
		this(obdamodel, sourceURI, ontology);
		try {
			this.out = new BufferedWriter(new FileWriter(file));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public R2RMLWriter(OBDAModel obdamodel, URI sourceURI, OWLOntology ontology)
	{
		this.sourceUri = sourceURI;	
		this.mappings = obdamodel.getMappings(sourceUri);
		this.prefixmng = obdamodel.getPrefixManager(); 
		this.ontology = ontology;
	}
	
	public R2RMLWriter(OBDAModel obdamodel, URI sourceURI){
		this(obdamodel, sourceURI, null);
	}
	
	public R2RMLWriter(File file, OBDAModel obdamodel, URI sourceURI){
		this(file, obdamodel, sourceURI, null);
	}


	/**
	 * call this method if you need the RDF Graph
	 * that represents the R2RML mappings
	 * @return an RDF Graph
	 */
	@Deprecated
	public Graph getGraph() {
		OBDAMappingTransformer transformer = new OBDAMappingTransformer();
		transformer.setOntology(ontology);
		List<Statement> statements = new ArrayList<Statement>();
		
		for (OBDAMappingAxiom axiom: this.mappings) {
			List<Statement> statements2 = transformer.getStatements(axiom,prefixmng);
			statements.addAll(statements2);
		}
		@SuppressWarnings("deprecation")
		Graph g = new GraphImpl(); 
		g.addAll(statements);
		return g;
	}

	public Collection <TriplesMap> getTriplesMaps() {
		OBDAMappingTransformer transformer = new OBDAMappingTransformer();
		transformer.setOntology(ontology);
		Collection<TriplesMap> coll = new LinkedList<TriplesMap>();
		for (OBDAMappingAxiom axiom: this.mappings) {
			TriplesMap tm = transformer.getTriplesMap(axiom, prefixmng);
			coll.add(tm);
		}
		return coll;
	}
	
	/**
	 * the method to write the R2RML mappings
	 * from an rdf Model to a file
	 * @param file the ttl file to write to
	 */
	public void write(File file) throws Exception {
		try {
            FileOutputStream fos = new FileOutputStream(file);
			write(fos);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

    /**
     * the method to write the R2RML mappings
     * from an rdf Model to a file
     * @param os the output target
     */
    public void write(OutputStream os) throws Exception {
        try {
            R2RMLMappingManager mm = new RDF4JR2RMLMappingManagerFactory().getR2RMLMappingManager();
            Collection<TriplesMap> coll = getTriplesMaps();
            Model out = mm.exportMappings(coll, Model.class);
            Rio.write(out, os, RDFFormat.TURTLE);
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
			throw e;
		}
	}
}
