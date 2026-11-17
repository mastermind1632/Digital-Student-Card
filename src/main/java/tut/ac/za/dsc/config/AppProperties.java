package tut.ac.za.dsc.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Settings under "app." in application.properties (each can be overridden with an environment variable). */
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /** Name printed on cards, pages and emails. */
    private String institutionName = "University of Technology";
    private String institutionShortName = "UoT";
    /** Where students go for help with their card. */
    private String cardOffice = "the Student Card Office";
    private List<String> campuses = new ArrayList<>();
    private List<String> departments = new ArrayList<>();
    /** Public address of the site, used inside QR codes and emails. Empty: taken from the request. */
    private String publicBaseUrl = "";
    /** How long new and renewed cards are valid. */
    private int cardValidityMonths = 12;
    /** When true, self-registered cards wait for an administrator's approval. */
    private boolean registrationApproval = true;
    /** If set, creating the first administrator requires this key. */
    private String setupKey = "";
    /** Fills an empty database with fictional students and staff for demos. */
    private boolean demoSeed = false;
    /** Sender address for emailed cards. */
    private String mailFrom = "no-reply@student-card.local";

    public String getInstitutionName() { return institutionName; }
    public void setInstitutionName(String institutionName) { this.institutionName = institutionName; }
    public String getInstitutionShortName() { return institutionShortName; }
    public void setInstitutionShortName(String institutionShortName) { this.institutionShortName = institutionShortName; }
    public String getCardOffice() { return cardOffice; }
    public void setCardOffice(String cardOffice) { this.cardOffice = cardOffice; }
    public List<String> getCampuses() { return campuses; }
    public void setCampuses(List<String> campuses) { this.campuses = campuses; }
    public List<String> getDepartments() { return departments; }
    public void setDepartments(List<String> departments) { this.departments = departments; }
    public String getPublicBaseUrl() { return publicBaseUrl; }
    public void setPublicBaseUrl(String publicBaseUrl) { this.publicBaseUrl = publicBaseUrl; }
    public int getCardValidityMonths() { return cardValidityMonths; }
    public void setCardValidityMonths(int cardValidityMonths) { this.cardValidityMonths = cardValidityMonths; }
    public boolean isRegistrationApproval() { return registrationApproval; }
    public void setRegistrationApproval(boolean registrationApproval) { this.registrationApproval = registrationApproval; }
    public String getSetupKey() { return setupKey; }
    public void setSetupKey(String setupKey) { this.setupKey = setupKey; }
    public boolean isDemoSeed() { return demoSeed; }
    public void setDemoSeed(boolean demoSeed) { this.demoSeed = demoSeed; }
    public String getMailFrom() { return mailFrom; }
    public void setMailFrom(String mailFrom) { this.mailFrom = mailFrom; }
}
