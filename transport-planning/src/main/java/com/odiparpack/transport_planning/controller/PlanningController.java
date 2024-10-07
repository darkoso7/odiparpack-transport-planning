package com.odiparpack.transport_planning.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.odiparpack.transport_planning.service.GLSAlgorithmService;

@RestController
@RequestMapping("/planning")
public class PlanningController {

    @Autowired
    private GLSAlgorithmService glsAlgorithmService;

    @GetMapping("/run")
    public String runGLSAlgorithm() {
        try {
            glsAlgorithmService.runGLS(null);
            return "GLS Algorithm executed successfully.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error executing GLS Algorithm: " + e.getMessage();
        }
    }
}
