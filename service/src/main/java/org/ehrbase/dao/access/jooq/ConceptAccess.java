/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School.

 * This file is part of Project EHRbase

 * Copyright (c) 2015 Christian Chevalley
 * This file is part of Project Ethercis
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
package org.ehrbase.dao.access.jooq;

import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.jooq.Record1;
import org.jooq.Result;

import java.util.UUID;

import static org.ehrbase.jooq.pg.Tables.CONCEPT;

/**
 * Created by Christian Chevalley on 4/10/2015.
 */
public class ConceptAccess {

    static public UUID fetchConceptUUID(I_DomainAccess domainAccess, Integer conceptId, String language) {
        Result<Record1<UUID>> uuids = domainAccess.getContext().select(CONCEPT.ID).from(CONCEPT).where(CONCEPT.CONCEPTID.eq(conceptId)).and(CONCEPT.LANGUAGE.equal(language)).fetch();
        return (UUID) uuids.get(0).getValue(0);
    }
}
