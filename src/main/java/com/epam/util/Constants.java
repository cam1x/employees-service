package com.epam.util;

public class Constants {

    public static final String GET_METHOD = "GET";
    public static final String POST_METHOD = "POST";
    public static final String PUT_METHOD = "PUT";
    public static final String DELETE_METHOD = "DELETE";

    public static final String EMPLOYEES_INDEX = "employees";
    public static final String EMPLOYEES_ENDPOINT = "/" + EMPLOYEES_INDEX;
    public static final String EMPLOYEES_DOCS_ENDPOINT_TEMPLATE = EMPLOYEES_ENDPOINT + "/_doc/%s";
    public static final String EMPLOYEES_SEARCH_ENDPOINT = EMPLOYEES_ENDPOINT + "/_search";
}
