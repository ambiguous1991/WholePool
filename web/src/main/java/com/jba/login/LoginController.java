package com.jba.login;

import com.jba.dao2.user.enitity.User;
import com.jba.dao2.user.enitity.UserType;
import com.jba.utils.Deserializer;
import com.jba.utils.Mailer;
import com.jba.utils.Methods;
import com.jba.utils.RestRequestBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping
public class LoginController {

    @Value("${wholepool.rest.url.base.url}")
    private String wholepoolBaseUrl;

    private final String usersBase = "users";

    @Autowired
    Deserializer deserializer;

    @Autowired
    private Methods methods;

    @Autowired
    Mailer mailer;

    @GetMapping(value = "/login")
    public String login(){
        return "login";
    }

    @PostMapping(value = "/login")
    public String doLogin(User user, HttpSession session){
        RestTemplate restTemplate = new RestTemplate();

        String userSearchURL = RestRequestBuilder
                .builder(wholepoolBaseUrl)
                .addPathParam(usersBase)
                .addParam("email", user.getEmailAddress())
                .build();

        String result = restTemplate.getForObject(userSearchURL, String.class);

        User userFromJson = deserializer.getSingleItemFor(result, User.class);

        System.out.println(userFromJson.toString());

        String verifyPasswordURL = RestRequestBuilder
                .builder(wholepoolBaseUrl)
                .addPathParam(usersBase)
                .addPathParam("verify")
                .addParam("hash", user.getPasswordHash())
                .addParam("userId", userFromJson.getUserId())
                .build();

        String verifyPassword = restTemplate.getForObject(verifyPasswordURL, String.class);

        Boolean isPasswordCorrect = deserializer.getSingleItemFor(verifyPassword, Boolean.class);

        if(isPasswordCorrect){
            session.setAttribute("user", userFromJson);
            return "redirect:/user/dashboard";
        }
        else{
            return "login";
        }
    }

    @GetMapping("/register")
    public String register(){
        return "register";
    }

    @PostMapping("/register")
    public String doRegister(User user, HttpSession session){
        String postNewUserQuery = RestRequestBuilder
                .builder(wholepoolBaseUrl)
                .addPathParam("users")
                .build();

        RestTemplate template = new RestTemplate();

        String hash = user.getPasswordHash();

        UserType passenger = methods.getUserTypeByName("Pasażer");

        user.setUserType(passenger);

        user = deserializer.getSingleItemFor(template.postForObject(postNewUserQuery, user, String.class), User.class);

        user.setPasswordHash(hash);

        String changePasswordRequest = RestRequestBuilder
                .builder(wholepoolBaseUrl)
                .addPathParam("users")
                .addPathParam("password")
                .addParam("userId", user.getUserId())
                .addParam("hash", hash)
                .build();

        template.put(changePasswordRequest, user);

        session.setAttribute("user", user);

        mailer.sendAccountCreatedMessage(user.getEmailAddress(), user.getFirstName());

        return "redirect:/user/dashboard";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session){
        session.removeAttribute("user");
        return "index";
    }
}