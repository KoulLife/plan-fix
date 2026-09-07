package taedonghee.plan_fix.application.ai;

import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.state.AgentState;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiTestApplicationService {

    private final ObjectProvider<ChatLanguageModel> chatLanguageModelProvider;


    /**
     * LangChain4j + Gemini 직접 호출 테스트
     */
    public String chatWithGemini(String message) {
        ChatLanguageModel model = getChatModel();
        log.info("[AiTest - LangChain4j] Sending message: {}", message);
        String response = model.chat(message);
        log.info("[AiTest - LangChain4j] Response: {}", response);
        return response;
    }

    /**
     * LangGraph4j + Gemini 그래프 워크플로우 호출 테스트
     */
    public String chatWithLangGraph(String message) throws Exception {
        ChatLanguageModel model = getChatModel();

        // 1. StateGraph 정의 (AgentState 기반)
        StateGraph<AgentState> workflow = new StateGraph<>(AgentState::new)
                .addNode("gemini_node", node_async(state -> {
                    String input = state.value("input")
                            .map(Object::toString)
                            .orElse("");
                    log.info("[LangGraph4j Node] Processing input: {}", input);
                    String result = model.chat(input);
                    return Map.of("output", result);
                }))
                .addEdge(START, "gemini_node")
                .addEdge("gemini_node", END);

        // 2. 그래프 컴파일
        CompiledGraph<AgentState> app = workflow.compile();

        // 3. 그래프 실행
        Optional<AgentState> finalState = app.invoke(Map.of("input", message));

        return finalState
                .flatMap(s -> s.value("output"))
                .map(Object::toString)
                .orElse("No response generated from LangGraph4j");
    }

    private ChatLanguageModel getChatModel() {
        ChatLanguageModel model = chatLanguageModelProvider.getIfAvailable();
        if (model == null) {
            throw new IllegalStateException("GEMINI_API_KEY가 설정되지 않았거나 ChatLanguageModel이 초기화되지 않았습니다. application-secret.yml을 확인해주세요.");
        }
        return model;
    }
}
