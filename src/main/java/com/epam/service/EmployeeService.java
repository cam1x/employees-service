package com.epam.service;

import com.epam.dto.EmployeeDto;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface EmployeeService {

    Collection<EmployeeDto> findAll() throws IOException;

    Optional<EmployeeDto> findById(String id) throws IOException;

    void create(EmployeeDto employee, String id) throws IOException;

    void delete(String id) throws IOException;

    Collection<EmployeeDto> find(MultiValueMap<String, String> params) throws IOException;

    String aggregate(Map<String, String> params) throws IOException;
}
