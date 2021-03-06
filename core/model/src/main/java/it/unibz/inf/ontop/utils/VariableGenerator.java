package it.unibz.inf.ontop.utils;

import com.google.common.collect.ImmutableSet;
import it.unibz.inf.ontop.datalog.CQIE;
import it.unibz.inf.ontop.model.term.Variable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static it.unibz.inf.ontop.model.OntopModelSingletons.TERM_FACTORY;

/**
 * Generates new variables that are guaranteed to not conflict with
 * already variables in a given scope.
 *
 * The typical scope for variables is the body of a rule.
 */
public class VariableGenerator {

    private int count;
    private final Set<Variable> knownVariables;

    private static String SUFFIX_PREFIX = "f";


    public VariableGenerator(Collection<Variable> knownVariables) {
        count = 0;
        this.knownVariables = new HashSet<>(knownVariables);
    }

    /**
     * Rule-level variable generator.
     */
    public VariableGenerator(CQIE initialRule) {
        count = 0;
        knownVariables = initialRule.getReferencedVariables();
    }

    /**
     * Declares additional variables as known.
     */
    public void registerAdditionalVariables(Collection<Variable> additionalVariables) {
        knownVariables.addAll(additionalVariables);
    }

    /**
     * Generates a new non-conflicting variable from a previous one.
     * It will reuse its name.
     */
    public Variable generateNewVariableFromVar(Variable previousVariable) {
        Variable newVariable;
        do {
            newVariable = TERM_FACTORY.getVariable(previousVariable.getName() + SUFFIX_PREFIX + (count++));
        } while(knownVariables.contains(newVariable));

        knownVariables.add(newVariable);
        return newVariable;
    }

    /**
     * Generates a new variable if a conflict is detected.
     */
    public Variable generateNewVariableIfConflicting(Variable previousVariable) {
        Variable newVariable = previousVariable;
        while(knownVariables.contains(newVariable)) {
            newVariable = TERM_FACTORY.getVariable(previousVariable.getName() + SUFFIX_PREFIX + (count++));
        }

        knownVariables.add(newVariable);
        return newVariable;
    }

    /**
     * Generates a new non-conflicting variable.
     */
    public Variable generateNewVariable() {
        Variable newVariable;
        do {
            newVariable = TERM_FACTORY.getVariable(SUFFIX_PREFIX + (count++));
        } while(knownVariables.contains(newVariable));

        knownVariables.add(newVariable);
        return newVariable;
    }

    /**
     * Instant snapshot of variable it knows.
     */
    public ImmutableSet<Variable> getKnownVariables() {
        return ImmutableSet.copyOf(knownVariables);
    }
}
