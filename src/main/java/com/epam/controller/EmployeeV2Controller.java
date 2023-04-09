package com.epam.controller;

import com.epam.service.EmployeeService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/employees")
public class EmployeeV2Controller extends EmployeeBaseController {

    public EmployeeV2Controller(@Qualifier("api-service") EmployeeService service) {
        super(service);
    }
}
