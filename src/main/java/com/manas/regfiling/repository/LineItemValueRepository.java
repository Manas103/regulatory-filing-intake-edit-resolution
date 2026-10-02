package com.manas.regfiling.repository;

import com.manas.regfiling.domain.LineItemValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LineItemValueRepository extends JpaRepository<LineItemValue, Long> {

    List<LineItemValue> findByFilingId(Long filingId);
}
