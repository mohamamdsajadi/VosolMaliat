package com.mparsict.vosolmaliat.domain.model.person;
import com.mparsict.vosolmaliat.domain.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
@org.hibernate.annotations.Immutable
@jakarta.persistence.EntityListeners(com.mparsict.vosolmaliat.domain.model.ImmutableEntityListener.class)
@Entity @Table(name="legal_person")
@AttributeOverride(name = "createdAt", column = @Column(name = "creation_date", nullable = false, updatable = false))
@Getter @Setter @NoArgsConstructor
public class LegalPerson extends BaseEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="national_id",unique=true,nullable=false,updatable=false) private String nationalId;
    @OneToOne(mappedBy="legalPerson",cascade=CascadeType.ALL,fetch=FetchType.EAGER)
    private LegalPersonExtraInfo extraInfo;
    @OneToMany(mappedBy="legalPerson",cascade=CascadeType.ALL)
    private java.util.List<LegalPersonExtraInfoHistory> profileHistory = new java.util.ArrayList<>();
    @Builder
    public LegalPerson(Long id,String nationalId,String registrationNumber,String registrationDate,String name) {
        this.id=id;this.nationalId=nationalId;
        if(registrationNumber!=null || registrationDate!=null || name!=null) {
            profile().setRegistrationNumber(registrationNumber);profile().setRegistrationDate(com.mparsict.vosolmaliat.domain.value.PersianDates.parse(registrationDate));profile().setName(name);
        }
    }
    public void updateProfile(String name,String registrationNumber,String registrationDate) {
        updateProfile(name, getTradeName(), registrationNumber, registrationDate);
    }
    public void updateProfile(String name,String tradeName,String registrationNumber,String registrationDate) {
        if (java.util.Objects.equals(getName(),name) && java.util.Objects.equals(getTradeName(),tradeName)
                && java.util.Objects.equals(getRegistrationNumber(),registrationNumber)
                && java.util.Objects.equals(getRegistrationDate(),registrationDate)) return;
        if (id != null && extraInfo != null) profileHistory.add(LegalPersonExtraInfoHistory.from(this, extraInfo));
        var target = profile();
        target.setName(name); target.setTradeName(tradeName); target.setRegistrationNumber(registrationNumber);
        target.setRegistrationDate(com.mparsict.vosolmaliat.domain.value.PersianDates.parse(registrationDate));
    }
    private LegalPersonExtraInfo profile() {
        if(extraInfo==null) extraInfo=LegalPersonExtraInfo.builder().legalPerson(this).build();
        return extraInfo;
    }
    public void updateTradeName(String name) {
        if (java.util.Objects.equals(profile().getTradeName(), name)) return;
        if (id != null && extraInfo.getId() != null) profileHistory.add(LegalPersonExtraInfoHistory.from(this, extraInfo));
        extraInfo.setTradeName(name);
    }
    public String getRegistrationNumber(){return extraInfo==null?null:extraInfo.getRegistrationNumber();}
    public String getRegistrationDate(){return extraInfo==null?null:com.mparsict.vosolmaliat.domain.value.PersianDates.format(extraInfo.getRegistrationDate());}
    public String getName(){return extraInfo==null?null:extraInfo.getName();}
    public String getTradeName(){return extraInfo==null?null:extraInfo.getTradeName();}
    public void setRegistrationNumber(String v){profile().setRegistrationNumber(v);}
    public void setRegistrationDate(String v){profile().setRegistrationDate(com.mparsict.vosolmaliat.domain.value.PersianDates.parse(v));}
    public void setName(String v){profile().setName(v);}
}
