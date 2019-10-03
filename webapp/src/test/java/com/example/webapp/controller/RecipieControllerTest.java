package com.example.webapp.controller;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.util.Base64;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class RecipieControllerTest {

    @Autowired
    RecipieController recipieController;

    MockHttpServletRequest request;
    MockHttpServletResponse response;
    String recipie;

    @Before
    public void setUp() {
        String username = "yuan@husky.edu";
        String password = "123abcABC";
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Basic " + Base64.getUrlEncoder().encodeToString((username + ":" + password).getBytes()));
        request.setCharacterEncoding("UTF-8");

        recipie = "{\n" +
                "  \"cook_time_in_min\": 15,\n" +
                "  \"prep_time_in_min\": 15,\n" +
                "  \"title\": \"Creamy Cajun Chicken Pasta\",\n" +
                "  \"cusine\": \"Italian\",\n" +
                "  \"servings\": 2,\n" +
                "  \"ingredients\": [\n" +
                "    \"4 ounces linguine pasta\",\n" +
                "    \"2 boneless, skinless chicken breast halves, sliced into thin strips\",\n" +
                "    \"2 teaspoons Cajun seasoning\",\n" +
                "    \"2 tablespoons butter\"\n" +
                "  ],\n" +
                "  \"steps\": [\n" +
                "    {\n" +
                "      \"position\": 1,\n" +
                "      \"items\": \"some text here\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"nutrition_information\": {\n" +
                "    \"calories\": 100,\n" +
                "    \"cholesterol_in_mg\": 4,\n" +
                "    \"sodium_in_mg\": 100,\n" +
                "    \"carbohydrates_in_grams\": 53.7,\n" +
                "    \"protein_in_grams\": 53.7\n" +
                "  }\n" +
                "}";

    }

    @Test
    @Transactional
    public void postRecipie() {
        String result = recipieController.postRecipie(recipie, request, response);
        assertEquals(true, result != null);
        assertEquals(HttpServletResponse.SC_CREATED, response.getStatus());
    }

    @Test
    @Transactional
    public void deleteRecipie() {
        request.setRequestURI("/v1/recipie/50196ced-5e36-4230-9633-b1a93f09b6cc");
        String header = request.getHeader("Authorization");
        System.out.println(header);
        recipieController.deleteRecipie(request, response);
        assertEquals(HttpServletResponse.SC_NO_CONTENT, response.getStatus());
    }

    @Test
    @Transactional
    public void putRecipie() {
        request.setRequestURI("/v1/recipie/50196ced-5e36-4230-9633-b1a93f09b6cc");
        recipie = "{\n" +
                "  \"cook_time_in_min\": 15,\n" +
                "  \"prep_time_in_min\": 15,\n" +
                "  \"title\": \"Creamy Cajun Chicken Pasta\",\n" +
                "  \"cusine\": \"Italian\",\n" +
                "  \"servings\": 2,\n" +
                "  \"ingredients\": [\n" +
                "    \"4 ounces linguine pasta\",\n" +
                "    \"2 boneless, skinless chicken breast halves, sliced into thin strips\",\n" +
                "    \"2 teaspoons Cajun seasoning\",\n" +
                "    \"2 tablespoons butter\"\n" +
                "  ],\n" +
                "  \"steps\": [\n" +
                "    {\n" +
                "      \"position\": 1,\n" +
                "      \"items\": \"updated\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"nutrition_information\": {\n" +
                "    \"calories\": 100,\n" +
                "    \"cholesterol_in_mg\": 4,\n" +
                "    \"sodium_in_mg\": 100,\n" +
                "    \"carbohydrates_in_grams\": 53.7,\n" +
                "    \"protein_in_grams\": 53.7\n" +
                "  }\n" +
                "}";
        recipieController.putRecipie(recipie, request, response);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    }

    @Test
    @Transactional
    public void getRecipie() {
        request.setRequestURI("/v1/recipie/50196ced-5e36-4230-9633-b1a93f09b6cc");
        recipieController.getRecipie(request, response);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    }
}