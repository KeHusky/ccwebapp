package com.example.webapp.controller;

import com.example.webapp.dao.RecipieRepository;
import com.example.webapp.entities.DummyRecipie;
import com.example.webapp.entities.Recipie;
import com.example.webapp.entities.User;
import com.example.webapp.helpers.Helper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

@RestController
@Component
@Service
public class RecipieController {

    @Autowired
    private RecipieRepository recipieRepository;

    @Autowired
    Helper helper;

    Gson gson = new Gson();
    JsonParser jsonParser = new JsonParser();
    JsonObject jsonObject = new JsonObject();

    @RequestMapping(value = "/v1/recipie", method = RequestMethod.POST, produces = "application/json")
    protected String postRecipie(@RequestBody String recipie_string, HttpServletRequest request, HttpServletResponse response) {

        DummyRecipie dummyRecipie = gson.fromJson(recipie_string, DummyRecipie.class);

        if (dummyRecipie.getCook_time_in_min() == null)
            jsonObject.addProperty("error message", "cook_time_in_min not contained");
        else if (Integer.parseInt(dummyRecipie.getCook_time_in_min()) % 5 != 0)
            jsonObject.addProperty("error message", "cook_time_in_min is not multiple of 5");
        if (dummyRecipie.getPrep_time_in_min() == null)
            jsonObject.addProperty("error message", "prep_time_in_min not contained");
        else if (Integer.parseInt(dummyRecipie.getPrep_time_in_min()) % 5 != 0)
            jsonObject.addProperty("error message", "prep_time_in_min is not multiple of 5");
        if (dummyRecipie.getTitle() == null)
            jsonObject.addProperty("error message", "title not contained");
        if (dummyRecipie.getTitle() == null)
            jsonObject.addProperty("error message", "cusine not contained");
        if (dummyRecipie.getServings() == null)
            jsonObject.addProperty("error message", "servings not contained");
        else if (Integer.parseInt(dummyRecipie.getServings()) < 1)
            jsonObject.addProperty("error message", "servings minimum is 1");
        else if (Integer.parseInt(dummyRecipie.getServings()) > 5)
            jsonObject.addProperty("error message", "servings maximum is 5");
        if (dummyRecipie.getIngredients() == null)
            jsonObject.addProperty("error message", "ingredients not contained");
        if (dummyRecipie.getSteps() == null)
            jsonObject.addProperty("error message", "steps not contained");
        else if (dummyRecipie.getSteps().length < 1)
            jsonObject.addProperty("error message", "steps minimum is 1");
        if (dummyRecipie.getNutrition_information() == null)
            jsonObject.addProperty("error message", "nutrition_information not contained");

        if (jsonObject.get("error message") != null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return jsonObject.toString();
        }

        String header = request.getHeader("Authorization");
        if (header == null) {
            jsonObject.addProperty("error message", "no auth");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        } else {
            User user = helper.validateUser(header);
            if (user == null) {
                jsonObject.addProperty("error message", "wrong username or password");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            } else {

                String now = new Date().toString();
                Recipie recipie = new Recipie();
                recipie.setCreated_ts(now);
                recipie.setUpdated_ts(now);
                recipie.setAuthor_id(user.getID());
                recipie.setCook_time_in_min(Integer.parseInt(dummyRecipie.getCook_time_in_min()));
                recipie.setPrep_time_in_min(Integer.parseInt(dummyRecipie.getPrep_time_in_min()));
                recipie.setTotal_time_in_min(recipie.getCook_time_in_min() + recipie.getPrep_time_in_min());
                recipie.setTitle(dummyRecipie.getTitle());
                recipie.setCusine(dummyRecipie.getCusine());
                recipie.setServings(Integer.parseInt(dummyRecipie.getServings()));
                recipie.setIngredients(gson.toJson(dummyRecipie.getIngredients()));
                recipie.setSteps(gson.toJson(dummyRecipie.getSteps()));
                recipie.setNutrition_information(gson.toJson(dummyRecipie.getNutrition_information()));

                recipieRepository.save(recipie);
                jsonObject.addProperty("id", recipie.getID());
                jsonObject.addProperty("created_ts", recipie.getCreated_ts());
                jsonObject.addProperty("updated_ts", recipie.getUpdated_ts());
                jsonObject.addProperty("author_id", recipie.getAuthor_id());
                jsonObject.addProperty("cook_time_in_min", recipie.getCook_time_in_min());
                jsonObject.addProperty("prep_time_in_time", recipie.getPrep_time_in_min());
                jsonObject.addProperty("total_time_in_min", recipie.getTotal_time_in_min());
                jsonObject.addProperty("title", recipie.getTitle());
                jsonObject.addProperty("cusine", recipie.getCusine());
                jsonObject.addProperty("servings", recipie.getServings());
                jsonObject.addProperty("ingrediets", recipie.getIngredients());
                jsonObject.addProperty("steps", recipie.getSteps());
                jsonObject.addProperty("nutrition_information", recipie.getNutrition_information());

                jsonObject.add("ingrediets", jsonParser.parse(recipie.getIngredients()).getAsJsonArray());
                jsonObject.add("steps", jsonParser.parse(recipie.getSteps()).getAsJsonArray());
                jsonObject.add("nutrition_information", jsonParser.parse(recipie.getNutrition_information()).getAsJsonObject());
                response.setStatus(HttpServletResponse.SC_CREATED);
            }
        }
        return jsonObject.toString();
    }

    @RequestMapping(value = "/v1/recipie/{id}", method = RequestMethod.DELETE, produces = "application/json")
    protected String deleteRecipie(HttpServletRequest request, HttpServletResponse response) {

        JsonObject jsonObject = new JsonObject();
        String recipieID = request.getRequestURI().split("/")[3];
        String header = request.getHeader("Authorization");

        if (header == null) {
            jsonObject.addProperty("error message", "no auth");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        } else {
            User user = helper.validateUser(header);
            if (user == null) {
                jsonObject.addProperty("error message", "wrong username or password");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            } else {
                Optional<Recipie> r = recipieRepository.findById(recipieID);
                Recipie recipie = r.isPresent() ? r.get() : null;
                if (recipie == null) {
                    jsonObject.addProperty("error message", "no such recipie");
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                } else if (!recipie.getAuthor_id().equals(user.getID())) {
                    jsonObject.addProperty("error message", "no access");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                } else {
                    recipieRepository.delete(recipie);
                    response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                }
            }

        }

        return jsonObject.toString();

    }

    @RequestMapping(value = "/v1/recipie/{id}", method = RequestMethod.PUT, produces = "application/json")
    protected String putRecipie(@RequestBody String recipie_string, HttpServletRequest request, HttpServletResponse response) {

        DummyRecipie dummyRecipie = gson.fromJson(recipie_string, DummyRecipie.class);

        if (dummyRecipie.getCook_time_in_min() == null)
            jsonObject.addProperty("error message", "cook_time_in_min not contained");
        else if (Integer.parseInt(dummyRecipie.getCook_time_in_min()) % 5 != 0)
            jsonObject.addProperty("error message", "cook_time_in_min is not multiple of 5");
        if (dummyRecipie.getPrep_time_in_min() == null)
            jsonObject.addProperty("error message", "prep_time_in_min not contained");
        else if (Integer.parseInt(dummyRecipie.getPrep_time_in_min()) % 5 != 0)
            jsonObject.addProperty("error message", "prep_time_in_min is not multiple of 5");
        if (dummyRecipie.getTitle() == null)
            jsonObject.addProperty("error message", "title not contained");
        if (dummyRecipie.getTitle() == null)
            jsonObject.addProperty("error message", "cusine not contained");
        if (dummyRecipie.getServings() == null)
            jsonObject.addProperty("error message", "servings not contained");
        else if (Integer.parseInt(dummyRecipie.getServings()) < 1)
            jsonObject.addProperty("error message", "servings minimum is 1");
        else if (Integer.parseInt(dummyRecipie.getServings()) > 5)
            jsonObject.addProperty("error message", "servings maximum is 5");
        if (dummyRecipie.getIngredients() == null)
            jsonObject.addProperty("error message", "ingredients not contained");
        if (dummyRecipie.getSteps() == null)
            jsonObject.addProperty("error message", "steps not contained");
        else if (dummyRecipie.getSteps().length < 1)
            jsonObject.addProperty("error message", "steps minimum is 1");
        if (dummyRecipie.getNutrition_information() == null)
            jsonObject.addProperty("error message", "nutrition_information not contained");

        if (jsonObject.get("error message") != null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return jsonObject.toString();
        }

        JsonObject jsonObject = new JsonObject();
        String recipieID = request.getRequestURI().split("/")[3];
        String header = request.getHeader("Authorization");

        if (header == null) {
            jsonObject.addProperty("error message", "no auth");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        } else {
            User user = helper.validateUser(header);
            if (user == null) {
                jsonObject.addProperty("error message", "wrong username or password");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            } else {
                Optional<Recipie> r = recipieRepository.findById(recipieID);
                Recipie recipie = r.isPresent() ? r.get() : null;
                if (recipie == null) {
                    jsonObject.addProperty("error message", "no such recipie");
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                } else if (!recipie.getAuthor_id().equals(user.getID())) {
                    jsonObject.addProperty("error message", "no access");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                } else {

                    recipie.setUpdated_ts(new Date().toString());
                    recipie.setCook_time_in_min(Integer.parseInt(dummyRecipie.getCook_time_in_min()));
                    recipie.setPrep_time_in_min(Integer.parseInt(dummyRecipie.getPrep_time_in_min()));
                    recipie.setTotal_time_in_min(recipie.getCook_time_in_min() + recipie.getPrep_time_in_min());
                    recipie.setTitle(dummyRecipie.getTitle());
                    recipie.setCusine(dummyRecipie.getCusine());
                    recipie.setServings(Integer.parseInt(dummyRecipie.getServings()));
                    recipie.setIngredients(gson.toJson(dummyRecipie.getIngredients()));
                    recipie.setSteps(gson.toJson(dummyRecipie.getSteps()));
                    recipie.setNutrition_information(gson.toJson(dummyRecipie.getNutrition_information()));

                    recipieRepository.save(recipie);
                    jsonObject.addProperty("id", recipie.getID());
                    jsonObject.addProperty("created_ts", recipie.getCreated_ts());
                    jsonObject.addProperty("updated_ts", recipie.getUpdated_ts());
                    jsonObject.addProperty("author_id", recipie.getAuthor_id());
                    jsonObject.addProperty("cook_time_in_min", recipie.getCook_time_in_min());
                    jsonObject.addProperty("prep_time_in_time", recipie.getPrep_time_in_min());
                    jsonObject.addProperty("total_time_in_min", recipie.getTotal_time_in_min());
                    jsonObject.addProperty("title", recipie.getTitle());
                    jsonObject.addProperty("cusine", recipie.getCusine());
                    jsonObject.addProperty("servings", recipie.getServings());
                    jsonObject.addProperty("ingrediets", recipie.getIngredients());
                    jsonObject.addProperty("steps", recipie.getSteps());
                    jsonObject.addProperty("nutrition_information", recipie.getNutrition_information());

                    jsonObject.add("ingrediets", jsonParser.parse(recipie.getIngredients()).getAsJsonArray());
                    jsonObject.add("steps", jsonParser.parse(recipie.getSteps()).getAsJsonArray());
                    jsonObject.add("nutrition_information", jsonParser.parse(recipie.getNutrition_information()).getAsJsonObject());

                    response.setStatus(HttpServletResponse.SC_OK);
                }
            }

        }

        return jsonObject.toString();

    }

    @RequestMapping(value = "/v1/recipie/{id}", method = RequestMethod.GET, produces = "application/json")
    public String getRecipie(HttpServletRequest request, HttpServletResponse response) {

        JsonObject jsonObject = new JsonObject();

        String recipieID = request.getRequestURI().split("/")[3];
        String header = request.getHeader("Authorization");

        Optional<Recipie> r = recipieRepository.findById(recipieID);
        Recipie recipie = r.isPresent() ? r.get() : null;
        if (recipie == null) {
            jsonObject.addProperty("error message", "no such recipie");
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } else {
            jsonObject.addProperty("id", recipie.getID());
            jsonObject.addProperty("created_ts", recipie.getCreated_ts());
            jsonObject.addProperty("updated_ts", recipie.getUpdated_ts());
            jsonObject.addProperty("author_id", recipie.getAuthor_id());
            jsonObject.addProperty("cook_time_in_min", recipie.getCook_time_in_min());
            jsonObject.addProperty("prep_time_in_time", recipie.getPrep_time_in_min());
            jsonObject.addProperty("total_time_in_min", recipie.getTotal_time_in_min());
            jsonObject.addProperty("title", recipie.getTitle());
            jsonObject.addProperty("cusine", recipie.getCusine());
            jsonObject.addProperty("servings", recipie.getServings());
            jsonObject.addProperty("ingrediets", recipie.getIngredients());
            jsonObject.addProperty("steps", recipie.getSteps());
            jsonObject.addProperty("nutrition_information", recipie.getNutrition_information());

            jsonObject.add("ingrediets", jsonParser.parse(recipie.getIngredients()).getAsJsonArray());
            jsonObject.add("steps", jsonParser.parse(recipie.getSteps()).getAsJsonArray());
            jsonObject.add("nutrition_information", jsonParser.parse(recipie.getNutrition_information()).getAsJsonObject());

            response.setStatus(HttpServletResponse.SC_OK);
        }

        return jsonObject.toString();

    }

}
