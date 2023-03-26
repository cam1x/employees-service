package com.epam.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.AverageAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.TermsAggregation;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.epam.dto.EmployeeDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.epam.util.Constants.EMPLOYEES_INDEX;

@Slf4j
@Service("api-service")
@RequiredArgsConstructor
public class EmployeeApiServiceImpl implements EmployeeService {

    private final ElasticsearchClient client;

    @Override
    public Collection<EmployeeDto> findAll() throws IOException {
        var searchResponse = client.search(
                s -> s.index(EMPLOYEES_INDEX),
                EmployeeDto.class
        );

        return searchResponse.hits().hits().stream()
                .map(Hit::source)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<EmployeeDto> findById(String id) throws IOException {
        var response = client.get(
                g -> g.index(EMPLOYEES_INDEX).id(id),
                EmployeeDto.class
        );

        if (response.found()) {
            var employee = response.source();
            return Optional.ofNullable(employee);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public void create(EmployeeDto employee, String id) throws IOException {
        var request = IndexRequest.of(i -> i
                .index(EMPLOYEES_INDEX)
                .id(id)
                .document(employee)
        );

        client.index(request);
    }

    @Override
    public void delete(String id) throws IOException {
        var request = DeleteRequest.of(i -> i
                .index(EMPLOYEES_INDEX)
                .id(id)
        );

        client.delete(request);
    }

    @Override
    public Collection<EmployeeDto> find(MultiValueMap<String, String> params) throws IOException {
        var iterator = params.entrySet().iterator();
        if (!iterator.hasNext()) {
            throw new IllegalArgumentException("Bad request: Value(s) is empty");
        }

        var map = iterator.next();
        if (!map.getValue().iterator().hasNext()) {
            throw new IllegalArgumentException("Bad request: Value(s) is empty");
        }

        var value = map.getValue().iterator().next();
        var response = client.search(s -> s
                        .index(EMPLOYEES_INDEX)
                        .query(q -> q
                                .term(t -> t
                                        .field(map.getKey())
                                        .value(value))),
                EmployeeDto.class);

        var totalHits = response.hits().total();
        if (totalHits == null) {
            throw new IllegalStateException("Failed to find documents");
        }

        return response.hits().hits().stream()
                .map(Hit::source)
                .collect(Collectors.toList());
    }

    @Override
    public String aggregate(Map<String, String> params) throws IOException {
        var aggField = params.get("agg_field");
        var metricField = params.get("metric_field");

        Map<String, Aggregation> metricAggregationReference = new HashMap<>();
        var subAggregation = new Aggregation.Builder()
                .avg(new AverageAggregation.Builder().field(metricField).build())
                .build();

        var aggregation = new Aggregation.Builder()
                .terms(new TermsAggregation.Builder().field(aggField + ".keyword").build())
                .aggregations(new HashMap<>() {{
                    put(metricField, subAggregation);
                }}).build();
        metricAggregationReference.put(metricField, aggregation);

        var request = new SearchRequest.Builder()
                .index(EMPLOYEES_INDEX)
                .size(0)
                .aggregations(metricAggregationReference)
                .build();

        SearchResponse<Void> response = client.search(request, Void.class);
        var buckets = response.aggregations()
                .get(metricField)
                .sterms()
                .buckets()
                .array();

        return buckets.stream()
                .toList()
                .toString();
    }
}
