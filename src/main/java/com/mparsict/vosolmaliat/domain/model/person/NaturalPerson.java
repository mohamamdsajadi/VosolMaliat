package com.mparsict.vosolmaliat.domain.model.person;
import com.mparsict.vosolmaliat.domain.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
@org.hibernate.annotations.Immutable
@jakarta.persistence.EntityListeners(com.mparsict.vosolmaliat.domain.model.ImmutableEntityListener.class)
@Entity @Table(name="natural_person")
@AttributeOverride(name = "createdAt", column = @Column(name = "creation_date", nullable = false, updatable = false))
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
            refreshFullName();profile().setFatherName(fatherName);profile().setBirthDate(com.mparsict.vosolmaliat.domain.value.PersianDates.parse(birthDate));
        }
    }
    public void updateProfile(String firstName,String lastName,String fatherName,String birthDate) {
        updateProfile(firstName, lastName, fullName(firstName, lastName), fatherName, birthDate);
    }
    public void updateProfile(String firstName,String lastName,String fullName,String fatherName,String birthDate) {
        if (java.util.Objects.equals(getFirstName(),firstName) && java.util.Objects.equals(getLastName(),lastName)
                && java.util.Objects.equals(extraInfo == null ? null : extraInfo.getFullName(),fullName)
                && java.util.Objects.equals(getFatherName(),fatherName) && java.util.Objects.equals(getBirthDate(),birthDate)) return;
        if (id != null && extraInfo != null) profileHistory.add(NaturalPersonExtraInfoHistory.from(this, extraInfo));
        var target = profile();
        target.setFirstName(firstName); target.setLastName(lastName); target.setFullName(fullName);
        target.setFatherName(fatherName); target.setBirthDate(com.mparsict.vosolmaliat.domain.value.PersianDates.parse(birthDate));
    }
    private NaturalPersonExtraInfo profile() {
        if(extraInfo==null) extraInfo=NaturalPersonExtraInfo.builder().naturalPerson(this).build();
        if (extraInfo.getIsForeign() == null) extraInfo.setIsForeign(fida != null);
        return extraInfo;
    }
    private void refreshFullName() {
        String name = fullName(extraInfo.getFirstName(), extraInfo.getLastName());
        if (!name.isBlank()) extraInfo.setFullName(name);
    }
    private static String fullName(String firstName, String lastName) {
        return java.util.stream.Stream.of(firstName, lastName)
                .filter(v -> v != null && !v.isBlank()).collect(java.util.stream.Collectors.joining(" "));
    }
    public void completeFullName(String name) {
        if (java.util.Objects.equals(profile().getFullName(), name)) return;
        if (id != null && extraInfo.getId() != null) profileHistory.add(NaturalPersonExtraInfoHistory.from(this, extraInfo));
        extraInfo.setFullName(name);
    }
    public String getFirstName(){return extraInfo==null?null:extraInfo.getFirstName();}
    public String getLastName(){return extraInfo==null?null:extraInfo.getLastName();}
    public String getFatherName(){return extraInfo==null?null:extraInfo.getFatherName();}
    public String getBirthDate(){return extraInfo==null?null:com.mparsict.vosolmaliat.domain.value.PersianDates.format(extraInfo.getBirthDate());}
    public void setFirstName(String v){profile().setFirstName(v);refreshFullName();}
    public void setLastName(String v){profile().setLastName(v);refreshFullName();}
    public void setFatherName(String v){profile().setFatherName(v);}
    public void setBirthDate(String v){profile().setBirthDate(com.mparsict.vosolmaliat.domain.value.PersianDates.parse(v));}
}
