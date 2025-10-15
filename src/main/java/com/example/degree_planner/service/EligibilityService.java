package com.example.degree_planner.service;

import com.example.degree_planner.data.PrereqRepository;
import com.example.degree_planner.domain.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class EligibilityService {
    private final PrereqRepository prereqs;

    public Map<String, Status> evaluate(Set<String> completed, Collection<String> allCourseCodes) {
        Map<String, Status> out = new LinkedHashMap<>();
        for (String code : allCourseCodes) {
            if (completed.contains(code)) {
                out.put(code, Status.COMPLETED);
                continue;
            }
            var req = prereqs.requiredCodes(code); // returns [] if none
            boolean ok = req.isEmpty() || completed.containsAll(req);
            out.put(code, ok ? Status.ELIGIBLE : Status.LOCKED);
        }
        return out;
    }
}