package com.example.webapp.service;

import com.example.webapp.dao.UserRepository;
import com.example.webapp.entities.User;
import com.example.webapp.helpers.BCrypt;
import com.example.webapp.helpers.Helper;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class UserService {

    @Autowired
    UserRepository userRepository;
    @Autowired
    Helper helper;
    public String createNewUser(User user) {

        JsonObject jsonObject = new JsonObject();
        List<User> u = userRepository.findByUsername(user.getUsername());
        if (!u.isEmpty()) {
            return "400";
        }

        String username = user.getUsername();
        String password = user.getPassword();

        if (isEmail(username) == false) {
            return "400";
        }
        if (!passwordCheck(password)) {
            return "400";
        }

        user.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
        String now = new Date().toString();
        user.setAccountCreated(now);
        user.setAccountUpdated(now);
        userRepository.save(user);
        jsonObject.addProperty("id", user.getID());
        jsonObject.addProperty("first_name", user.getFirstname());
        jsonObject.addProperty("last_name", user.getLastname());
        jsonObject.addProperty("email_address", username);
        jsonObject.addProperty("account_created", user.getAccountCreated());
        jsonObject.addProperty("account_updated", user.getAccountUpdated());
        return jsonObject.toString();

    }

    public String getUserInformation(HttpServletRequest request) {

        JsonObject jsonObject = new JsonObject();
        String header = request.getHeader("Authorization");
        if (header != null) {
            User user = helper.validateUser(header);
            if (user != null) {
                jsonObject.addProperty("id", user.getID());
                jsonObject.addProperty("first_name", user.getFirstname());
                jsonObject.addProperty("last_name", user.getLastname());
                jsonObject.addProperty("email_address", user.getUsername());
                jsonObject.addProperty("account_created", user.getAccountCreated());
                jsonObject.addProperty("account_updated", user.getAccountUpdated());
                return jsonObject.toString();
            }
        }
        return "401";

    }

    public String updateUserInformation(User user, HttpServletRequest request) {

        JsonObject jsonObject = new JsonObject();

        String header = request.getHeader("Authorization");
        if (user == null) {
            return "204";
        }
        if (!(user.getID() == null && user.getUsername() == null && user.getAccountCreated() == null && user.getAccountUpdated() == null)) {
            return "400";
        }

        if (!passwordCheck((user.getPassword()))) {
            return "400";
        }
        if (header != null) {
            User u = helper.validateUser(header);
            if (u != null) {
                u.setPassword(BCrypt.hashpw(user.getPassword(), BCrypt.gensalt()));
                u.setFirstName(user.getFirstname());
                u.setLastName(user.getLastname());
                u.setAccountUpdated(new Date().toString());
                userRepository.save(u);
                jsonObject.addProperty("id", u.getID());
                jsonObject.addProperty("first_name", u.getFirstname());
                jsonObject.addProperty("last_name", u.getLastname());
                jsonObject.addProperty("email_address", u.getUsername());
                jsonObject.addProperty("account_created", u.getAccountCreated());
                jsonObject.addProperty("account_updated", u.getAccountUpdated());
                return jsonObject.toString();
            }

        }
        return "401";
    }

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

}
