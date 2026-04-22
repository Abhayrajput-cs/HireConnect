package com.hireconnect.web.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hireconnect.web.domain.InvoiceRecord;

public interface InvoiceRecordRepository extends JpaRepository<InvoiceRecord, Long> {

    List<InvoiceRecord> findByRecruiterProfileIdOrderByIssuedAtDesc(Integer recruiterProfileId);

    List<InvoiceRecord> findAllByOrderByIssuedAtDesc();
}
