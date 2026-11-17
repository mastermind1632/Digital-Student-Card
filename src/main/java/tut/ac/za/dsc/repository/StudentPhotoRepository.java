package tut.ac.za.dsc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tut.ac.za.dsc.domain.StudentPhoto;

public interface StudentPhotoRepository extends JpaRepository<StudentPhoto, Long> {
}
