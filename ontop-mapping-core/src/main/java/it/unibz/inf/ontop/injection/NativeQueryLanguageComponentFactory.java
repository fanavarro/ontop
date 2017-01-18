package it.unibz.inf.ontop.injection;

import com.google.inject.assistedinject.Assisted;
import it.unibz.inf.ontop.model.*;
import org.eclipse.rdf4j.model.Model;
import it.unibz.inf.ontop.mapping.MappingParser;
import it.unibz.inf.ontop.nativeql.DBMetadataExtractor;

import java.io.File;
import java.io.Reader;
import java.util.List;

/**
 * Factory following the Guice AssistedInject pattern.
 *
 * See https://github.com/google/guice/wiki/AssistedInject.
 *
 * Builds core components that we want to be modular.
 *
 * Please note that the NativeQueryGenerator is NOT PART
 * of this factory because it belongs to another module
 * (see the QuestComponentFactory).
 *
 */
public interface NativeQueryLanguageComponentFactory {

    DBMetadataExtractor create();

    OBDAMappingAxiom create(String id, @Assisted("sourceQuery") SourceQuery sourceQuery,
                            @Assisted("targetQuery") List<Function> targetQuery);

    OBDAMappingAxiom create(@Assisted("sourceQuery") SourceQuery sourceQuery,
                            @Assisted("targetQuery") List<Function> targetQuery);
}
