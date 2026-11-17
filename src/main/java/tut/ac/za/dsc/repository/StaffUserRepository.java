package tut.ac.za.dsc.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import tut.ac.za.dsc.domain.StaffUser;

public interface StaffUserRepository extends JpaRepository<StaffUser, Long> {

    Optional<StaffUser> findByUsernameIgnoreCase(String username);

    long countByRoleAndActiveTrue(StaffUser.Role role);

    List<StaffUser> findAllByOrderByUsernameAsc();
}
