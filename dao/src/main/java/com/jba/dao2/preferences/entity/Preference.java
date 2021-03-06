package com.jba.dao2.preferences.entity;

import lombok.*;

import javax.persistence.*;

@Data
@Entity
@NoArgsConstructor
@RequiredArgsConstructor
@Table(name = "Preference")
public class Preference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PK_PREFERENCE_ID")
    private long preferenceId;

    @Column(name="PREFERENCE_NAME")
    @NonNull
    private String preferenceName;

    @Column(name = "PREFERENCE_TYPE")
    @NonNull
    private String preferenceType;

    public static Preference of(int id){
        Preference preference = new Preference();
        preference.setPreferenceId(id);
        return preference;
    }
}
