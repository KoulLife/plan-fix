package taedonghee.plan_fix.interfaces.api.ai;

public record AiTestResponse(
        String response,
        String model,
        String engine
) {
}
