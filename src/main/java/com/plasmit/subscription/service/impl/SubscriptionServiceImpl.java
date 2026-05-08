package com.plasmit.subscription.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plasmit.subscription.dto.request.*;
import com.plasmit.subscription.dto.response.*;
import com.plasmit.subscription.repository.SubscriptionRepository;
import com.plasmit.subscription.security.TenantContext;
import com.plasmit.subscription.service.SubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SubscriptionServiceImpl implements SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionServiceImpl.class);

    private final SubscriptionRepository subscriptionRepository;
    private final ObjectMapper objectMapper;

    public SubscriptionServiceImpl(SubscriptionRepository subscriptionRepository, ObjectMapper objectMapper) {
        this.subscriptionRepository = subscriptionRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public Map<String, Object> createPlan(CreatePlanRequest request) {
        Long userId = TenantContext.getUserId();

        validatePlan(request);

        log.info("Creating subscription plan. name={}, createdBy={}", request.getName(), userId);

        Long planId = subscriptionRepository.createPlan(request, toJson(request.getIncludedFeatures()), userId);

        Map<String, Object> response = new HashMap<>();
        response.put("id", planId);
        return response;
    }

    @Override
    public List<SubscriptionPlanResponse> getPlans(String search, String status) {
        log.info("Fetching subscription plans. search={}, status={}, userId={}",
                search, status, TenantContext.getUserId());

        return subscriptionRepository.findPlans(search, status);
    }

    @Override
    public Map<String, Object> updatePlan(Long planId, UpdatePlanRequest request) {
        Long userId = TenantContext.getUserId();

        validatePlan(request);

        log.info("Updating subscription plan. planId={}, updatedBy={}", planId, userId);

        int updated = subscriptionRepository.updatePlan(planId, request, toJson(request.getIncludedFeatures()), userId);
        if (updated == 0) {
            throw new IllegalArgumentException("Subscription plan not found");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", planId);
        response.put("updatedAt", LocalDateTime.now());
        return response;
    }

    @Override
    public Map<String, Object> updatePlanStatus(Long planId, UpdatePlanStatusRequest request) {
        Long userId = TenantContext.getUserId();

        validatePlanStatus(request.getStatus());

        log.info("Updating plan status. planId={}, status={}, reason={}, updatedBy={}",
                planId, request.getStatus(), request.getReason(), userId);

        int updated = subscriptionRepository.updatePlanStatus(planId, request.getStatus(), userId);
        if (updated == 0) {
            throw new IllegalArgumentException("Subscription plan not found");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", planId);
        response.put("status", request.getStatus());
        return response;
    }

    @Override
    public List<HospitalSubscriptionMappingResponse> getHospitalMappings(Long hospitalId, Long planId, String status) {
        log.info("Fetching hospital subscription mappings. hospitalId={}, planId={}, status={}",
                hospitalId, planId, status);

        return subscriptionRepository.findHospitalMappings(hospitalId, planId, status);
    }

    @Override
    public Map<String, Object> assignPlanToHospital(CreateHospitalSubscriptionRequest request) {
        Long userId = TenantContext.getUserId();

        validateMapping(request);

        log.info("Assigning plan to hospital. hospitalId={}, planId={}, userId={}",
                request.getHospitalId(), request.getPlanId(), userId);

        Long mappingId = subscriptionRepository.createHospitalMapping(request, userId);

        Map<String, Object> response = new HashMap<>();
        response.put("id", mappingId);
        return response;
    }

    @Override
    public Map<String, Object> updateHospitalMapping(Long mappingId, UpdateHospitalSubscriptionRequest request) {
        Long userId = TenantContext.getUserId();

        if (request.getStatus() == null || request.getStatus().isBlank()) {
            request.setStatus("ACTIVE");
        }

        if (request.getPaymentStatus() == null || request.getPaymentStatus().isBlank()) {
            request.setPaymentStatus("PENDING");
        }

        log.info("Updating hospital mapping. mappingId={}, planId={}, status={}, paymentStatus={}",
                mappingId, request.getPlanId(), request.getStatus(), request.getPaymentStatus());

        int updated = subscriptionRepository.updateHospitalMapping(mappingId, request, userId);
        if (updated == 0) {
            throw new IllegalArgumentException("Hospital subscription mapping not found");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", mappingId);
        response.put("status", request.getStatus());
        return response;
    }

    private void validatePlan(CreatePlanRequest request) {
        if (request.getPrice() == null || request.getPrice().doubleValue() < 0) {
            throw new IllegalArgumentException("Price must be >= 0");
        }

        if (request.getUserLimit() == null || request.getUserLimit() < 1) {
            throw new IllegalArgumentException("User limit must be >= 1");
        }

        if (request.getBranchLimit() == null || request.getBranchLimit() < 1) {
            throw new IllegalArgumentException("Branch limit must be >= 1");
        }

        if (request.getStorageLimitGb() == null || request.getStorageLimitGb() < 1) {
            throw new IllegalArgumentException("Storage limit must be >= 1");
        }

        String cycle = request.getBillingCycle();
        if (!"MONTHLY".equalsIgnoreCase(cycle)
                && !"YEARLY".equalsIgnoreCase(cycle)
                && !"CUSTOM".equalsIgnoreCase(cycle)) {
            throw new IllegalArgumentException("Invalid billing cycle");
        }
    }

    private void validatePlanStatus(String status) {
        if (!"ACTIVE".equalsIgnoreCase(status)
                && !"INACTIVE".equalsIgnoreCase(status)) {
            throw new IllegalArgumentException("Invalid plan status");
        }
    }

    private void validateMapping(CreateHospitalSubscriptionRequest request) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? List.of() : value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid features JSON");
        }
    }
}