package com.example.webapp.helpers;

import com.example.webapp.dao.UserRepository;
import com.example.webapp.entities.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Optional;

@Service
public class Helper {

    @Autowired
    UserRepository userRepository;

    public User validateUser(String header) {

        String basicAuthEncoded = header.substring(6);
        String basicAuthAsString = new String(Base64.getDecoder().decode(basicAuthEncoded.getBytes()));
        final String[] credentialValues = basicAuthAsString.split(":", 2);

        String username = credentialValues[0];
        String password = credentialValues[1];

        Optional<User> optionalUser = userRepository.findById(username);
        User user = optionalUser.isPresent() ? optionalUser.get() : null;
        if (user == null)
            return null;
        if (BCrypt.checkpw(password, user.getPassword()))
            return user;
        return null;
    }
}
