package com.manas.regfiling.repository;

import com.manas.regfiling.domain.Filing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface FilingRepository extends JpaRepository<Filing, Long> {

    List<Filing> findByInstitutionIdAndReportTypeAndPeriodEndDateLessThanOrderByPeriodEndDateDesc(
            String institutionId, String reportType, LocalDate periodEndDate);
}
