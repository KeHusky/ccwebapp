package com.example.webapp.controller;

import com.example.webapp.entities.User;
import com.example.webapp.dao.UserRepository;
import com.example.webapp.helpers.BCrypt;
import com.example.webapp.helpers.Helper;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
public class UserController {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private Helper helper;

    //Create a user
    @RequestMapping(value = "/v1/user", method = RequestMethod.POST, produces = "application/json")
    public String createNewUser(@RequestBody User user, HttpServletRequest request, HttpServletResponse response) {

        JsonObject jsonObject = new JsonObject();
        List<User> u = userRepository.findByUsername(user.getUsername());
        if (!u.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }

        String username = user.getUsername();
        String password = user.getPassword();

        if (isEmail(username) == false) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }
        if (!passwordCheck(password)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }

        user.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
        String now = new Date().toString();
        user.setAccountCreated(now);
        user.setAccountUpdated(now);
        userRepository.save(user);
        jsonObject.addProperty("id", user.getID());
        jsonObject.addProperty("first_name:", user.getFirstname());
        jsonObject.addProperty("last_name:", user.getLastname());
        jsonObject.addProperty("email-address", username);
        jsonObject.addProperty("account_created", user.getAccountCreated());
        jsonObject.addProperty("account_updated", user.getAccountUpdated());
        response.setStatus(HttpServletResponse.SC_CREATED);
        return jsonObject.toString();

    }

    //Get User Information
    @RequestMapping(value = "/v1/user/self", method = RequestMethod.GET, produces = "application/json")
    protected String getUserInformation(HttpServletRequest request, HttpServletResponse response) {

        JsonObject jsonObject = new JsonObject();
        String header = request.getHeader("Authorization");
        if (header != null) {
            User user = helper.validateUser(header);
            if (user != null) {
                jsonObject.addProperty("id", user.getID());
                jsonObject.addProperty("first_name:", user.getFirstname());
                jsonObject.addProperty("last_name:", user.getLastname());
                jsonObject.addProperty("email-address", user.getUsername());
                jsonObject.addProperty("account_created", user.getAccountCreated());
                jsonObject.addProperty("account_updated", user.getAccountUpdated());
                response.setStatus(HttpServletResponse.SC_OK);
                return jsonObject.toString();
            }
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        return null;
    }

    //Update user information
    @RequestMapping(value = "/v1/user/self", method = RequestMethod.PUT, produces = "application/json")
    protected String updateUserInformation(@RequestBody User user, HttpServletRequest request, HttpServletResponse response) {

        JsonObject jsonObject = new JsonObject();

        String header = request.getHeader("Authorization");
        if (user == null) {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return null;
        }
        if (!(user.getID() == null && user.getUsername() == null && user.getAccountCreated() == null && user.getAccountUpdated() == null)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }

        if (!passwordCheck((user.getPassword()))) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }
        if (header != null) {
            User u = helper.validateUser(header);
            if (u != null) {
                System.out.println(user.getPassword());
                u.setPassword(BCrypt.hashpw(user.getPassword(), BCrypt.gensalt()));
                System.out.println(user.getPassword());
                u.setFirstName(user.getFirstname());
                u.setLastName(user.getLastname());
                u.setAccountUpdated(new Date().toString());
                userRepository.save(u);
                jsonObject.addProperty("id", u.getID());
                jsonObject.addProperty("first_name:", u.getFirstname());
                jsonObject.addProperty("last_name:", u.getLastname());
                jsonObject.addProperty("email-address", u.getUsername());
                jsonObject.addProperty("account_created", u.getAccountCreated());
                jsonObject.addProperty("account_updated", u.getAccountUpdated());
                response.setStatus(HttpServletResponse.SC_OK);
                return jsonObject.toString();
            }

        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return null;

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

