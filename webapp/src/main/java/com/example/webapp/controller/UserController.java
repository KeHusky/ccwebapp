package com.example.webapp.controller;

import com.example.webapp.dao.UserRepository;
import com.example.webapp.entities.User;
import com.example.webapp.helpers.BCrypt;
import com.example.webapp.helpers.Helper;
import com.google.gson.JsonObject;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
public class UserController {
    @Autowired
    Helper helper;
    @Autowired
    UserRepository userRepository;
    Logger logger = LoggerFactory.getLogger(UserController.class);
    StatsDClient statsd = new NonBlockingStatsDClient("csye6225", "localhost", 8125);

    //Create a user
    @RequestMapping(value = "/v1/user", method = RequestMethod.POST, produces = "application/json")
    public String createNewUser(@RequestBody User user, HttpServletRequest request, HttpServletResponse response) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("api");
        statsd.increment("user.POST");

        JsonObject jsonObject = new JsonObject();
        Optional<User> optionalUser = userRepository.findById(user.getUsername());
        User u = optionalUser.isPresent() ? optionalUser.get() : null;
        if (u != null) {
            jsonObject.addProperty("error message", "user exited");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            logger.info("user exited");
            stopWatch.stop();
            statsd.recordExecutionTime("user.POST-api",stopWatch.getTotalTimeMillis());
            return jsonObject.toString();
        }

        String username = user.getUsername();
        String password = user.getPassword();

        if (isEmail(username) == false) {
            jsonObject.addProperty("error message", "username is not an email");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            logger.info("username is not an email");
            stopWatch.stop();
            statsd.recordExecutionTime("user.POST-api",stopWatch.getTotalTimeMillis());
            return jsonObject.toString();
        }
        if (!passwordCheck(password)) {
            jsonObject.addProperty("error message", "password is not strong");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            logger.info("password is not strong");
            stopWatch.stop();
            statsd.recordExecutionTime("user.POST-api",stopWatch.getTotalTimeMillis());
            return jsonObject.toString();
        }

        user.setId(UUID.randomUUID().toString());
        user.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
        String now = new Date().toString();
        user.setAccount_created(now);
        user.setAccount_updated(now);
        stopWatch.stop();
        stopWatch.start("sql");
        userRepository.save(user);
        stopWatch.stop();
        statsd.recordExecutionTime("user.POST-sql",stopWatch.getLastTaskTimeMillis());
        stopWatch.start("api");
        jsonObject.addProperty("id", user.getId());
        jsonObject.addProperty("first_name", user.getFirstname());
        jsonObject.addProperty("last_name", user.getLastname());
        jsonObject.addProperty("email_address", username);
        jsonObject.addProperty("account_created", user.getAccount_created());
        jsonObject.addProperty("account_updated", user.getAccount_updated());
        response.setStatus(HttpServletResponse.SC_CREATED);
        logger.info("user created");
        stopWatch.stop();
        statsd.recordExecutionTime("user.POST-api",stopWatch.getTotalTimeMillis());
        return jsonObject.toString();
    }

    //Get User Information
    @RequestMapping(value = "/v1/user/self", method = RequestMethod.GET, produces = "application/json")
    protected String getUserInformation(HttpServletRequest request, HttpServletResponse response) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("api");
        statsd.increment("user.GET");

        JsonObject jsonObject = new JsonObject();
        String header = request.getHeader("Authorization");
        if (header == null) {
            jsonObject.addProperty("error message", "no auth");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            logger.info("no auth");
            stopWatch.stop();
            statsd.recordExecutionTime("user.GET-api",stopWatch.getTotalTimeMillis());
            return jsonObject.toString();
        }

        stopWatch.stop();
        stopWatch.start("sql");
        User user = helper.validateUser(header);
        stopWatch.stop();
        statsd.recordExecutionTime("user.GET-sql",stopWatch.getLastTaskTimeMillis());
        stopWatch.start("api");
        if (user == null) {
            jsonObject.addProperty("error message", "wrong username or password");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            logger.info("wrong username or password");
            stopWatch.stop();
            statsd.recordExecutionTime("user.GET-api",stopWatch.getTotalTimeMillis());
            return jsonObject.toString();
        }
        jsonObject.addProperty("id", user.getId());
        jsonObject.addProperty("first_name", user.getFirstname());
        jsonObject.addProperty("last_name", user.getLastname());
        jsonObject.addProperty("email_address", user.getUsername());
        jsonObject.addProperty("account_created", user.getAccount_created());
        jsonObject.addProperty("account_updated", user.getAccount_updated());
        response.setStatus(HttpServletResponse.SC_OK);
        logger.info("user got");
        stopWatch.stop();
        statsd.recordExecutionTime("user.GET-api",stopWatch.getTotalTimeMillis());
        return jsonObject.toString();
    }

    //Update user information
    @RequestMapping(value = "/v1/user/self", method = RequestMethod.PUT, produces = "application/json")
    protected String updateUserInformation(@RequestBody User user, HttpServletRequest request, HttpServletResponse response) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("api");
        statsd.increment("user.PUT");

        JsonObject jsonObject = new JsonObject();

        String header = request.getHeader("Authorization");

        if (!passwordCheck((user.getPassword()))) {
            jsonObject.addProperty("error message", "password is not strong");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            logger.info("password is not strong");
            stopWatch.stop();
            statsd.recordExecutionTime("user.PUT-api",stopWatch.getTotalTimeMillis());
            return jsonObject.toString();
        }

        if (header == null) {
            jsonObject.addProperty("error message", "no auth");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            logger.info("no auth");
            stopWatch.stop();
            statsd.recordExecutionTime("user.PUT-api",stopWatch.getTotalTimeMillis());
            return jsonObject.toString();
        }
        stopWatch.stop();
        stopWatch.start("sql");
        User u = helper.validateUser(header);
        stopWatch.stop();
        statsd.recordExecutionTime("user.PUT-sql-1",stopWatch.getLastTaskTimeMillis());
        stopWatch.start("api");
        if (u == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            logger.info("wrong username or password");
            return jsonObject.toString();
        }
        u.setPassword(BCrypt.hashpw(user.getPassword(), BCrypt.gensalt()));
        u.setFirstname(user.getFirstname());
        u.setLastname(user.getLastname());
        u.setAccount_updated(new Date().toString());
        userRepository.save(u);
        jsonObject.addProperty("id", u.getId());
        jsonObject.addProperty("first_name", u.getFirstname());
        jsonObject.addProperty("last_name", u.getLastname());
        jsonObject.addProperty("email_address", u.getUsername());
        jsonObject.addProperty("account_created", u.getAccount_created());
        jsonObject.addProperty("account_updated", u.getAccount_updated());
        response.setStatus(HttpServletResponse.SC_OK);
        logger.info("user updated");
        stopWatch.stop();
        statsd.recordExecutionTime("user.PUT-api",stopWatch.getTotalTimeMillis());
        return jsonObject.toString();
    }

    private boolean passwordCheck(String password) {
        if (password.length() <= 8) {
            return false;
        }

        String reg = "[A-Z]";
        Pattern pattern = Pattern.compile(reg);
        Matcher matcher = pattern.matcher(password);
        int j = 0;
        while (matcher.find()) {
            j++;
        }
        if (j == 0) {
            return false;

        }

        String reg2 = "[a-z]";
        Pattern pattern1 = Pattern.compile(reg2);
        Matcher matcher1 = pattern1.matcher(password);
        int k = 0;
        while (matcher1.find()) {
            k++;
        }
        if (k == 0) {
            return false;
        }

        String reg3 = "[0-9]";
        Pattern pattern2 = Pattern.compile(reg3);
        Matcher matcher2 = pattern2.matcher(password);
        int l = 0;
        while (matcher2.find()) {
            l++;
        }
        if (l == 0) {
            return false;
        }

        return true;
    }

    //check if a username is an email address or not
    private static boolean isEmail(String string) {
        if (string == null) return false;
        String regEx1 = "^([a-z0-9A-Z]+[-|\\.]?)+[a-z0-9A-Z]@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-zA-Z]{2,}$";
        Pattern p;
        Matcher m;
        p = Pattern.compile(regEx1);
        m = p.matcher(string);
        if (m.matches()) return true;
        else
            return false;
    }
}

