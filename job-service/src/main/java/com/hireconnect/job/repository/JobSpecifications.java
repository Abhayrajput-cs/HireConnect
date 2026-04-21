package com.hireconnect.job.repository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.hireconnect.job.domain.Job;

import jakarta.persistence.criteria.Predicate;

public final class JobSpecifications {

    private JobSpecifications() {
    }

    public static Specification<Job> withFilters(
        String title,
        String category,
        String location,
        Double salaryMin,
        Double salaryMax,
        Integer experienceRequired,
        String status,
        Integer postedBy
    ) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (title != null && !title.isBlank()) {
                predicates.add(builder.like(builder.lower(root.get("title")), "%" + title.trim().toLowerCase() + "%"));
            }
            if (category != null && !category.isBlank()) {
                predicates.add(builder.equal(builder.lower(root.get("category")), category.trim().toLowerCase()));
            }
            if (location != null && !location.isBlank()) {
                predicates.add(builder.like(builder.lower(root.get("location")), "%" + location.trim().toLowerCase() + "%"));
            }
            if (salaryMin != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("salaryMin"), salaryMin));
            }
            if (salaryMax != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("salaryMax"), salaryMax));
            }
            if (experienceRequired != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("experienceRequired"), experienceRequired));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(builder.equal(builder.lower(root.get("status")), status.trim().toLowerCase()));
            }
            if (postedBy != null) {
                predicates.add(builder.equal(root.get("postedBy"), postedBy));
            }

            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
