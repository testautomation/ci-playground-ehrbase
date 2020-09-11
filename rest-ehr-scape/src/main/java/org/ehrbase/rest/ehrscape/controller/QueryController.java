/*
 * Copyright (c) 2019 Stefan Spiska (Vitasystems GmbH) and Jake Smolka (Hannover Medical School).
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

package org.ehrbase.rest.ehrscape.controller;

import org.ehrbase.api.definitions.QueryMode;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.service.QueryService;
import org.ehrbase.rest.ehrscape.responsedata.Action;
import org.ehrbase.rest.ehrscape.responsedata.QueryResponseData;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping(path = "/rest/ecis/v1/query", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
public class QueryController extends BaseController {

    private final QueryService queryService;

    @Autowired
    public QueryController(QueryService queryService) {
        this.queryService = Objects.requireNonNull(queryService);
    }

    @PostMapping
    @ApiOperation(value = "Execute query")
    public ResponseEntity<QueryResponseData> query(@ApiParam(value = "Request to return the generated SQL (boolean).") @RequestParam(value = "explain", defaultValue = "false") Boolean explain,
                                                   @ApiParam(value = "Query") @RequestBody() String content) {

        Map<String, String> kvPairs = extractQuery(new String(content.getBytes()));

        final String queryString;
        final QueryMode queryMode;
        if (kvPairs.containsKey(QueryMode.AQL.getCode())) {
            queryMode = QueryMode.AQL;
            queryString = kvPairs.get(QueryMode.AQL.getCode());
        } else if (kvPairs.containsKey(QueryMode.SQL.getCode())) {
            queryMode = QueryMode.SQL;
            queryString = kvPairs.get(QueryMode.SQL.getCode());
        } else {
            throw new InvalidApiParameterException("No query parameter supplied");
        }
        QueryResponseData responseData = new QueryResponseData(queryService.query(queryString, queryMode, explain));
        responseData.setAction(Action.EXECUTE);
        return ResponseEntity.ok(responseData);
    }


    private static Map<String, String> extractQuery(String content) {
        Pattern patternKey = Pattern.compile("(?<=\\\")(.*?)(?=\")");
        Matcher matcherKey = patternKey.matcher(content);

        if (matcherKey.find()) {
            String type = matcherKey.group(1);
            String query = content.substring(content.indexOf(':') + 1, content.lastIndexOf('\"'));
            query = query.substring(query.indexOf('\"') + 1);
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put(type.toLowerCase(), query);
            return queryMap;
        } else
            throw new IllegalArgumentException("Could not identified query type (sql or aql) in content:" + content);

    }
}
