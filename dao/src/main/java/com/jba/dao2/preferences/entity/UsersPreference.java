package com.jba.dao2.preferences.entity;

import com.jba.dao2.user.enitity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import javax.persistence.*;
import java.io.Serializable;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "UsersPreference")
public class UsersPreference implements Serializable {

    @Id
    @ManyToOne
    @JoinColumn(name = "FK_PREFERENCE_USER_ID")
    private User user;

    @Id
    @ManyToOne
    @JoinColumn(name = "FK_PREFERENCE_PREF_ID")
    private Preference preference;

    @Column(name = "PREFERENCE_VALUE")
    @NonNull
    private String value;

}
