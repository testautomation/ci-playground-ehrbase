/*
 * Copyright (c) 2019 Vitasystems GmbH and Hannover Medical School.
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

package org.ehrbase.aql.sql.queryImpl;

import org.ehrbase.aql.sql.queryImpl.value_field.NodePredicate;
import org.ehrbase.ehr.util.LocatableHelper;

import java.util.List;

/**
 * Created by christian on 5/3/2018.
 */
public class VariablePath {

    String path;

    public VariablePath(String path) {
        this.path = path;
    }

    public boolean hasPredicate() {

        if (path == null)
            return false;

        List<String> segments = LocatableHelper.dividePathIntoSegments(path);
        for (int i = 0; i < segments.size(); i++) {
            String nodeId = segments.get(i);
            if (new NodePredicate(nodeId).hasPredicate())
                return true;
        }

        return false;
    }
}
