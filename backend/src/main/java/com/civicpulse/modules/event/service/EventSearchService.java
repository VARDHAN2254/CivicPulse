package com.civicpulse.modules.event.service;

import com.civicpulse.common.response.PagedResponse;
import com.civicpulse.modules.event.dto.EventDto;
import com.civicpulse.modules.event.dto.EventSearchCriteria;
import com.civicpulse.modules.event.model.Event;
import com.civicpulse.modules.event.model.EventStatus;
import com.civicpulse.modules.event.model.EventVisibility;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventSearchService {

    @PersistenceContext
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public PagedResponse<EventDto> searchEvents(EventSearchCriteria criteria, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // 1. Main Query
        CriteriaQuery<Event> cq = cb.createQuery(Event.class);
        Root<Event> root = cq.from(Event.class);

        // Fetch join relations for performance
        root.fetch("organization", JoinType.LEFT);
        root.fetch("category", JoinType.LEFT);
        root.fetch("createdBy", JoinType.LEFT);

        List<Predicate> predicates = buildPredicates(cb, root, criteria);
        cq.where(predicates.toArray(new Predicate[0]));

        // Sorting
        applySorting(cb, cq, root, criteria);

        TypedQuery<Event> query = entityManager.createQuery(cq);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        List<Event> results = query.getResultList();

        // 2. Count Query
        CriteriaQuery<Long> countCq = cb.createQuery(Long.class);
        Root<Event> countRoot = countCq.from(Event.class);
        countCq.select(cb.count(countRoot));
        countCq.where(buildPredicates(cb, countRoot, criteria).toArray(new Predicate[0]));
        Long totalCount = entityManager.createQuery(countCq).getSingleResult();

        Page<EventDto> page = new PageImpl<>(
                results.stream().map(EventDto::fromEntity).collect(Collectors.toList()),
                pageable,
                totalCount
        );

        return PagedResponse.from(page);
    }

    private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<Event> root, EventSearchCriteria criteria) {
        List<Predicate> predicates = new ArrayList<>();

        // Public visibility and active discovery status only
        predicates.add(cb.equal(root.get("visibility"), EventVisibility.PUBLIC));
        predicates.add(root.get("status").in(EventStatus.PUBLISHED, EventStatus.REGISTRATION_OPEN, EventStatus.REGISTRATION_CLOSED));

        // Keyword query
        if (criteria.getQuery() != null && !criteria.getQuery().isBlank()) {
            String pattern = "%" + criteria.getQuery().trim().toLowerCase() + "%";
            Predicate titleMatch = cb.like(cb.lower(root.get("title")), pattern);
            Predicate descMatch = cb.like(cb.lower(root.get("description")), pattern);
            Predicate venueMatch = cb.like(cb.lower(root.get("venueName")), pattern);
            Predicate cityMatch = cb.like(cb.lower(root.get("city")), pattern);
            predicates.add(cb.or(titleMatch, descMatch, venueMatch, cityMatch));
        }

        // Category filter
        if (criteria.getCategory() != null && !criteria.getCategory().isBlank()) {
            predicates.add(cb.equal(root.get("category").get("slug"), criteria.getCategory().trim()));
        }

        // Organization filter
        if (criteria.getOrganizationId() != null) {
            predicates.add(cb.equal(root.get("organization").get("id"), criteria.getOrganizationId()));
        }

        // Location Type
        if (criteria.getLocationType() != null) {
            predicates.add(cb.equal(root.get("locationType"), criteria.getLocationType()));
        }

        // City
        if (criteria.getCity() != null && !criteria.getCity().isBlank()) {
            predicates.add(cb.like(cb.lower(root.get("city")), "%" + criteria.getCity().trim().toLowerCase() + "%"));
        }

        // Date range
        if (criteria.getFromDate() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("startTime"), criteria.getFromDate()));
        } else {
            // Default: Only upcoming or ongoing events
            predicates.add(cb.greaterThan(root.get("endTime"), Instant.now()));
        }

        if (criteria.getToDate() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("startTime"), criteria.getToDate()));
        }

        // Available capacity only
        if (Boolean.TRUE.equals(criteria.getAvailableOnly())) {
            predicates.add(cb.lt(root.get("currentRegistrationCount"), root.get("capacity")));
        }

        return predicates;
    }

    private void applySorting(CriteriaBuilder cb, CriteriaQuery<Event> cq, Root<Event> root, EventSearchCriteria criteria) {
        String sortBy = criteria.getSortBy() != null ? criteria.getSortBy() : "startTime";
        boolean isAsc = "ASC".equalsIgnoreCase(criteria.getSortDirection());

        if ("popularity".equalsIgnoreCase(sortBy)) {
            cq.orderBy(isAsc ? cb.asc(root.get("currentRegistrationCount")) : cb.desc(root.get("currentRegistrationCount")));
        } else if ("createdAt".equalsIgnoreCase(sortBy)) {
            cq.orderBy(isAsc ? cb.asc(root.get("createdAt")) : cb.desc(root.get("createdAt")));
        } else {
            // Default: startTime
            cq.orderBy(isAsc ? cb.asc(root.get("startTime")) : cb.desc(root.get("startTime")));
        }
    }
}
