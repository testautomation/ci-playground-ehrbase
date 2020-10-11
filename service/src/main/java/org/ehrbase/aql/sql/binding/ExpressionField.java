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

import com.nedap.archie.rm.datavalues.DataValue;
import com.nedap.archie.rminfo.ArchieRMInfoLookup;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.aql.definition.I_VariableDefinition;
import org.ehrbase.aql.sql.queryImpl.CompositionAttributeQuery;
import org.ehrbase.aql.sql.queryImpl.DefaultColumnId;
import org.ehrbase.aql.sql.queryImpl.I_QueryImpl;
import org.ehrbase.aql.sql.queryImpl.JsonbEntryQuery;
import org.ehrbase.aql.sql.queryImpl.VariableAqlPath;
import org.jooq.Field;
import org.jooq.impl.DSL;

import java.util.Objects;

class ExpressionField {

    private final I_VariableDefinition variableDefinition;
    private final JsonbEntryQuery jsonbEntryQuery;
    private final CompositionAttributeQuery compositionAttributeQuery;
    private boolean containsJsonDataBlock = false;

    private String rootJsonKey = null;
    private String optionalPath = null;
    private String jsonbItemPath = null;

    public ExpressionField(I_VariableDefinition variableDefinition, JsonbEntryQuery jsonbEntryQuery, CompositionAttributeQuery compositionAttributeQuery) {
        this.variableDefinition = variableDefinition;
        this.jsonbEntryQuery = jsonbEntryQuery;
        this.compositionAttributeQuery = compositionAttributeQuery;
    }

    Field<?> toSql(String className, String template_id, String identifier) {

        Field<?> field;

        switch (className) {
            //COMPOSITION attributes
            case "COMPOSITION":
                CompositionAttribute compositionAttribute = new CompositionAttribute(compositionAttributeQuery, jsonbEntryQuery, I_QueryImpl.Clause.SELECT);
                field = compositionAttribute.toSql(variableDefinition, template_id, identifier);
                jsonbItemPath = compositionAttribute.getJsonbItemPath();
                containsJsonDataBlock = compositionAttribute.isContainsJsonDataBlock();
                optionalPath = compositionAttribute.getOptionalPath();
                break;
            // EHR attributes
            case "EHR":

                field = compositionAttributeQuery.makeField(template_id, identifier, variableDefinition, I_QueryImpl.Clause.SELECT);
                containsJsonDataBlock = compositionAttributeQuery.isJsonDataBlock();
                optionalPath = variableDefinition.getPath();
                break;
            // other, f.e. CLUSTER, ADMIN_ENTRY, OBSERVATION etc.
            default:
                // other_details f.e.
                if (compositionAttributeQuery.isCompositionAttributeItemStructure(template_id, variableDefinition.getIdentifier())) {
                    ContextualAttribute contextualAttribute = new ContextualAttribute(compositionAttributeQuery, jsonbEntryQuery, I_QueryImpl.Clause.SELECT);
                    field = contextualAttribute.toSql(template_id, variableDefinition);
                    jsonbItemPath = contextualAttribute.getJsonbItemPath();
                    containsJsonDataBlock = contextualAttribute.isContainsJsonDataBlock();
                    optionalPath = contextualAttribute.getOptionalPath();
                }
                else {
                    // all other that are supported as simpleClassExpr (most common resolution)
                    LocatableItem locatableItem = new LocatableItem(compositionAttributeQuery, jsonbEntryQuery, I_QueryImpl.Clause.SELECT);
                    field = locatableItem.toSql(template_id, variableDefinition, className);
                    jsonbItemPath = locatableItem.getJsonbItemPath();
                    containsJsonDataBlock = containsJsonDataBlock | locatableItem.isContainsJsonDataBlock();
                    optionalPath = locatableItem.getOptionalPath();
                    rootJsonKey = locatableItem.getRootJsonKey();
                    jsonbItemPath = locatableItem.getJsonbItemPath();
                }
                break;
        }

        return field;
    }

    private Field<?> _compositionAttributeToSql(String template_id, String identifier){
        Field<?> field;

        if (variableDefinition.getPath() != null && variableDefinition.getPath().startsWith("content")) {
            field = jsonbEntryQuery.makeField(template_id, identifier, variableDefinition, I_QueryImpl.Clause.SELECT);
            containsJsonDataBlock = jsonbEntryQuery.isJsonDataBlock();
            jsonbItemPath = jsonbEntryQuery.getJsonbItemPath();
        } else {
            field = compositionAttributeQuery.makeField(template_id, identifier, variableDefinition, I_QueryImpl.Clause.SELECT);
            containsJsonDataBlock = compositionAttributeQuery.isJsonDataBlock();
        }
        optionalPath = variableDefinition.getPath();
        return field;
    }

    private Field<?> _locatableItemToSql(String template_id, String identifier, String className){
        Field<?> field;

        field = jsonbEntryQuery.makeField(template_id, identifier, variableDefinition, I_QueryImpl.Clause.SELECT);
        jsonbItemPath = jsonbEntryQuery.getJsonbItemPath();
        containsJsonDataBlock = containsJsonDataBlock | jsonbEntryQuery.isJsonDataBlock();
        if (jsonbEntryQuery.isJsonDataBlock() ) {

            if (jsonbEntryQuery.getItemType() != null){
                Class itemClass = ArchieRMInfoLookup.getInstance().getClass(jsonbEntryQuery.getItemType());

                if (itemClass == null && className != null) //this may occur f.e. for itemType 'MULTIPLE'. try we classname
                    itemClass = ArchieRMInfoLookup.getInstance().getClass(className);

                if (DataValue.class.isAssignableFrom(itemClass)) {
                    VariableAqlPath variableAqlPath = new VariableAqlPath(variableDefinition.getPath());
                    if (variableAqlPath.getSuffix().equals("value")){
                        if (Objects.equals(className, "COMPOSITION")) { //assumes this is a data value within an ELEMENT
                            try {
                                I_VariableDefinition variableDefinition1 = variableDefinition.clone();
                                variableDefinition1.setPath(variableAqlPath.getInfix());
                                field = jsonbEntryQuery.makeField(template_id, identifier, variableDefinition1, I_QueryImpl.Clause.SELECT);
                                jsonbItemPath = jsonbEntryQuery.getJsonbItemPath();
                                rootJsonKey = variableAqlPath.getSuffix();
                            } catch (CloneNotSupportedException e) {
                                throw new InternalServerException("Couldn't handle variable:" + variableDefinition.toString() + "Code error:" + e);
                            }
                        }
                        else if (jsonbEntryQuery.getItemCategory().equals("ELEMENT") || jsonbEntryQuery.getItemCategory().equals("CLUSTER")){
                            int cut = jsonbItemPath.lastIndexOf(",/value");
                            if (cut != -1)
                                //we keep the path that select the json element value block, and call the formatting function
                                //to pass the actual value datatype into the json block
                                field = DSL.field("(ehr.js_typed_element_value(" + jsonbItemPath.substring(0, cut) + "}')::jsonb))");

                            String alias = variableDefinition.getAlias();
                            if (alias == null)
                                alias = new DefaultColumnId().value(variableDefinition);
                            field = field.as(alias);
                        }
                    }

                }
            }
        }
        return field;
    }



    boolean isContainsJsonDataBlock() {
        return containsJsonDataBlock;
    }

    String getRootJsonKey() {
        return rootJsonKey;
    }

    String getOptionalPath() {
        return optionalPath;
    }

    String getJsonbItemPath() {
        return jsonbItemPath;
    }
}
