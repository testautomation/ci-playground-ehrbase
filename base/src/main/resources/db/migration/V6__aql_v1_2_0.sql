/*
 * Modifications copyright (C) 2019 Vitasystems GmbH and Hannover Medical School

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

CREATE OR REPLACE FUNCTION ehr.aql_node_name_predicate(entry JSONB, name_value_predicate TEXT, jsonb_path TEXT)
  RETURNS JSONB AS
  $$
  DECLARE
    entry_segment JSONB;
    jsquery_node_expression TEXT;
    subnode JSONB;
  BEGIN

    -- get the segment for the predicate

    SELECT jsonb_extract_path(entry, VARIADIC string_to_array(jsonb_path, ',')) INTO STRICT entry_segment;

    IF (entry_segment IS NULL) THEN
      RETURN NULL ;
    END IF ;

    -- identify structure with name/value matching argument
    IF (jsonb_typeof(entry_segment) <> 'array') THEN
      RETURN NULL;
    END IF;

    FOR subnode IN SELECT jsonb_array_elements(entry_segment)
      LOOP
        IF ((subnode #>> '{/name,0,value}') = name_value_predicate) THEN
          RETURN subnode;
        END IF;
      END LOOP;

    RETURN NULL;

  END
  $$
LANGUAGE plpgsql;