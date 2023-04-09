package com.epam.service;

import com.epam.dto.EmployeeDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.epam.util.Constants.DELETE_METHOD;
import static com.epam.util.Constants.EMPLOYEES_DOCS_ENDPOINT_TEMPLATE;
import static com.epam.util.Constants.EMPLOYEES_SEARCH_ENDPOINT;
import static com.epam.util.Constants.GET_METHOD;
import static com.epam.util.Constants.POST_METHOD;
import static com.epam.util.Constants.PUT_METHOD;

@Slf4j
@Service("low-level-service")
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private static final String FIND_QUERY_PREFIX = """
            {
            \t"query": {
            \t\t"bool": {
            \t\t\t"must": [""";
    private static final String FIND_QUERY_POSTFIX = """
                    }
                }
            }
            """;
    private static final String AGGREGATE_QUERY_TEMPLATE = """
            {
                "size": 0,
                "aggs": {
                    "%s": {
                        "terms": {\s
                            "field": "%s.keyword",
                            "order": {
                                "rating_stats.%s": "%s"
                            }
                        },
                        "aggs": {
                            "rating_stats": {\s
                                "stats": {\s
                                    "field": "%s"\s
                                }\s
                            }
                        }
                    }
                }
            }
            """;

    private final RestClient restClient;
    private final ObjectMapper mapper;

    private static String getResponseBody(Response response) throws IOException {
        return EntityUtils.toString(response.getEntity());
    }

    @Override
    public Collection<EmployeeDto> findAll() throws IOException {
        var request = new Request(GET_METHOD, EMPLOYEES_SEARCH_ENDPOINT);
        var response = restClient.performRequest(request);
        var responseBody = getResponseBody(response);

        return getEmployeesFromResponse(responseBody);
    }

    @Override
    public Optional<EmployeeDto> findById(String id) throws IOException {
        var path = String.format(EMPLOYEES_DOCS_ENDPOINT_TEMPLATE, id);
        var request = new Request(GET_METHOD, path);

        Response response;
        try {
            response = restClient.performRequest(request);
        } catch (ResponseException e) {
            log.warn("Employee wasn't found by id [{}]", id);
            return Optional.empty();
        }

        var responseBody = getResponseBody(response);
        return getEmployeeFromResponse(responseBody);
    }

    @Override
    public void create(EmployeeDto employee, String id) throws IOException {
        if (id.isEmpty()) {
            throw new IllegalArgumentException("Id should be present for inserting document!");
        }
        var path = String.format(EMPLOYEES_DOCS_ENDPOINT_TEMPLATE, id);
        var request = new Request(PUT_METHOD, path);
        var jsonEntity = mapper.writeValueAsString(employee);
        request.setJsonEntity(jsonEntity);
        restClient.performRequest(request);
    }

    @Override
    public void delete(String id) throws IOException {
        var path = String.format(EMPLOYEES_DOCS_ENDPOINT_TEMPLATE, id);
        var request = new Request(DELETE_METHOD, path);
        restClient.performRequest(request);
    }

    @Override
    public Collection<EmployeeDto> find(MultiValueMap<String, String> params) throws IOException {
        var query = new StringBuilder();
        query.append(FIND_QUERY_PREFIX);

        var iterator = params.entrySet().iterator();
        while (iterator.hasNext()) {
            appendTerm(query, iterator.next(), iterator.hasNext());
        }
        query.append(FIND_QUERY_POSTFIX);

        var request = new Request(GET_METHOD, EMPLOYEES_SEARCH_ENDPOINT);
        request.setJsonEntity(query.toString());

        var response = restClient.performRequest(request);
        var responseBody = getResponseBody(response);
        return getEmployeesFromResponse(responseBody);
    }

    @Override
    public String aggregate(Map<String, String> params) throws IOException {
        var aggField = params.get("agg_field");
        var metricType = params.get("metric_type");
        var metricField = params.get("metric_field");
        var sortOrder = params.get("sort_order");
        var query = String.format(AGGREGATE_QUERY_TEMPLATE, aggField, aggField, metricType, sortOrder, metricField);

        var request = new Request(POST_METHOD, EMPLOYEES_SEARCH_ENDPOINT);
        request.setJsonEntity(query);

        var response = restClient.performRequest(request);
        var responseBody = getResponseBody(response);

        return mapper.readTree(responseBody)
                .findValues("buckets")
                .toString();
    }

    private Collection<EmployeeDto> getEmployeesFromResponse(String responseBody) throws IOException {
        var nodes = mapper.readTree(responseBody)
                .findValue("hits")
                .findPath("hits")
                .findValues("_source");

        return nodes.stream()
                .map(this::getEmployeeFromNode)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private Optional<EmployeeDto> getEmployeeFromNode(JsonNode node) {
        var reader = mapper.readerFor(new TypeReference<EmployeeDto>() {
        });

        try {
            EmployeeDto employee = reader.readValue(node);
            return Optional.of(employee);
        } catch (IOException e) {
            log.error("Node [{}] can't be parsed to EmployeeDto.class.", node.asText());
            return Optional.empty();
        }
    }

    private Optional<EmployeeDto> getEmployeeFromResponse(String responseBody) throws JsonProcessingException {
        var node = mapper.readTree(responseBody).findPath("_source");
        return getEmployeeFromNode(node);
    }

    private void appendTerm(StringBuilder query, Map.Entry<String, List<String>> map, boolean hasNext) {
        var values = map.getValue();

        if (!values.isEmpty()) {
            query.append(" {")
                    .append("\n")
                    .append("\t\t\t\t\"terms\" : {")
                    .append("\n")
                    .append("          \"")
                    .append(map.getKey())
                    .append("\": [ ");
            var valuesIterator = values.iterator();
            while (valuesIterator.hasNext()) {
                query.append("\"")
                        .append(valuesIterator.next())
                        .append("\"");
                if (valuesIterator.hasNext()) {
                    query.append(", ");
                }
            }
            query.append(" ]")
                    .append("\n")
                    .append("\t\t\t\t}")
                    .append("\n")
                    .append("\t\t\t}");

            if (hasNext) {
                query.append(",");
            } else {
                query.append(" ]\n");
            }
        } else {
            throw new IllegalArgumentException("Bad request: Value(s) is empty");
        }
    }
}
