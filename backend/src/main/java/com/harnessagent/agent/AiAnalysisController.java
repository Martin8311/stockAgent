package com.harnessagent.agent;

import com.harnessagent.security.AuthenticatedUser;
import com.harnessagent.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiAnalysisController {

    private final AiModelCatalog modelCatalog;
    private final InvestmentAnalysisService investmentAnalysisService;

    public AiAnalysisController(AiModelCatalog modelCatalog, InvestmentAnalysisService investmentAnalysisService) {
        this.modelCatalog = modelCatalog;
        this.investmentAnalysisService = investmentAnalysisService;
    }

    @GetMapping("/models")
    @Operation(summary = "List selectable AI models and billing metadata")
    public ApiResponse<List<AiModelDescriptor>> models() {
        return ApiResponse.ok(modelCatalog.models());
    }

    @PostMapping("/analysis")
    @Operation(summary = "Run a structured investment analysis with a selected AI model")
    public ApiResponse<InvestmentAnalysisResponse> analyze(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody AiAnalysisRequest request
    ) {
        return ApiResponse.ok(investmentAnalysisService.analyze(user.id(), user.email(), request));
    }
}
