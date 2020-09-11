/*
 * Copyright (c) 2019 Vitasystems GmbH and Christian Chevalley (Hannover Medical School).
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
package org.ehrbase.aql.sql.queryImpl.attribute.eventcontext.participations;

import org.ehrbase.aql.sql.queryImpl.attribute.FieldResolutionContext;
import org.ehrbase.aql.sql.queryImpl.attribute.I_RMObjectAttribute;
import org.ehrbase.aql.sql.queryImpl.attribute.JoinSetup;
import org.ehrbase.aql.sql.queryImpl.attribute.eventcontext.EventContextAttribute;
import org.ehrbase.aql.sql.queryImpl.attribute.eventcontext.EventContextJson;
import org.ehrbase.aql.sql.queryImpl.value_field.GenericJsonField;
import org.jooq.Field;
import org.jooq.TableField;

import java.util.Optional;

import static org.ehrbase.jooq.pg.Tables.PARTY_IDENTIFIED;
import static org.ehrbase.jooq.pg.tables.EventContext.EVENT_CONTEXT;

public class ParticipationsJson extends EventContextAttribute {

    protected Optional<String> jsonPath = Optional.empty();

    public ParticipationsJson(FieldResolutionContext fieldContext, JoinSetup joinSetup) {
        super(fieldContext, joinSetup);
    }

    //TODO: participations is actually an ARRAY. GenericJsonField needs to support querying on Json array
    @Override
    public Field<?> sqlField() {
        fieldContext.setJsonDatablock(true);
        if (jsonPath.isPresent())
            return new GenericJsonField(fieldContext, joinSetup).forJsonPath(jsonPath.get()).jsonField("PARTICIPATION","ehr.js_participations", EVENT_CONTEXT.ID);
        else
            return new GenericJsonField(fieldContext, joinSetup).jsonField("PARTICIPATION","ehr.js_canonical_participations", EVENT_CONTEXT.ID);
    }

    @Override
    public I_RMObjectAttribute forTableField(TableField tableField) {
        return this;
    }

    public ParticipationsJson forJsonPath(String jsonPath){
        if (jsonPath == null || jsonPath.isEmpty()) {
            this.jsonPath = Optional.empty();
            return this;
        }
        this.jsonPath = Optional.of(jsonPath);
        return this;
    }

}
