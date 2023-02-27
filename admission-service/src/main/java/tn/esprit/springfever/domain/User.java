package tn.esprit.springfever.domain;

import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;


@Entity
@Getter
@Setter
public class User {

    @Id
    @Column(nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userID;

    @Column
    private String etatuser;

    @OneToOne
    private DemandeAdmission demandeAdmission;

    @OneToMany(mappedBy = "rDVuser")
    private Set<RDV> rDVuserRDVs;

}