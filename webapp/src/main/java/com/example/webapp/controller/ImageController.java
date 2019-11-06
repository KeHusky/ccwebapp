package com.example.webapp.controller;

import com.example.webapp.dao.ImageRepository;
import com.example.webapp.dao.RecipeRepository;
import com.example.webapp.entities.Image;
import com.example.webapp.entities.Recipe;
import com.example.webapp.entities.User;
import com.example.webapp.helpers.Helper;
import com.example.webapp.helpers.S3Hanlder;
import com.google.gson.JsonObject;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
public class ImageController {

    @Autowired
    Helper helper;
    @Autowired
    RecipeRepository recipeRepository;
    @Autowired
    S3Hanlder s3Hanlder;
    @Autowired
    ImageRepository imageRepository;
    Logger logger = LoggerFactory.getLogger(ImageController.class);
    StatsDClient statsd = new NonBlockingStatsDClient("csye6225", "localhost", 8125);

    @RequestMapping(value = "/v1/recipe/{id}/image", method = RequestMethod.POST, produces = "application/json")
    protected String postImage(@RequestParam MultipartFile file, HttpServletRequest request, HttpServletResponse response) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("api");
        statsd.increment("image.POST");
        JsonObject jsonObject = new JsonObject();

        String suffix = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".") + 1);
        if (!(suffix.toLowerCase().equals("png") || suffix.toLowerCase().equals("jpg") || suffix.toLowerCase().equals("jpeg"))) {
            jsonObject.addProperty("error message", "wrong file type");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            logger.info("wrong file type");
            stopWatch.stop();
            statsd.recordExecutionTime("image.POST-api",stopWatch.getTotalTimeMillis());
            return jsonObject.toString();
        }

        String recipeId = request.getRequestURI().split("/")[3];
        String header = request.getHeader("Authorization");

        if (header == null) {
            jsonObject.addProperty("error message", "no auth");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            logger.info("no auth");
            stopWatch.stop();
            statsd.recordExecutionTime("image.POST-api",stopWatch.getTotalTimeMillis());
            return jsonObject.toString();
        }
        stopWatch.stop();
        stopWatch.start("sql");
        User user = helper.validateUser(header);
        stopWatch.stop();
        statsd.recordExecutionTime("image.POST-sql-1",stopWatch.getLastTaskTimeMillis());
        stopWatch.start("api");
        if (user == null) {
            jsonObject.addProperty("error message", "wrong username or password");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            logger.info("wrong username or password");
            stopWatch.stop();
            statsd.recordExecutionTime("image.POST-api",stopWatch.getTotalTimeMillis());
            return jsonObject.toString();
        }
        stopWatch.stop();
        stopWatch.start("sql");
        Optional<Recipe> o = recipeRepository.findById(recipeId);
        stopWatch.stop();
        statsd.recordExecutionTime("image.POST-sql-2",stopWatch.getLastTaskTimeMillis());
        stopWatch.start("api");
        Recipe recipe = o.isPresent() ? o.get() : null;
        if (recipe == null) {
            jsonObject.addProperty("error message", "no such recipe");
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            logger.info("no such recipe");
            stopWatch.stop();
            statsd.recordExecutionTime("image.POST-api",stopWatch.getTotalTimeMillis());
            return jsonObject.toString();
        }
        if (!recipe.getAuthor_id().equals(user.getId())) {
            jsonObject.addProperty("error message", "no access");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            logger.info("no access");
            stopWatch.stop();
            statsd.recordExecutionTime("image.POST-api",stopWatch.getTotalTimeMillis());
            return jsonObject.toString();
        }
        Image image = new Image();
        image.setId(UUID.randomUUID().toString());
        String md5 = "";

        try {
            md5 = new String(org.apache.commons.codec.binary.Base64.encodeBase64(DigestUtils.md5(file.getBytes())));
        } catch (IOException e) {
            e.printStackTrace();
        }

        image.setUrl(s3Hanlder.uploadfile(file, image.getId()));
        image.setRecipeId(recipeId);
        image.setMd5(md5);
        image.setSize(file.getSize());
        image.setType("image/" + suffix);
        stopWatch.stop();
        stopWatch.start("sql");
        List<Image> imageList = imageRepository.findByRecipeId(recipeId);
        stopWatch.stop();
        statsd.recordExecutionTime("image.POST-sql-3",stopWatch.getLastTaskTimeMillis());
        stopWatch.start("api");
        for(Image i :imageList) {
            if(i.getFileName().equals(file.getOriginalFilename())) {
                jsonObject.addProperty("error message", "image exited");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                logger.info("image exited");
                stopWatch.stop();
                statsd.recordExecutionTime("image.POST-api",stopWatch.getTotalTimeMillis());
                return jsonObject.toString();
            }
        }
        image.setFileName(file.getOriginalFilename());
        stopWatch.stop();
        stopWatch.start("sql");
        imageRepository.save(image);
        stopWatch.stop();
        statsd.recordExecutionTime("image.POST-sql-4",stopWatch.getLastTaskTimeMillis());
        stopWatch.start("api");

        jsonObject.addProperty("id", image.getId());
        jsonObject.addProperty("url", image.getUrl());
        response.setStatus(HttpServletResponse.SC_CREATED);
        logger.info("image created");
        stopWatch.stop();
        statsd.recordExecutionTime("image.POST-api",stopWatch.getTotalTimeMillis());
        return jsonObject.toString();
    }

    @RequestMapping(value = "/v1/recipe/{recipeId}/image/{imageId}", method = RequestMethod.DELETE, produces = "application/json")
    protected String deleteImage(HttpServletRequest request, HttpServletResponse response) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("api");
        statsd.increment("image.DELETE");
        JsonObject jsonObject = new JsonObject();

        String recipeId = request.getRequestURI().split("/")[3];
        String imageId = request.getRequestURI().split("/")[5];
        String header = request.getHeader("Authorization");

        if (header == null) {
            jsonObject.addProperty("error message", "no auth");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            logger.info("no auth");
            stopWatch.stop();
            statsd.recordExecutionTime("image.DELETE-api",stopWatch.getTotalTimeMillis());
            return jsonObject.toString();
        }
        stopWatch.stop();
        stopWatch.start("sql");
        User user = helper.validateUser(header);
        stopWatch.stop();
        statsd.recordExecutionTime("image.DELETE-sql-1",stopWatch.getLastTaskTimeMillis());
        stopWatch.start("api");
        if (user == null) {
            jsonObject.addProperty("error message", "wrong username or password");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            logger.info("wrong username or password");
            stopWatch.stop();
            statsd.recordExecutionTime("image.DELETE-api",stopWatch.getTotalTimeMillis());
            return jsonObject.toString();
        }
        stopWatch.stop();
        stopWatch.start("sql");
        Optional<Recipe> optionalRecipe = recipeRepository.findById(recipeId);
        stopWatch.stop();
        statsd.recordExecutionTime("image.DELETE-sql-2",stopWatch.getLastTaskTimeMillis());
        stopWatch.start("api");
        Recipe recipe = optionalRecipe.isPresent() ? optionalRecipe.get() : null;
        if (recipe == null) {
            jsonObject.addProperty("error message", "no such recipe");
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            logger.info("no such recipe");
            stopWatch.stop();
            statsd.recordExecutionTime("image.DELETE-api",stopWatch.getTotalTimeMillis());
            return jsonObject.toString();
        }
        if (!recipe.getAuthor_id().equals(user.getId())) {
            jsonObject.addProperty("error message", "no recipe access");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            logger.info("no recipe access");
            stopWatch.stop();
            statsd.recordExecutionTime("image.DELETE-api",stopWatch.getTotalTimeMillis());
            return jsonObject.toString();
        }
        stopWatch.stop();
        stopWatch.start("sql");
        Optional<Image> optionalImage = imageRepository.findById(imageId);
        stopWatch.stop();
        statsd.recordExecutionTime("image.DELETE-sql-3",stopWatch.getLastTaskTimeMillis());
        stopWatch.start("api");
        Image image = optionalImage.isPresent() ? optionalImage.get() : null;
        if (image == null) {
            jsonObject.addProperty("error message", "no such image");
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            logger.info("no such image");
            stopWatch.stop();
            statsd.recordExecutionTime("image.DELETE-api",stopWatch.getTotalTimeMillis());
            return jsonObject.toString();
        }
        if (!image.getRecipeId().equals(recipeId)) {
            jsonObject.addProperty("error message", "no image access");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            logger.info("no image access");
            stopWatch.stop();
            statsd.recordExecutionTime("image.DELETE-api",stopWatch.getTotalTimeMillis());
            return jsonObject.toString();
        }
        s3Hanlder.deletefile(imageId);
        stopWatch.stop();
        stopWatch.start("sql");
        imageRepository.delete(image);
        stopWatch.stop();
        statsd.recordExecutionTime("image.DELETE-sql-4",stopWatch.getLastTaskTimeMillis());
        stopWatch.start("api");
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        logger.info("image deleted");
        stopWatch.stop();
        statsd.recordExecutionTime("image.DELETE-api",stopWatch.getTotalTimeMillis());
        return jsonObject.toString();
    }

    @RequestMapping(value = "/v1/recipe/{recipeId}/image/{imageId}", method = RequestMethod.GET, produces = "application/json")
    public String getImage(HttpServletRequest request, HttpServletResponse response) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("api");
        statsd.increment("image.GET");
        JsonObject jsonObject = new JsonObject();

        String recipeId = request.getRequestURI().split("/")[3];
        String imageId = request.getRequestURI().split("/")[5];
        stopWatch.stop();
        stopWatch.start("sql");
        Optional<Recipe> optionalRecipe = recipeRepository.findById(recipeId);
        stopWatch.stop();
        statsd.recordExecutionTime("image.GET-sql-1",stopWatch.getLastTaskTimeMillis());
        stopWatch.start("api");
        Recipe recipe = optionalRecipe.isPresent() ? optionalRecipe.get() : null;
        if (recipe == null) {
            jsonObject.addProperty("error message", "no such recipe");
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            logger.info("no such recipe");
            stopWatch.stop();
            statsd.recordExecutionTime("image.GET-api",stopWatch.getTotalTimeMillis());
            return jsonObject.toString();
        }
        stopWatch.stop();
        stopWatch.start("sql");
        Optional<Image> optionalImage = imageRepository.findById(imageId);
        stopWatch.stop();
        statsd.recordExecutionTime("image.GET-sql-2",stopWatch.getLastTaskTimeMillis());
        stopWatch.start("api");
        Image image = optionalImage.isPresent() ? optionalImage.get() : null;
        if (image == null) {
            jsonObject.addProperty("error message", "no such image");
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            logger.info("no such image");
            stopWatch.stop();
            statsd.recordExecutionTime("image.GET-api",stopWatch.getTotalTimeMillis());
            return jsonObject.toString();
        }
        if (!image.getRecipeId().equals(recipeId)) {
            jsonObject.addProperty("error message", "no image access");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            logger.info("no image access");
            stopWatch.stop();
            statsd.recordExecutionTime("image.GET-api",stopWatch.getTotalTimeMillis());
            return jsonObject.toString();
        }

        jsonObject.addProperty("id", imageId);
        jsonObject.addProperty("url", image.getUrl());
        response.setStatus(HttpServletResponse.SC_OK);
        stopWatch.stop();
        statsd.recordExecutionTime("image.GET-api",stopWatch.getTotalTimeMillis());
        return jsonObject.toString();
    }
}
