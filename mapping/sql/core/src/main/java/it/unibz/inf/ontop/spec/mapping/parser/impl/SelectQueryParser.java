package it.unibz.inf.ontop.spec.mapping.parser.impl;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import it.unibz.inf.ontop.dbschema.*;
import it.unibz.inf.ontop.iq.IntermediateQueryBuilder;
import it.unibz.inf.ontop.model.term.Function;
import it.unibz.inf.ontop.model.term.Term;
import it.unibz.inf.ontop.model.term.Variable;
import it.unibz.inf.ontop.spec.mapping.parser.exception.*;
import it.unibz.inf.ontop.utils.ImmutableCollectors;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.parser.TokenMgrError;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static it.unibz.inf.ontop.model.OntopModelSingletons.TERM_FACTORY;

/**
 * Created by Roman Kontchakov on 01/11/2016.
 *
 */
public class SelectQueryParser {
    private final DBMetadata metadata;
    private final QuotedIDFactory idfac;

    private int relationIndex = 0;

    public SelectQueryParser(DBMetadata metadata) {
        this.metadata = metadata;
        this.idfac = metadata.getQuotedIDFactory();
    }

    public RAExpression parse(String sql) throws InvalidSelectQueryException, UnsupportedSelectQueryException {
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (!(statement instanceof Select))
                throw new InvalidSelectQueryException("The query is not a SELECT statement", statement);

            RAExpression re = select(((Select) statement).getSelectBody());
            return re;
        }
        catch (JSQLParserException e) {
            throw new UnsupportedSelectQueryException("Cannot parse SQL: " + sql, e);
        }
        catch (InvalidSelectQueryRuntimeException e) {
            throw new InvalidSelectQueryException(e.getMessage(), e.getObject());
        }
        catch (UnsupportedSelectQueryRuntimeException e) {
            throw new UnsupportedSelectQueryException(e.getMessage(), e.getObject());
        }
        catch (TokenMgrError e) {
            throw new InvalidSelectQueryException("Cannot parse SQL: " + sql, e);
        }
    }

    public RAExpression parse(String sql, IntermediateQueryBuilder queryBuilder) throws InvalidSelectQueryException, UnsupportedSelectQueryException {
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (!(statement instanceof Select))
                throw new InvalidSelectQueryException("The query is not a SELECT statement", statement);

            RAExpression re = select(((Select) statement).getSelectBody());
            return re;
        }
        catch (JSQLParserException e) {
            throw new UnsupportedSelectQueryException("Cannot parse SQL: " + sql, e);
        }
        catch (InvalidSelectQueryRuntimeException e) {
            throw new InvalidSelectQueryException(e.getMessage(), e.getObject());
        }
        catch (UnsupportedSelectQueryRuntimeException e) {
            throw new UnsupportedSelectQueryException(e.getMessage(), e.getObject());
        }
        catch (TokenMgrError e) {
            throw new InvalidSelectQueryException("Cannot parse SQL: " + sql, e);
        }
    }



    private RAExpression select(SelectBody selectBody) {

        if (!(selectBody instanceof PlainSelect))
            throw new UnsupportedSelectQueryRuntimeException("Complex SELECT statements are not supported", selectBody);

        PlainSelect plainSelect = (PlainSelect) selectBody;

        if (plainSelect.getDistinct() != null)
            throw new UnsupportedSelectQueryRuntimeException("DISTINCT is not supported", selectBody);

        if (plainSelect.getGroupByColumnReferences() != null || plainSelect.getHaving() != null)
            throw new UnsupportedSelectQueryRuntimeException("GROUP BY / HAVING are not supported", selectBody);

        if (plainSelect.getLimit() != null || plainSelect.getTop() != null || plainSelect.getOffset()!= null)
            throw new UnsupportedSelectQueryRuntimeException("LIMIT / OFFSET / TOP are not supported", selectBody);

        if (plainSelect.getOrderByElements() != null)
            throw new UnsupportedSelectQueryRuntimeException("ORDER BY is not supported", selectBody);

        if (plainSelect.getOracleHierarchical() != null || plainSelect.isOracleSiblings())
            throw new UnsupportedSelectQueryRuntimeException("Oracle START WITH ... CONNECT BY / ORDER SIBLINGS BY are not supported", selectBody);

        if (plainSelect.getIntoTables() != null)
            throw new InvalidSelectQueryRuntimeException("SELECT INTO is not allowed in mappings", selectBody);

        if (plainSelect.getFromItem() == null)
            throw new UnsupportedSelectQueryRuntimeException("SELECT without FROM is not supported", selectBody);

        RAExpression current = getRelationalExpression(plainSelect.getFromItem());
        if (plainSelect.getJoins() != null) {
            for (Join join : plainSelect.getJoins())
                try {
                    current = join(current, join);
                }
                catch (IllegalJoinException e) {
                    throw new InvalidSelectQueryRuntimeException(e.toString(), plainSelect);
                }
        }

        ImmutableList<Function> filterAtoms = (plainSelect.getWhere() == null)
                ? current.getFilterAtoms()
                : ImmutableList.<Function>builder()
                .addAll(current.getFilterAtoms())
                .addAll(new ExpressionParser(idfac, current.getAttributes())
                        .parseBooleanExpression(plainSelect.getWhere()))
                .build();

        ImmutableMap.Builder<QualifiedAttributeID, Term> attributesBuilder = ImmutableMap.builder();
        SelectItemProcessor sip = new SelectItemProcessor(current.getAttributes());

        plainSelect.getSelectItems().forEach(si -> {
            ImmutableMap<QualifiedAttributeID, Term> attrs = sip.getAttributes(si);

            // attributesBuilder.build() below checks that the keys in attrs do not intersect
            attributesBuilder.putAll(attrs);
        });

        ImmutableMap<QualifiedAttributeID, Term> attributes;
        try {
            attributes = attributesBuilder.build();
        }
        catch (IllegalArgumentException e) {
            SelectItemProcessor sip2 = new SelectItemProcessor(current.getAttributes());
            Map<QualifiedAttributeID, Integer> duplicates = new HashMap<>();
            plainSelect.getSelectItems().forEach(si -> {
                ImmutableMap<QualifiedAttributeID, Term> attrs = sip2.getAttributes(si);
                for (Map.Entry<QualifiedAttributeID, Term> a : attrs.entrySet())
                    duplicates.put(a.getKey(), duplicates.getOrDefault(a.getKey(), 0) + 1);
            });
            throw new InvalidSelectQueryRuntimeException(
                    "Duplicate column names " + Joiner.on(", ").join(
                            duplicates.entrySet().stream()
                                    .filter(d -> d.getValue() > 1)
                                    .map(d -> d.getKey())
                                    .collect(ImmutableCollectors.toList())) + " in the SELECT clause: ", selectBody);
        }

        return new RAExpression(current.getDataAtoms(),
                ImmutableList.<Function>builder().addAll(filterAtoms).build(),
                new RAExpressionAttributes(attributes, null));
    }

    private RAExpression join(RAExpression left, Join join) throws IllegalJoinException {

        if (join.isFull() || join.isRight() || join.isLeft() || join.isOuter())
            throw new UnsupportedSelectQueryRuntimeException("LEFT/RIGHT/FULL OUTER JOINs are not supported", join);

        RAExpression right = getRelationalExpression(join.getRightItem());
        if (join.isSimple()) {
            return RAExpression.crossJoin(left, right);
        }
        else if (join.isCross()) {
            if (join.getOnExpression() != null || join.getUsingColumns() != null)
                throw new InvalidSelectQueryRuntimeException("CROSS JOIN cannot have USING/ON conditions", join);

            if (join.isInner())
                throw new InvalidSelectQueryRuntimeException("CROSS INNER JOIN is not allowed", join);

            return RAExpression.crossJoin(left, right);
        }
        else if (join.isNatural()) {
            if (join.getOnExpression() != null || join.getUsingColumns() != null)
                throw new InvalidSelectQueryRuntimeException("NATURAL JOIN cannot have USING/ON conditions", join);

            if (join.isInner())
                throw new InvalidSelectQueryRuntimeException("NATURAL INNER JOIN is not allowed", join);

            return RAExpression.naturalJoin(left, right);
        }
        else {
            if (join.getOnExpression() != null) {
                if (join.getUsingColumns() !=null)
                    throw new InvalidSelectQueryRuntimeException("JOIN cannot have both USING and ON", join);

                return RAExpression.joinOn(left, right,
                        (attributes ->  new ExpressionParser(idfac, attributes)
                                .parseBooleanExpression(join.getOnExpression())));
            }
            else if (join.getUsingColumns() != null) {
                return RAExpression.joinUsing(left, right,
                        join.getUsingColumns().stream()
                                .map(p -> idfac.createAttributeID(p.getColumnName()))
                                .collect(ImmutableCollectors.toSet()));
            }
            else
                throw new InvalidSelectQueryRuntimeException("[INNER] JOIN requires either ON or USING", join);
        }
    }


    private RAExpression getRelationalExpression(FromItem fromItem) {
        return new FromItemProcessor(fromItem).result;
    }


    private class FromItemProcessor implements FromItemVisitor {

        private RAExpression result = null;

        FromItemProcessor(FromItem fromItem) {
            fromItem.accept(this);
        }

        @Override
        public void visit(Table tableName) {

            RelationID id = idfac.createRelationID(tableName.getSchemaName(), tableName.getName());
            // construct the predicate using the table name
            DatabaseRelationDefinition relation = metadata.getDatabaseRelation(id);
            if (relation == null)
                throw new InvalidSelectQueryRuntimeException("Table " + id + " not found in metadata", tableName);
            relationIndex++;

            RelationID alias = (tableName.getAlias() != null)
                    ? idfac.createRelationID(null, tableName.getAlias().getName())
                    : relation.getID();

            List<Term> terms = new ArrayList<>(relation.getAttributes().size());
            ImmutableMap.Builder attributes = ImmutableMap.<QuotedID, Variable>builder();
            // the order in the loop is important
            relation.getAttributes().forEach(attribute -> {
                QuotedID attributeId = attribute.getID();
                Variable var = TERM_FACTORY.getVariable(attributeId.getName() + relationIndex);
                terms.add(var);
                attributes.put(attributeId, var);
            });
            // create an atom for a particular table
            Function atom = Relation2Predicate.getAtom(relation, terms);

            // DEFAULT SCHEMA
            // TODO: to be improved
            RAExpressionAttributes attrs;
            if ((tableName.getAlias() == null) &&
                    relation.getID().getSchemaName() != null &&
                    metadata.getDatabaseRelation(relation.getID().getSchemalessID()).equals(relation))
                attrs = RAExpressionAttributes.create(attributes.build(), alias, relation.getID().getSchemalessID());
            else
                attrs = RAExpressionAttributes.create(attributes.build(), alias);

            result = new RAExpression(ImmutableList.of(atom), ImmutableList.of(), attrs);
        }


        @Override
        public void visit(SubSelect subSelect) {
            if (subSelect.getAlias() == null || subSelect.getAlias().getName() == null)
                throw new InvalidSelectQueryRuntimeException("SUB-SELECT must have an alias", subSelect);

            RAExpression current = select(subSelect.getSelectBody());

            RelationID aliasId = idfac.createRelationID(null, subSelect.getAlias().getName());
            result = RAExpression.alias(current, aliasId);
        }

        @Override
        public void visit(SubJoin subjoin) {
            if (subjoin.getAlias() == null || subjoin.getAlias().getName() == null)
                throw new InvalidSelectQueryRuntimeException("SUB-JOIN must have an alias", subjoin);

            RAExpression left = getRelationalExpression(subjoin.getLeft());
            RAExpression join;
            try {
                join = join(left, subjoin.getJoin());
            }
            catch (IllegalJoinException e) {
                throw new InvalidSelectQueryRuntimeException(e.toString(), subjoin);
            }

            RelationID aliasId = idfac.createRelationID(null, subjoin.getAlias().getName());
            result = RAExpression.alias(join, aliasId);
        }

        @Override
        public void visit(LateralSubSelect lateralSubSelect) {
            throw new UnsupportedSelectQueryRuntimeException("LateralSubSelects are not supported", lateralSubSelect);
        }

        @Override
        public void visit(ValuesList valuesList) {
            throw new UnsupportedSelectQueryRuntimeException("ValuesLists are not supported", valuesList);
        }

        @Override
        public void visit(TableFunction tableFunction) {
            throw new UnsupportedSelectQueryRuntimeException("TableFunction are not supported", tableFunction);
        }
    }

    private class SelectItemProcessor implements SelectItemVisitor {
        final ImmutableMap<QualifiedAttributeID, Term> attributes;

        ImmutableMap<QualifiedAttributeID, Term> map;

        SelectItemProcessor(ImmutableMap<QualifiedAttributeID, Term> attributes) {
            this.attributes = attributes;
        }

        ImmutableMap<QualifiedAttributeID, Term> getAttributes(SelectItem si) {
            si.accept(this);
            return map;
        }

        @Override
        public void visit(AllColumns allColumns) {
            map = attributes.entrySet().stream()
                    .filter(e -> e.getKey().getRelation() == null)
                    .collect(ImmutableCollectors.toMap());
        }

        @Override
        public void visit(AllTableColumns allTableColumns) {
            Table table = allTableColumns.getTable();
            RelationID id = idfac.createRelationID(table.getSchemaName(), table.getName());

            map = attributes.entrySet().stream()
                    .filter(e -> e.getKey().getRelation() != null && e.getKey().getRelation().equals(id))
                    .collect(ImmutableCollectors.toMap(
                            e -> new QualifiedAttributeID(null, e.getKey().getAttribute()),
                            Map.Entry::getValue));
        }

        @Override
        public void visit(SelectExpressionItem selectExpressionItem) {
            Expression expr = selectExpressionItem.getExpression();
            if (expr instanceof Column) {
                Column column = (Column) expr;
                QuotedID id = idfac.createAttributeID(column.getColumnName());
                Table table = column.getTable();
                QualifiedAttributeID attr = (table == null || table.getName() == null)
                        ? new QualifiedAttributeID(null, id)
                        : new QualifiedAttributeID(idfac.createRelationID(table.getSchemaName(), table.getName()), id);

                Term var = attributes.get(attr);
                if (var != null) {
                    Alias columnAlias = selectExpressionItem.getAlias();
                    QuotedID name = (columnAlias == null || columnAlias.getName() == null)
                            ? id
                            : idfac.createAttributeID(columnAlias.getName());

                    map = ImmutableMap.of(new QualifiedAttributeID(null, name), var);
                }
                else
                    throw new InvalidSelectQueryRuntimeException("Column not found", selectExpressionItem);
            }
            else {
                Alias columnAlias = selectExpressionItem.getAlias();
                if (columnAlias == null || columnAlias.getName() == null)
                    throw new InvalidSelectQueryRuntimeException("Complex expression in SELECT must have an alias", selectExpressionItem);

                QuotedID name = idfac.createAttributeID(columnAlias.getName());
                //Variable var = TERM_FACTORY.getVariable(name.getName() + relationIndex);

                Term term =  new ExpressionParser(idfac, attributes).parseTerm(expr);
                map = ImmutableMap.of(new QualifiedAttributeID(null, name), term);
            }
        }
    }
}
