package com.odiparpack.transport_planning.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.odiparpack.transport_planning.service.GLSAlgorithmService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@RestController
@RequestMapping("/planning")
public class PlanningController {

    @Autowired
    private GLSAlgorithmService glsAlgorithmService;

    @GetMapping("/run")
    public String runGLSAlgorithm(@RequestParam("startDate") String startDateStr, @RequestParam("endDate") String endDateStr) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            // Parse dates from the request parameters
            Date startDate = sdf.parse(startDateStr);
            Date endDate = sdf.parse(endDateStr);

            // Run the GLS algorithm with the provided dates
            glsAlgorithmService.runGLS(startDate, endDate);

            return "GLS Algorithm executed successfully between " + startDateStr + " and " + endDateStr;
        } catch (ParseException e) {
            e.printStackTrace();
            return "Error parsing dates: " + e.getMessage();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error executing GLS Algorithm: " + e.getMessage();
        }
    }
}
