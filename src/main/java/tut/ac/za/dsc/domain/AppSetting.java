package tut.ac.za.dsc.domain;

import jakarta.persistence.*;

/** Values that must survive restarts, such as the key that signs QR codes. */
@Entity
@Table(name = "app_setting")
public class AppSetting {

    @Id
    @Column(name = "setting_key", length = 80)
    private String key;

    @Column(name = "setting_value", nullable = false, length = 2000)
    private String value;

    protected AppSetting() {
    }

    public AppSetting(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() { return key; }
    public String getValue() { return value; }
}
