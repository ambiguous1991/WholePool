package com.jba.dao2.ride.enitity;

import com.jba.dao2.DAOConfig;
import com.jba.dao2.route.entity.Route;
import com.jba.dao2.source.entity.Source;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.time.Instant;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { DAOConfig.class })
@WebAppConfiguration
public class RideDetailsTest {

    @Autowired
    private SessionFactory sessionFactory;

    @Test
    @Transactional
    public void shouldCreateRideDetails(){
        Session session = sessionFactory.getCurrentSession();
        Route route = new Route("xxx","yyy");
        session.save(route);
        Source source = session.createQuery("from Source s where s.sourceName='localhost'",Source.class).getSingleResult();

        Ride ride = new Ride(source, route);
        session.save(ride);

        LocalDateTime departure = LocalDateTime.parse("2018-11-24T13:00:00");

        LocalDateTime arrival = departure.plusHours(2);

        RideDetails details = new RideDetails(
                ride,
                departure,
                arrival,
                2000,
                2.0,
                "Description"
        );

        session.save(details);
    }
}