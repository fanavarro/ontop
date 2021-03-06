package it.unibz.inf.ontop.utils;

import com.google.common.collect.ImmutableList;
import it.unibz.inf.ontop.dbschema.DatabaseRelationDefinition;
import it.unibz.inf.ontop.dbschema.RDBMetadata;
import it.unibz.inf.ontop.dbschema.RDBMetadataExtractionTools;
import it.unibz.inf.ontop.exception.DuplicateMappingException;
import it.unibz.inf.ontop.exception.InvalidMappingException;
import it.unibz.inf.ontop.exception.MappingIOException;
import it.unibz.inf.ontop.injection.OntopSQLOWLAPIConfiguration;
import it.unibz.inf.ontop.spec.mapping.OBDASQLQuery;
import it.unibz.inf.ontop.spec.mapping.SQLMappingFactory;
import it.unibz.inf.ontop.spec.mapping.bootstrap.impl.DirectMappingEngine;
import it.unibz.inf.ontop.spec.mapping.pp.SQLPPMapping;
import it.unibz.inf.ontop.spec.mapping.pp.SQLPPTriplesMap;
import it.unibz.inf.ontop.spec.mapping.pp.impl.OntopNativeSQLPPTriplesMap;
import it.unibz.inf.ontop.spec.mapping.impl.SQLMappingFactoryImpl;
import it.unibz.inf.ontop.model.term.ImmutableFunctionalTerm;
import it.unibz.inf.ontop.spec.mapping.bootstrap.impl.DirectMappingAxiomProducer;
import it.unibz.inf.ontop.protege.core.OBDAModel;
import it.unibz.inf.ontop.protege.core.OBDAModelManager;
import it.unibz.inf.ontop.protege.utils.JDBCConnectionManager;
import org.protege.editor.owl.model.OWLModelManager;
import org.semanticweb.owlapi.model.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import static it.unibz.inf.ontop.model.OntopModelSingletons.TERM_FACTORY;


public class BootstrapGenerator {


    private final JDBCConnectionManager connManager;
    private final OntopSQLOWLAPIConfiguration configuration;
    private final OBDAModel activeOBDAModel;
    private final OWLModelManager owlManager;
    private static final SQLMappingFactory SQL_MAPPING_FACTORY = SQLMappingFactoryImpl.getInstance();
    private int currentMappingIndex = 1;

    public BootstrapGenerator(OBDAModelManager obdaModelManager, String baseUri, OWLModelManager owlManager) throws DuplicateMappingException, InvalidMappingException, MappingIOException, SQLException, OWLOntologyCreationException, OWLOntologyStorageException {

        this.connManager = JDBCConnectionManager.getJDBCConnectionManager();
        this.owlManager =  owlManager;
        this.configuration = obdaModelManager.getConfigurationManager().buildOntopSQLOWLAPIConfiguration(owlManager.getActiveOntology());
        this.activeOBDAModel = obdaModelManager.getActiveOBDAModel();

        bootstrapMappingAndOntologyProtege(baseUri);
    }

    private void bootstrapMappingAndOntologyProtege(String baseUri) throws DuplicateMappingException, SQLException {

        List<SQLPPTriplesMap> sqlppTriplesMaps = bootstrapMapping(activeOBDAModel.generatePPMapping(), baseUri);

        // update protege ontology
        OWLOntologyManager manager = owlManager.getActiveOntology().getOWLOntologyManager();
        Set<OWLDeclarationAxiom> declarationAxioms = DirectMappingEngine.extractDeclarationAxioms(manager,
                sqlppTriplesMaps.stream()
                        .flatMap(ax -> ax.getTargetAtoms().stream()));
        List<AddAxiom> addAxioms = declarationAxioms.stream()
                .map(ax -> new AddAxiom(owlManager.getActiveOntology(), ax))
                .collect(Collectors.toList());

        owlManager.applyChanges(addAxioms);
    }

    private List<SQLPPTriplesMap> bootstrapMapping(SQLPPMapping ppMapping, String baseURI)
            throws DuplicateMappingException, SQLException {

        List<SQLPPTriplesMap> newTriplesMap = new ArrayList<>();

        currentMappingIndex = ppMapping.getTripleMaps().size() + 1;

        final Connection conn;
        try {
            conn = connManager.getConnection(configuration.getSettings());
        }
        catch (SQLException e) {
            throw new RuntimeException("JDBC connection are missing, have you setup Ontop Mapping properties?" +
                    " Message: " + e.getMessage());
        }
        RDBMetadata metadata = RDBMetadataExtractionTools.createMetadata(conn);

        // this operation is EXPENSIVE
        RDBMetadataExtractionTools.loadMetadata(metadata, conn, null);

        if (baseURI == null || baseURI.isEmpty()) {
            baseURI = ppMapping.getMetadata().getPrefixManager().getDefaultPrefix();
        }
        else {
            baseURI = DirectMappingEngine.fixBaseURI(baseURI);
        }
        Collection<DatabaseRelationDefinition> tables = metadata.getDatabaseRelations();

        for (DatabaseRelationDefinition td : tables) {
            newTriplesMap.addAll(getMapping(td, baseURI));
        }

        //add to the current model the boostrapped triples map
        for (SQLPPTriplesMap triplesMap: newTriplesMap) {
            activeOBDAModel.addTriplesMap(triplesMap, true);
        }
        return newTriplesMap;
    }


    private List<SQLPPTriplesMap> getMapping(DatabaseRelationDefinition table, String baseUri) {

        DirectMappingAxiomProducer dmap = new DirectMappingAxiomProducer(baseUri, TERM_FACTORY);

        List<SQLPPTriplesMap> axioms = new ArrayList<>();
        axioms.add(new OntopNativeSQLPPTriplesMap("MAPPING-ID"+ currentMappingIndex, SQL_MAPPING_FACTORY.getSQLQuery(dmap.getSQL(table)), dmap.getCQ(table)));
        currentMappingIndex++;

        Map<String, ImmutableList<ImmutableFunctionalTerm>> refAxioms = dmap.getRefAxioms(table);
        for (Map.Entry<String, ImmutableList<ImmutableFunctionalTerm>> e : refAxioms.entrySet()) {
            OBDASQLQuery sqlQuery = SQL_MAPPING_FACTORY.getSQLQuery(e.getKey());
            ImmutableList<ImmutableFunctionalTerm> targetQuery = e.getValue();
            axioms.add(new OntopNativeSQLPPTriplesMap("MAPPING-ID"+ currentMappingIndex, sqlQuery, targetQuery));
            currentMappingIndex++;
        }
        return axioms;
    }
}
