package com.harnessagent.skill;

import com.harnessagent.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/skills")
public class SkillController {

    private final SkillService skillService;

    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    @GetMapping
    @Operation(summary = "List active approved skills")
    public ApiResponse<List<SkillDefinitionResponse>> listActiveSkills() {
        return ApiResponse.ok(skillService.listActiveSkills());
    }
}
