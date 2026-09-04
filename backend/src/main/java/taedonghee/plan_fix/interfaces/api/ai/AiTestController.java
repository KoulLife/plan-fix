package taedonghee.plan_fix.interfaces.api.ai;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import taedonghee.plan_fix.application.ai.AiTestApplicationService;
import taedonghee.plan_fix.infrastructure.ai.GeminiProperties;

@Tag(name = "AI 테스트 API", description = "Gemini 및 LangGraph4j 연동 테스트용 API")
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiTestController {

    private final AiTestApplicationService aiTestApplicationService;
    private final GeminiProperties geminiProperties;

    @Operation(summary = "Gemini 직접 대화 테스트", description = "LangChain4j를 통해 Gemini 모델과 직접 대화합니다.")
    @PostMapping("/chat")
    public ResponseEntity<AiTestResponse> chat(@RequestBody AiTestRequest request) {
        String response = aiTestApplicationService.chatWithGemini(request.message());
        return ResponseEntity.ok(new AiTestResponse(response, geminiProperties.modelName(), "LangChain4j (GoogleAiGeminiChatModel)"));
    }

    @Operation(summary = "Gemini 간단 GET 대화 테스트", description = "쿼리 파라미터로 간단히 Gemini 응답을 확인합니다.")
    @GetMapping("/chat")
    public ResponseEntity<AiTestResponse> chatGet(@RequestParam(defaultValue = "안녕? 30자 이내로 짧게 자기소개해줘.") String message) {
        String response = aiTestApplicationService.chatWithGemini(message);
        return ResponseEntity.ok(new AiTestResponse(response, geminiProperties.modelName(), "LangChain4j (GoogleAiGeminiChatModel)"));
    }
 
    @Operation(summary = "LangGraph4j + Gemini 워크플로우 테스트", description = "LangGraph4j StateGraph 노드를 거쳐 Gemini 응답을 생성합니다.")
    @PostMapping("/graph-chat")
    public ResponseEntity<AiTestResponse> graphChat(@RequestBody AiTestRequest request) throws Exception {
        String response = aiTestApplicationService.chatWithLangGraph(request.message());
        return ResponseEntity.ok(new AiTestResponse(response, geminiProperties.modelName(), "LangGraph4j (StateGraph Workflow)"));
    }
}
