package com.epam.controller;

import com.epam.service.EmployeeService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/employees")
public class EmployeeV1Controller extends EmployeeBaseController {

    public EmployeeV1Controller(@Qualifier("low-level-service") EmployeeService service) {
        super(service);
    }
}
