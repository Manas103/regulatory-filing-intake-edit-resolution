package com.manas.regfiling.repository;

import com.manas.regfiling.domain.EditFailure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EditFailureRepository extends JpaRepository<EditFailure, Long> {

    List<EditFailure> findByFilingId(Long filingId);
}
