package tut.ac.za.dsc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tut.ac.za.dsc.domain.AppSetting;

public interface AppSettingRepository extends JpaRepository<AppSetting, String> {
}
