package com.example.webapp.controller;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.example.webapp.dao.ImageRepository;
import com.example.webapp.dao.RecipeRepository;
import com.example.webapp.entities.DummyRecipe;
import com.example.webapp.entities.Image;
import com.example.webapp.entities.Recipe;
import com.example.webapp.entities.User;
import com.example.webapp.helpers.Helper;
import com.example.webapp.helpers.S3Hanlder;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

@RestController
public class RecipeController {

    @Autowired
    RecipeRepository recipeRepository;
    @Autowired
    ImageRepository imageRepository;
    @Autowired
    S3Hanlder s3Hanlder;
    Logger logger = LoggerFactory.getLogger(RecipeController.class);
    StatsDClient statsd = new NonBlockingStatsDClient("csye6225", "localhost", 8125);

    @Autowired
    Helper helper;

    Gson gson = new Gson();

    @Value("${TOPIC_ARN}")
    String TOPIC_ARN;
    @Value("${AWS_REGION}")
    String AWS_REGION;
    @Value("${ROUTE53}")
    String ROUTE53;
    @Value("${AWS_ACCESS_KEY_ID}")
    String AWS_ACCESS_KEY_ID;
    @Value("${AWS_SECRET_ACCESS_KEY}")
    String AWS_SECRET_ACCESS_KEY;

    @RequestMapping(value = "/v1/recipe", method = RequestMethod.POST, produces = "application/json")
    protected String postRecipe(@RequestBody String recipe_string, HttpServletRequest request, HttpServletResponse response) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("api");
        statsd.increment("recipe.POST");
        JsonObject jsonObject = new JsonObject();

        DummyRecipe dummyRecipe = gson.fromJson(recipe_string, DummyRecipe.class);

        if (dummyRecipe.getCook_time_in_min() == null)
            jsonObject.addProperty("error message", "cook_time_in_min not contained");
        else if (Integer.parseInt(dummyRecipe.getCook_time_in_min()) % 5 != 0)
            jsonObject.addProperty("error message", "cook_time_in_min is not multiple of 5");
        if (dummyRecipe.getPrep_time_in_min() == null)
            jsonObject.addProperty("error message", "prep_time_in_min not contained");
        else if (Integer.parseInt(dummyRecipe.getPrep_time_in_min()) % 5 != 0)
            jsonObject.addProperty("error message", "prep_time_in_min is not multiple of 5");
        if (dummyRecipe.getTitle() == null)
            jsonObject.addProperty("error message", "title not contained");
        if (dummyRecipe.getTitle() == null)
            jsonObject.addProperty("error message", "cusine not contained");
        if (dummyRecipe.getServings() == null)
            jsonObject.addProperty("error message", "servings not contained");
        else if (Integer.parseInt(dummyRecipe.getServings()) < 1)
            jsonObject.addProperty("error message", "servings minimum is 1");
        else if (Integer.parseInt(dummyRecipe.getServings()) > 5)
            jsonObject.addProperty("error message", "servings maximum is 5");
        if (dummyRecipe.getIngredients() == null)
            jsonObject.addProperty("error message", "ingredients not contained");
        if (dummyRecipe.getSteps() == null)
            jsonObject.addProperty("error message", "steps not contained");
        else if (dummyRecipe.getSteps().length < 1)
            jsonObject.addProperty("error message", "steps minimum is 1");
        if (dummyRecipe.getNutrition_information() == null)
            jsonObject.addProperty("error message", "nutrition_information not contained");

        if (jsonObject.get("error message") != null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            logger.info(jsonObject.get("error meaasage").getAsString());
            stopWatch.stop();
            statsd.recordExecutionTime("recipe.POST-api", stopWatch.getTotalTimeMillis());
            return jsonObject.toString();
        }

        String header = request.getHeader("Authorization");
        if (header == null) {
            jsonObject.addProperty("error message", "no auth");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            logger.info("no auth");
            stopWatch.stop();
            statsd.recordExecutionTime("recipe.POST-api", stopWatch.getTotalTimeMillis());
            return jsonObject.toString();
        }

        stopWatch.stop();
        stopWatch.start("sql");
        User user = helper.validateUser(header);
        stopWatch.stop();
        statsd.recordExecutionTime("recipe.POST-sql-1", stopWatch.getLastTaskTimeMillis());
        stopWatch.start("api");
        if (user == null) {
            jsonObject.addProperty("error message", "wrong username or password");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            logger.info("wrong username or password");
            stopWatch.stop();
            statsd.recordExecutionTime("recipe.POST-api", stopWatch.getTotalTimeMillis());
            return jsonObject.toString();
        }

        String now = new Date().toString();
        Recipe recipe = new Recipe();
        recipe.setCreated_ts(now);
        recipe.setUpdatedTs(now);
        recipe.setAuthorId(user.getId());
        recipe.setCook_time_in_min(Integer.parseInt(dummyRecipe.getCook_time_in_min()));
        recipe.setPrep_time_in_min(Integer.parseInt(dummyRecipe.getPrep_time_in_min()));
        recipe.setTotal_time_in_min(recipe.getCook_time_in_min() + recipe.getPrep_time_in_min());
        recipe.setTitle(dummyRecipe.getTitle());
        recipe.setCusine(dummyRecipe.getCusine());
        recipe.setServings(Integer.parseInt(dummyRecipe.getServings()));
        recipe.setIngredients(gson.toJson(dummyRecipe.getIngredients()));
        recipe.setSteps(gson.toJson(dummyRecipe.getSteps()));
        recipe.setNutrition_information(gson.toJson(dummyRecipe.getNutrition_information()));
        stopWatch.stop();
        stopWatch.start("sql");
        recipeRepository.save(recipe);
        stopWatch.stop();
        statsd.recordExecutionTime("recipe.POST-sql-2", stopWatch.getLastTaskTimeMillis());
        stopWatch.start("api");
        jsonObject.addProperty("id", recipe.getId());
        jsonObject.addProperty("created_ts", recipe.getCreated_ts());
        jsonObject.addProperty("updated_ts", recipe.getUpdatedTs());
        jsonObject.addProperty("author_id", recipe.getAuthorId());
        jsonObject.addProperty("cook_time_in_min", recipe.getCook_time_in_min());
        jsonObject.addProperty("prep_time_in_time", recipe.getPrep_time_in_min());
        jsonObject.addProperty("total_time_in_min", recipe.getTotal_time_in_min());
        jsonObject.addProperty("title", recipe.getTitle());
        jsonObject.addProperty("cusine", recipe.getCusine());
        jsonObject.addProperty("servings", recipe.getServings());
        jsonObject.addProperty("ingrediets", recipe.getIngredients());
        jsonObject.addProperty("steps", recipe.getSteps());
        jsonObject.addProperty("nutrition_information", recipe.getNutrition_information());

        jsonObject.add("ingrediets", gson.fromJson(recipe.getIngredients(), JsonArray.class));
        jsonObject.add("steps", gson.fromJson(recipe.getIngredients(), JsonArray.class));
        jsonObject.add("nutrition_information", gson.fromJson(recipe.getNutrition_information(), JsonObject.class));
        response.setStatus(HttpServletResponse.SC_CREATED);
        logger.info("recipe created");
        stopWatch.stop();
        statsd.recordExecutionTime("recipe.POST-api", stopWatch.getTotalTimeMillis());
        return jsonObject.toString();
    }

    @RequestMapping(value = "/v1/recipe/{id}", method = RequestMethod.DELETE, produces = "application/json")
    protected String deleteRecipe(HttpServletRequest request, HttpServletResponse response) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("api");
        statsd.increment("recipe.DELETE");

        JsonObject jsonObject = new JsonObject();
        String recipeId = request.getRequestURI().split("/")[3];
        String header = request.getHeader("Authorization");

        if (header == null) {
            jsonObject.addProperty("error message", "no auth");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            logger.info("no auth");
            stopWatch.stop();
            statsd.recordExecutionTime("recipe.DELETE-api", stopWatch.getTotalTimeMillis());
            return jsonObject.toString();
        }
        stopWatch.stop();
        stopWatch.start("sql");
        User user = helper.validateUser(header);
        stopWatch.stop();
        statsd.recordExecutionTime("recipe.DELETE-sql-1", stopWatch.getLastTaskTimeMillis());
        stopWatch.start("api");

        if (user == null) {
            jsonObject.addProperty("error message", "wrong username or password");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            logger.info("wrong username or password");
            stopWatch.stop();
            statsd.recordExecutionTime("recipe.DELETE-api", stopWatch.getTotalTimeMillis());
            return jsonObject.toString();
        }
        Optional<Recipe> r = recipeRepository.findById(recipeId);
        Recipe recipe = r.isPresent() ? r.get() : null;
        if (recipe == null) {
            jsonObject.addProperty("error message", "no such recipe");
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            logger.info("no such recipe");
            stopWatch.stop();
            statsd.recordExecutionTime("recipe.DELETE-api", stopWatch.getTotalTimeMillis());
            return jsonObject.toString();
        }
        if (!recipe.getAuthorId().equals(user.getId())) {
            jsonObject.addProperty("error message", "no access");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            logger.info("no access");
            stopWatch.stop();
            statsd.recordExecutionTime("recipe.DELETE-api", stopWatch.getTotalTimeMillis());
            return jsonObject.toString();
        }

        List<Image> imageList = imageRepository.findByRecipeId(recipeId);
        for (Image image : imageList) {
            s3Hanlder.deletefile(image.getId());
            stopWatch.stop();
            stopWatch.start("sql");
            imageRepository.delete(image);
            stopWatch.stop();
            statsd.recordExecutionTime("recipe.DELETE-sql-2", stopWatch.getLastTaskTimeMillis());
            stopWatch.start("api");
        }
        recipeRepository.delete(recipe);
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        logger.info("recipe deleted");
        stopWatch.stop();
        statsd.recordExecutionTime("recipe.DELETE-api", stopWatch.getTotalTimeMillis());
        return jsonObject.toString();

    }

    @RequestMapping(value = "/v1/recipe/{id}", method = RequestMethod.PUT, produces = "application/json")
    protected String putRecipe(@RequestBody String recipe_string, HttpServletRequest request, HttpServletResponse response) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("api");
        statsd.increment("recipe.PUT");

        JsonObject jsonObject = new JsonObject();

        DummyRecipe dummyRecipe = gson.fromJson(recipe_string, DummyRecipe.class);

        if (dummyRecipe.getCook_time_in_min() == null)
            jsonObject.addProperty("error message", "cook_time_in_min not contained");
        else if (Integer.parseInt(dummyRecipe.getCook_time_in_min()) % 5 != 0)
            jsonObject.addProperty("error message", "cook_time_in_min is not multiple of 5");
        if (dummyRecipe.getPrep_time_in_min() == null)
            jsonObject.addProperty("error message", "prep_time_in_min not contained");
        else if (Integer.parseInt(dummyRecipe.getPrep_time_in_min()) % 5 != 0)
            jsonObject.addProperty("error message", "prep_time_in_min is not multiple of 5");
        if (dummyRecipe.getTitle() == null)
            jsonObject.addProperty("error message", "title not contained");
        if (dummyRecipe.getTitle() == null)
            jsonObject.addProperty("error message", "cusine not contained");
        if (dummyRecipe.getServings() == null)
            jsonObject.addProperty("error message", "servings not contained");
        else if (Integer.parseInt(dummyRecipe.getServings()) < 1)
            jsonObject.addProperty("error message", "servings minimum is 1");
        else if (Integer.parseInt(dummyRecipe.getServings()) > 5)
            jsonObject.addProperty("error message", "servings maximum is 5");
        if (dummyRecipe.getIngredients() == null)
            jsonObject.addProperty("error message", "ingredients not contained");
        if (dummyRecipe.getSteps() == null)
            jsonObject.addProperty("error message", "steps not contained");
        else if (dummyRecipe.getSteps().length < 1)
            jsonObject.addProperty("error message", "steps minimum is 1");
        if (dummyRecipe.getNutrition_information() == null)
            jsonObject.addProperty("error message", "nutrition_information not contained");

        if (jsonObject.get("error message") != null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            logger.info(jsonObject.get("error message").getAsString());
            stopWatch.stop();
            statsd.recordExecutionTime("recipe.PUT-api", stopWatch.getTotalTimeMillis());
            return jsonObject.toString();
        }

        String recipeId = request.getRequestURI().split("/")[3];
        String header = request.getHeader("Authorization");

        if (header == null) {
            jsonObject.addProperty("error message", "no auth");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            logger.info("no auth");
            stopWatch.stop();
            statsd.recordExecutionTime("recipe.PUT-api", stopWatch.getTotalTimeMillis());
            return jsonObject.toString();
        }
        stopWatch.stop();
        stopWatch.start("sql");
        User user = helper.validateUser(header);
        stopWatch.stop();
        statsd.recordExecutionTime("recipe.PUT-sql-1", stopWatch.getLastTaskTimeMillis());
        stopWatch.start("api");
        if (user == null) {
            jsonObject.addProperty("error message", "wrong username or password");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            logger.info("wrong username or password");
            stopWatch.stop();
            statsd.recordExecutionTime("recipe.PUT-api", stopWatch.getTotalTimeMillis());
            return jsonObject.toString();
        }
        stopWatch.stop();
        stopWatch.start("sql");
        Optional<Recipe> r = recipeRepository.findById(recipeId);
        stopWatch.stop();
        statsd.recordExecutionTime("recipe.PUT-sql-2", stopWatch.getLastTaskTimeMillis());
        stopWatch.start("api");
        Recipe recipe = r.isPresent() ? r.get() : null;
        if (recipe == null) {
            jsonObject.addProperty("error message", "no such recipe");
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            logger.info("no such recipe");
            stopWatch.stop();
            statsd.recordExecutionTime("recipe.PUT-api", stopWatch.getTotalTimeMillis());
            return jsonObject.toString();
        }
        if (!recipe.getAuthorId().equals(user.getId())) {
            jsonObject.addProperty("error message", "no access");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            logger.info("no access");
            stopWatch.stop();
            statsd.recordExecutionTime("recipe.PUT-api", stopWatch.getTotalTimeMillis());
            return jsonObject.toString();
        }

        recipe.setUpdatedTs(new Date().toString());
        recipe.setCook_time_in_min(Integer.parseInt(dummyRecipe.getCook_time_in_min()));
        recipe.setPrep_time_in_min(Integer.parseInt(dummyRecipe.getPrep_time_in_min()));
        recipe.setTotal_time_in_min(recipe.getCook_time_in_min() + recipe.getPrep_time_in_min());
        recipe.setTitle(dummyRecipe.getTitle());
        recipe.setCusine(dummyRecipe.getCusine());
        recipe.setServings(Integer.parseInt(dummyRecipe.getServings()));
        recipe.setIngredients(gson.toJson(dummyRecipe.getIngredients()));
        recipe.setSteps(gson.toJson(dummyRecipe.getSteps()));
        recipe.setNutrition_information(gson.toJson(dummyRecipe.getNutrition_information()));
        stopWatch.stop();
        stopWatch.start("sql");
        recipeRepository.save(recipe);
        stopWatch.stop();
        statsd.recordExecutionTime("recipe.PUT-sql-3", stopWatch.getLastTaskTimeMillis());
        stopWatch.start("api");
        jsonObject.addProperty("id", recipe.getId());
        jsonObject.addProperty("created_ts", recipe.getCreated_ts());
        jsonObject.addProperty("updated_ts", recipe.getUpdatedTs());
        jsonObject.addProperty("author_id", recipe.getAuthorId());
        jsonObject.addProperty("cook_time_in_min", recipe.getCook_time_in_min());
        jsonObject.addProperty("prep_time_in_time", recipe.getPrep_time_in_min());
        jsonObject.addProperty("total_time_in_min", recipe.getTotal_time_in_min());
        jsonObject.addProperty("title", recipe.getTitle());
        jsonObject.addProperty("cusine", recipe.getCusine());
        jsonObject.addProperty("servings", recipe.getServings());
        jsonObject.addProperty("ingrediets", recipe.getIngredients());
        jsonObject.addProperty("steps", recipe.getSteps());
        jsonObject.addProperty("nutrition_information", recipe.getNutrition_information());

        jsonObject.add("ingrediets", gson.fromJson(recipe.getIngredients(), JsonArray.class));
        jsonObject.add("steps", gson.fromJson(recipe.getIngredients(), JsonArray.class));
        jsonObject.add("nutrition_information", gson.fromJson(recipe.getNutrition_information(), JsonObject.class));

        response.setStatus(HttpServletResponse.SC_OK);
        logger.info("recipe updated");
        stopWatch.stop();
        statsd.recordExecutionTime("recipe.PUT-api", stopWatch.getTotalTimeMillis());
        return jsonObject.toString();

    }

    @RequestMapping(value = "/v1/recipe/{id}", method = RequestMethod.GET, produces = "application/json")
    public String getRecipe(HttpServletRequest request, HttpServletResponse response) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("api");
        statsd.increment("recipe.GET");
        JsonObject jsonObject = new JsonObject();

        String recipeId = request.getRequestURI().split("/")[3];
        stopWatch.stop();
        stopWatch.start("sql");
        Optional<Recipe> optionalRecipe = recipeRepository.findById(recipeId);
        stopWatch.stop();
        statsd.recordExecutionTime("recipe.GET-sql-1", stopWatch.getLastTaskTimeMillis());
        stopWatch.start("api");
        Recipe recipe = optionalRecipe.isPresent() ? optionalRecipe.get() : null;
        if (recipe == null) {
            jsonObject.addProperty("error message", "no such recipe");
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            logger.info("no such recipe");
            stopWatch.stop();
            statsd.recordExecutionTime("recipe.GET-api", stopWatch.getTotalTimeMillis());
            return jsonObject.toString();
        }
        stopWatch.stop();
        stopWatch.start("sql");
        List<Image> imageList = imageRepository.findByRecipeId(recipeId);
        stopWatch.stop();
        statsd.recordExecutionTime("recipe.GET-sql-2", stopWatch.getLastTaskTimeMillis());
        stopWatch.start("api");
        JsonArray jsonArray = new JsonArray();
        for (Image image : imageList) {
            JsonObject j = new JsonObject();
            j.addProperty("id", image.getId());
            j.addProperty("url", image.getUrl());
            jsonArray.add(j);
        }
        jsonObject.add("image", jsonArray);
        jsonObject.addProperty("id", recipe.getId());
        jsonObject.addProperty("created_ts", recipe.getCreated_ts());
        jsonObject.addProperty("updated_ts", recipe.getUpdatedTs());
        jsonObject.addProperty("author_id", recipe.getAuthorId());
        jsonObject.addProperty("cook_time_in_min", recipe.getCook_time_in_min());
        jsonObject.addProperty("prep_time_in_time", recipe.getPrep_time_in_min());
        jsonObject.addProperty("total_time_in_min", recipe.getTotal_time_in_min());
        jsonObject.addProperty("title", recipe.getTitle());
        jsonObject.addProperty("cusine", recipe.getCusine());
        jsonObject.addProperty("servings", recipe.getServings());
        jsonObject.addProperty("ingrediets", recipe.getIngredients());
        jsonObject.addProperty("steps", recipe.getSteps());
        jsonObject.addProperty("nutrition_information", recipe.getNutrition_information());

        jsonObject.add("ingrediets", gson.fromJson(recipe.getIngredients(), JsonArray.class));
        jsonObject.add("steps", gson.fromJson(recipe.getIngredients(), JsonArray.class));
        jsonObject.add("nutrition_information", gson.fromJson(recipe.getNutrition_information(), JsonObject.class));

        response.setStatus(HttpServletResponse.SC_OK);
        logger.info("recipe got");
        stopWatch.stop();
        statsd.recordExecutionTime("recipe.GET-api", stopWatch.getTotalTimeMillis());
        return jsonObject.toString();

    }

    @RequestMapping(value = "/v1/recipes", method = RequestMethod.GET, produces = "application/json")
    public String getNewestRecipe(HttpServletRequest request, HttpServletResponse response) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("api");
        statsd.increment("recipes.GET");
        JsonObject jsonObject = new JsonObject();

        stopWatch.stop();
        stopWatch.start("sql");
        List<Recipe> recipeList = recipeRepository.findAll(new Sort(Sort.Direction.DESC, "updatedTs"));
        stopWatch.stop();
        statsd.recordExecutionTime("recipes.GET-sql-1", stopWatch.getLastTaskTimeMillis());
        stopWatch.start("api");
        Recipe recipe = recipeList.get(0);
        stopWatch.stop();
        stopWatch.start("sql");
        List<Image> imageList = imageRepository.findByRecipeId(recipe.getId());
        stopWatch.stop();
        statsd.recordExecutionTime("recipes.GET-sql-2", stopWatch.getLastTaskTimeMillis());
        stopWatch.start("api");
        JsonArray jsonArray = new JsonArray();
        for (Image image : imageList) {
            JsonObject jsonObjectImage = new JsonObject();
            jsonObjectImage.addProperty("id", image.getId());
            jsonObjectImage.addProperty("url", image.getUrl());
            jsonArray.add(jsonObjectImage);
        }
        jsonObject.add("image", jsonArray);
        jsonObject.addProperty("id", recipe.getId());
        jsonObject.addProperty("created_ts", recipe.getCreated_ts());
        jsonObject.addProperty("updated_ts", recipe.getUpdatedTs());
        jsonObject.addProperty("author_id", recipe.getAuthorId());
        jsonObject.addProperty("cook_time_in_min", recipe.getCook_time_in_min());
        jsonObject.addProperty("prep_time_in_time", recipe.getPrep_time_in_min());
        jsonObject.addProperty("total_time_in_min", recipe.getTotal_time_in_min());
        jsonObject.addProperty("title", recipe.getTitle());
        jsonObject.addProperty("cusine", recipe.getCusine());
        jsonObject.addProperty("servings", recipe.getServings());
        jsonObject.addProperty("ingrediets", recipe.getIngredients());
        jsonObject.addProperty("steps", recipe.getSteps());
        jsonObject.addProperty("nutrition_information", recipe.getNutrition_information());

        jsonObject.add("ingrediets", gson.fromJson(recipe.getIngredients(), JsonArray.class));
        jsonObject.add("steps", gson.fromJson(recipe.getIngredients(), JsonArray.class));
        jsonObject.add("nutrition_information", gson.fromJson(recipe.getNutrition_information(), JsonObject.class));

        response.setStatus(HttpServletResponse.SC_OK);
        logger.info("recipes got");
        stopWatch.stop();
        statsd.recordExecutionTime("recipes.PET-api", stopWatch.getTotalTimeMillis());
        return jsonObject.toString();

    }

    @RequestMapping(value = "/v1/myrecipes", method = RequestMethod.POST, produces = "application/json")
    public String getMyRecipe(HttpServletRequest request, HttpServletResponse response) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("api");
        statsd.increment("myrecipes.POST");
        JsonObject jsonObject = new JsonObject();

        String header = request.getHeader("Authorization");
        if (header == null) {
            jsonObject.addProperty("error message", "no auth");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            logger.info("no auth");
            stopWatch.stop();
            statsd.recordExecutionTime("recipe.POST-api", stopWatch.getTotalTimeMillis());
            return jsonObject.toString();
        }

        stopWatch.stop();
        stopWatch.start("sql");
        User user = helper.validateUser(header);
        stopWatch.stop();
        statsd.recordExecutionTime("myrecipes.POST-sql-1", stopWatch.getLastTaskTimeMillis());
        stopWatch.start("api");
        if (user == null) {
            jsonObject.addProperty("error message", "wrong username or password");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            logger.info("wrong username or password");
            stopWatch.stop();
            statsd.recordExecutionTime("myrecipes.POST-api", stopWatch.getTotalTimeMillis());
            return jsonObject.toString();
        }

        stopWatch.stop();
        stopWatch.start("sql");
        List<Recipe> myRecipes = recipeRepository.findByAuthorId(user.getId());
        stopWatch.stop();
        statsd.recordExecutionTime("myrecipes.POST.sql-2", stopWatch.getLastTaskTimeMillis());
        stopWatch.start("api");


        AmazonSNS snsClient = AmazonSNSClient.builder().withRegion("us-east-1")
                .withCredentials(new InstanceProfileCredentialsProvider(false)).build();
//                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY))).build();

        StringBuffer message = new StringBuffer();
        message.append(AWS_REGION);
        message.append("|");
        message.append(ROUTE53);
        message.append("|");
        message.append(user.getUsername());
        message.append("|");

        JsonArray jsonArray = new JsonArray();
        for (Recipe recipe : myRecipes) {
            JsonObject jsonObjectRecipe = new JsonObject();
            jsonObjectRecipe.addProperty("id", recipe.getId());
            jsonArray.add(jsonObjectRecipe);
            message.append(recipe.getId());
            message.append("|");
        }

        snsClient.publish(TOPIC_ARN, message.toString());
        logger.info("sns published");


        response.setStatus(HttpServletResponse.SC_OK);
        logger.info("myrecipes got");
        stopWatch.stop();
        statsd.recordExecutionTime("myrecipes.POST-api", stopWatch.getTotalTimeMillis());
        return jsonArray.toString();
    }

}
