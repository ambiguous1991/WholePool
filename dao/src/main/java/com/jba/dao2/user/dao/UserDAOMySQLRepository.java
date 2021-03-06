package com.jba.dao2.user.dao;

import com.jba.dao2.preferences.entity.Preference;
import com.jba.dao2.preferences.entity.UsersPreference;
import com.jba.dao2.user.enitity.User;
import com.jba.dao2.user.enitity.UserType;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@EnableTransactionManagement
public class UserDAOMySQLRepository implements UserDAO {

    @Autowired
    private SessionFactory sessionFactory;

    private final static Logger logger = Logger.getLogger(UserDAOMySQLRepository.class);

    @Override
    public List<User> getAllUsers() {
        Session session = sessionFactory.getCurrentSession();

        return session.createQuery("from User", User.class).getResultList();
    }

    public User getUserById(int id) {
        Session session = sessionFactory.getCurrentSession();
        try {
            List<User> users = session.
                    createQuery("from User where userId=:id", User.class).
                    setParameter("id", id).
                    getResultList();

            if (users.size() == 0) {
                throw new IllegalArgumentException("No users found with id: " + id);
            } else if (users.size() > 1) {
                throw new IllegalArgumentException("There are more than one users of this id");
            } else {
                return users.get(0);
            }
        } catch (Exception e) {
            logger.error("Error getting user with id: " + id);
            throw e;
        }
    }

    public User getUserByEmail(String email) {
        Session session = sessionFactory.getCurrentSession();
        try {
            List<User> users = session.
                    createQuery("from User where emailAddress=:email", User.class).
                    setParameter("email", email).
                    getResultList();

            if (users.size() == 0) {
                throw new IllegalArgumentException("No user found with email: " + email);
            } else if (users.size() > 1) {
                throw new IllegalArgumentException("There are more than one users of this email");
            } else {
                return users.get(0);
            }
        } catch (Exception e) {
            logger.error("Error getting user with email: " + email);
            throw e;
        }
    }

    public User getUserByName(String name) {
        Session session = sessionFactory.getCurrentSession();
        try {
            List<User> users = session.
                    createQuery("from User where userName=:name", User.class).
                    setParameter("name", name).
                    getResultList();

            if (users.size() == 0) {
                throw new IllegalArgumentException("No users found with name: " + name);
            } else if (users.size() > 1) {
                throw new IllegalArgumentException("There are more than one users of this name");
            } else {
                return users.get(0);
            }
        } catch (Exception e) {
            logger.error("Error getting user with name: " + name);
            throw e;
        }
    }

    public String getUserPasswordHash(User user) {
        User fromDB = getUserById(user.getUserId());

        return fromDB.getPasswordHash();
    }

    public void updateUserData(User user) {
        Session session = sessionFactory.getCurrentSession();

        session.saveOrUpdate(user);
    }

    @Override
    public void updateUserPasswordHash(User user, String hash) {
        Session session = sessionFactory.getCurrentSession();

        session.createQuery("update User u set u.passwordHash=:hash where u.userId=:id")
                .setParameter("hash", hash)
                .setParameter("id", user.getUserId())
                .executeUpdate();
    }

    @Override
    public List<UserType> getUserTypes() {
        Session session = sessionFactory.getCurrentSession();

        List<UserType> userTypes = session
                .createQuery("from UserType", UserType.class)
                .getResultList();

        return userTypes;
    }

    public List<Preference> getAllPreferences() {
        Session session = sessionFactory.getCurrentSession();
        return session.createQuery("from Preference", Preference.class).getResultList();
    }

    @Override
    public Preference getPreferenceById(long id) {
        Session session = sessionFactory.getCurrentSession();

        return session
                .createQuery("from Preference p where p.id=:id", Preference.class)
                .setParameter("id", id)
                .getSingleResult();
    }

    @Transactional
    public UsersPreference setPreference(User user, Preference preference, String value) {
        Session session = sessionFactory.getCurrentSession();

        UsersPreference usersPreference = new UsersPreference(user, preference, value);

        session.saveOrUpdate(usersPreference);

        return usersPreference;
    }

    public Map<String, String> getUsersPreferences(User user) {
        Session session = sessionFactory.getCurrentSession();

        try {
            int userId = user.getUserId();

            List<UsersPreference> usersPreferences = session.
                    createQuery("from UsersPreference up where up.user=:user", UsersPreference.class).
                    setParameter("user", user).
                    getResultList();

            Map<String, String> result = new HashMap<>();

            usersPreferences.forEach(
                    preference -> result.put(preference.getPreference().getPreferenceName(), preference.getValue())
            );

            return result;
        } catch (Exception e) {
            logger.error("Error retrieving users preferences!", e);
            throw e;
        }
    }

    @Override
    public boolean deletePreference(UsersPreference usersPreference) {
        Session session = sessionFactory.getCurrentSession();

        try {
            session.delete(usersPreference);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public User addNewUser(User user) {
        Session session = sessionFactory.getCurrentSession();
        session.saveOrUpdate(user);
        return user;
    }

    public void resetPassword(User user, String passwordHash) {
        user.setPasswordHash(passwordHash);
        Session session = sessionFactory.getCurrentSession();
        session.saveOrUpdate(user);
    }
}
