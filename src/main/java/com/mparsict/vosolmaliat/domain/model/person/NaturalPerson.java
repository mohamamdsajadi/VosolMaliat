package com.mparsict.vosolmaliat.domain.model.person;
import com.mparsict.vosolmaliat.domain.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="natural_person")
@org.hibernate.annotations.Check(constraints="(national_code IS NOT NULL AND fida IS NULL) OR (national_code IS NULL AND fida IS NOT NULL)")
@Getter @Setter @NoArgsConstructor
public class NaturalPerson extends BaseEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="national_code",unique=true,updatable=false) private String nationalCode;
    @Column(name="fida",unique=true,updatable=false) private String fida;
    @OneToOne(mappedBy="naturalPerson",cascade=CascadeType.ALL,fetch=FetchType.EAGER)
    private NaturalPersonExtraInfo extraInfo;
    @OneToMany(mappedBy="naturalPerson",cascade=CascadeType.ALL)
    private java.util.List<NaturalPersonExtraInfoHistory> profileHistory = new java.util.ArrayList<>();
    @Builder
    public NaturalPerson(Long id,String nationalCode,String fida,String firstName,String lastName,String fatherName,String birthDate) {
        this.id=id;this.nationalCode=nationalCode;this.fida=fida;
        if(firstName!=null || lastName!=null || fatherName!=null || birthDate!=null) {
            profile().setFirstName(firstName);profile().setLastName(lastName);
            profile().setFatherName(fatherName);profile().setBirthDate(birthDate);
        }
    }
    public void updateProfile(String firstName,String lastName,String fatherName,String birthDate) {
        if (java.util.Objects.equals(getFirstName(),firstName) && java.util.Objects.equals(getLastName(),lastName) && java.util.Objects.equals(getFatherName(),fatherName) && java.util.Objects.equals(getBirthDate(),birthDate)) return;
        if (id != null && extraInfo != null) profileHistory.add(NaturalPersonExtraInfoHistory.from(this, extraInfo));
        setFirstName(firstName); setLastName(lastName); setFatherName(fatherName); setBirthDate(birthDate);
    }
    private NaturalPersonExtraInfo profile() {
        if(extraInfo==null) extraInfo=NaturalPersonExtraInfo.builder().naturalPerson(this).build();
        return extraInfo;
    }
    public String getFirstName(){return extraInfo==null?null:extraInfo.getFirstName();}
    public String getLastName(){return extraInfo==null?null:extraInfo.getLastName();}
    public String getFatherName(){return extraInfo==null?null:extraInfo.getFatherName();}
    public String getBirthDate(){return extraInfo==null?null:extraInfo.getBirthDate();}
    public void setFirstName(String v){profile().setFirstName(v);}
    public void setLastName(String v){profile().setLastName(v);}
    public void setFatherName(String v){profile().setFatherName(v);}
    public void setBirthDate(String v){profile().setBirthDate(v);}
}
