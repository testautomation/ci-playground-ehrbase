/*
 * Copyright (c) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.aql.sql.binding;

import org.ehrbase.aql.definition.I_VariableDefinition;
import org.ehrbase.aql.sql.queryImpl.CompositionAttributeQuery;
import org.ehrbase.aql.sql.queryImpl.I_QueryImpl;
import org.ehrbase.aql.sql.queryImpl.JsonbEntryQuery;
import org.jooq.Field;

/**
 * convert a field that is not identied as an EHR or a COMPOSITION (content or attribute). For example a CLUSTER
 * in other_context
 */
public class ContextualAttribute {

    private final CompositionAttributeQuery compositionAttributeQuery;
    private final JsonbEntryQuery jsonbEntryQuery;
    private final I_QueryImpl.Clause clause;

    private boolean containsJsonDataBlock;
    private String jsonbItemPath;
    private String optionalPath;

    public ContextualAttribute(CompositionAttributeQuery compositionAttributeQuery, JsonbEntryQuery jsonbEntryQuery, I_QueryImpl.Clause clause) {
        this.compositionAttributeQuery = compositionAttributeQuery;
        this.jsonbEntryQuery = jsonbEntryQuery;
        this.clause = clause;
    }

    public Field<?> toSql(String template_id, I_VariableDefinition variableDefinition){
        String inTemplatePath = compositionAttributeQuery.variableTemplatePath(template_id, variableDefinition.getIdentifier());
        if (inTemplatePath.startsWith("/"))
            inTemplatePath = inTemplatePath.substring(1); //conventionally, composition attribute path have the leading '/' striped.
        String originalPath = variableDefinition.getPath();
        variableDefinition.setPath(inTemplatePath+(variableDefinition.getPath() == null? "": "/"+variableDefinition.getPath()));
        CompositionAttribute compositionAttribute = new CompositionAttribute(compositionAttributeQuery, jsonbEntryQuery, clause);
        Field field = compositionAttribute.toSql(variableDefinition, template_id, variableDefinition.getIdentifier());

        if (clause.equals(I_QueryImpl.Clause.SELECT)) {
            variableDefinition.setPath(originalPath);
            field = field.as("/"+originalPath);
        }

        jsonbItemPath = compositionAttribute.getJsonbItemPath();
        containsJsonDataBlock = compositionAttribute.isContainsJsonDataBlock();
        optionalPath = compositionAttribute.getOptionalPath();

        return field;
    }

    public boolean isContainsJsonDataBlock() {
        return containsJsonDataBlock;
    }

    public String getJsonbItemPath() {
        return jsonbItemPath;
    }

    public String getOptionalPath() {
        return optionalPath;
    }
}
