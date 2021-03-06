package com.jba.user;

import com.jba.dao2.cars.entity.Car;
import com.jba.dao2.cars.entity.CarType;
import com.jba.dao2.user.dao.UserDAO;
import com.jba.dao2.user.enitity.User;
import com.jba.dao2.user.enitity.UserType;
import com.jba.utils.Deserializer;
import com.jba.utils.Mailer;
import com.jba.utils.Methods;
import com.jba.utils.RestRequestBuilder;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.thymeleaf.util.StringUtils;

import javax.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/user")
public class UserController {

    Logger logger = Logger.getLogger(this.getClass());

    @Autowired
    Mailer mailer;

    @Autowired
    UserDAO userDAO;

    @Value("${wholepool.rest.url.base.url}")
    String WPLBaseURL;

    @Autowired
    Deserializer deserializer;

    @Autowired
    private Methods methods;

    @GetMapping("/settings")
    public String setting(HttpSession session){
        if(session.getAttribute("user")==null){
            return "error/401";
        }
        return "user-settings";
    }

    @GetMapping("/settings/preferences")
    public String getPreferencesSettings(HttpSession session){
        if(session.getAttribute("user")==null){
            return "error/401";
        }
        return "user-settings-preferences";
    }

    @GetMapping("/settings/data")
    public String getDataSettings(HttpSession session){
        if(session.getAttribute("user")==null){
            return "error/401";
        }
        return "user-settings-data";
    }

    @PostMapping("/settings/data")
    public String updateUserData(User user, HttpSession session, RedirectAttributes redirectAttributes){
        User userFromSession = (User) session.getAttribute("user");

        logger.debug("User start values: "+userFromSession);
        logger.debug("User from form: "+user);

        if(!userFromSession.getUserName().equals(user.getUserName()))
            userFromSession.setUserName(user.getUserName());
        if(!userFromSession.getFirstName().equals(user.getFirstName()))
            userFromSession.setFirstName(StringUtils.capitalize(user.getFirstName()));
        if(!userFromSession.getLastName().equals(user.getLastName()))
            userFromSession.setLastName(StringUtils.capitalize(user.getLastName()));
        if(!userFromSession.getEmailAddress().equals(user.getEmailAddress()))
            userFromSession.setEmailAddress(user.getEmailAddress());
        if(!userFromSession.getDateOfBirth().equals(user.getDateOfBirth()))
            userFromSession.setDateOfBirth(user.getDateOfBirth());

        logger.debug("User data changed to: "+userFromSession);

        String updateUserQuery = RestRequestBuilder
                .builder(WPLBaseURL)
                .addPathParam("users")
                .build();

        RestTemplate template = new RestTemplate();
        template.put(updateUserQuery, userFromSession);
        logger.info("Updated user data successfully");

        String getUserQuery = RestRequestBuilder
                .builder(WPLBaseURL)
                .addPathParam("users")
                .addParam("id", userFromSession.getUserId())
                .build();

        userFromSession = deserializer.getSingleItemFor(template.getForObject(getUserQuery, String.class), User.class);

        logger.info("Updating user data in session to "+userFromSession);

        session.removeAttribute("user");
        session.setAttribute("user", userFromSession);

        redirectAttributes.addAttribute("message", "Twoje dane zostały zaktualizowane.");

        return "redirect:/user/settings/confirm";
    }

    @GetMapping("/settings/accountType")
    public String getAccountType(Model model, HttpSession session){
        if(session.getAttribute("user")==null){
            return "error/401";
        }

        String getUserTypesRequest = RestRequestBuilder
                .builder(WPLBaseURL)
                .addPathParam("users")
                .addPathParam("type")
                .build();

        RestTemplate template= new RestTemplate();

        UserType[] userTypes = deserializer.getResultArrayFor(template.getForObject(getUserTypesRequest, String.class), UserType[].class);

        List<UserType> filtered = Arrays.stream(userTypes)
                .filter(userType -> userType.getTypeName().equals("Pasażer")||userType.getTypeName().equals("Kierowca"))
                .sorted(Comparator.comparing(UserType::getTypeId))
                .collect(Collectors.toList());

        model.addAttribute("userTypes", filtered);

        filtered.forEach(
                userType -> logger.debug(userType + "retrieved.")
        );

        return "user-settings-accountType";
    }

    @PostMapping("/settings/accountType")
    public String doChangeAccountType(UserType userType, RedirectAttributes redirectAttributes, HttpSession session){
        logger.debug("read: "+userType);

        userType = methods.getUserTypeById(Integer.parseInt(userType.getTypeName()));

        if(userType.getTypeName().equals("Pasażer")){
            redirectAttributes.addAttribute("message", "not-a-driver");
            return "redirect:/settings/accountType";
        }

        User userFromSession = (User) session.getAttribute("user");

        String changeAccountTypeRequest = RestRequestBuilder
                .builder(WPLBaseURL)
                .addPathParam("users")
                .build();

        userFromSession.setUserType(userType);

        RestTemplate template = new RestTemplate();

        logger.info("Updating user data");
        template.put(changeAccountTypeRequest, userFromSession);

        logger.info("Updating session info");
        session.removeAttribute("user");
        session.setAttribute("user", userFromSession);

        redirectAttributes.addAttribute("message", "become-driver");
        return "redirect:/user/settings/addCar";
    }

    @GetMapping("/settings/changePassword")
    public String getChangePassword(HttpSession session){
        if(session.getAttribute("user")==null){
            return "error/401";
        }
        return "user-settings-changePassword";
    }

    @PostMapping("/settings/changePassword")
    public String updatePassword(String password, String passwordConfirm, HttpSession session, RedirectAttributes redirectAttributes){
        User userFromSession = (User) session.getAttribute("user");

        logger.debug(password + "=" + passwordConfirm + "?");

        if(password.equals(passwordConfirm)){
            try {

                password = generatePasswordHash(password);

            }
            catch (NoSuchAlgorithmException e){
                logger.error(e);
            }
            userFromSession.setPasswordHash(password);

            String changePasswordRequest = RestRequestBuilder
                    .builder(WPLBaseURL)
                    .addPathParam("users")
                    .addPathParam("password")
                    .addParam("userId", userFromSession.getUserId())
                    .addParam("hash", password)
                    .build();

            RestTemplate template = new RestTemplate();

            template.put(changePasswordRequest, userFromSession);

            redirectAttributes.addAttribute("message", "Twoje hasło zostało zmienione.");

            mailer.sendChangedPasswordMessage(userFromSession.getEmailAddress(), userFromSession.getFirstName());

            return "redirect:/user/settings/confirm";
        }
        else{
            logger.error("Error setting password!");
            redirectAttributes.addAttribute("message", "Wprowadzone hasła różnią się od siebie. Spróbuj ponownie");
            return "redirect:/user/settings/confirm";
        }
    }

    @GetMapping("/settings/myCars")
    public String getCars(HttpSession session, Model model){
        if(session.getAttribute("user")==null){
            return "error/401";
        }
        User user = (User) session.getAttribute("user");

        String getCarsQuery = RestRequestBuilder
                .builder(WPLBaseURL)
                .addPathParam("cars")
                .addParam("userId", user.getUserId())
                .build();

        RestTemplate template = new RestTemplate();

        Car[] cars = deserializer.getResultArrayFor(template.getForObject(getCarsQuery, String.class), Car[].class);

        model.addAttribute("cars", cars);

        return "user-settings-myCars";
    }

    @GetMapping("/settings/addCar")
    public String getAddCar(HttpSession session, Model model){
        if(session.getAttribute("user")==null){
            return "error/401";
        }
        String getCarTypesQuery = RestRequestBuilder
                .builder(WPLBaseURL)
                .addPathParam("cars")
                .addPathParam("type")
                .build();

        RestTemplate template = new RestTemplate();

        CarType[] carTypes = deserializer.getResultArrayFor(template.getForObject(getCarTypesQuery, String.class), CarType[].class);

        model.addAttribute("carTypes", carTypes);

        return "user-settings-addCar";
    }

    @PostMapping("/settings/addCar")
    public String addCar(Car car, HttpSession session, RedirectAttributes redirectAttributes){
        car.toString();

        car.setCarTypeId(CarType.of(Integer.parseInt(car.getCarTypeId().getCarTypeName())));

        User userInSession = (User) session.getAttribute("user");

        String postNewCarQuery = RestRequestBuilder
                .builder(WPLBaseURL)
                .addPathParam("cars")
                .addPathParam("user")
                .addParam("userId", userInSession.getUserId())
                .build();

        RestTemplate restTemplate = new RestTemplate();

        Car result = deserializer.getSingleItemFor(restTemplate.postForObject(postNewCarQuery, car, String.class), Car.class);

        redirectAttributes.addAttribute("message", "Auto o nr rejestracyjnym "+car.getCarRegistrationNumber()+" zostało zapisane");

        return "redirect:/user/settings/confirm";
    }

    @GetMapping("/rides")
    public String getUsersRides(HttpSession session){
        if(session.getAttribute("user")==null){
            return "error/401";
        }
        return "user-rides";
    }

    @GetMapping("/searchHistory")
    public String getUserSearchHistory(HttpSession session){
        if(session.getAttribute("user")==null){
            return "error/401";
        }
        return "user-search-history";
    }

    @GetMapping("/settings/confirm")
    public String getConfirmation(@RequestParam("message") String message, Model model, HttpSession session){
        if(session.getAttribute("user")==null){
            return "error/401";
        }
        model.addAttribute("message", message);
        return "user-settings-confirm";
    }

    public String generatePasswordHash(String password) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] table = digest.digest(
                password.getBytes(StandardCharsets.UTF_8));
        return new String(Hex.encode(table));
    }
}
