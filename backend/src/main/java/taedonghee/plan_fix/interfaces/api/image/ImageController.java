package taedonghee.plan_fix.interfaces.api.image;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import taedonghee.plan_fix.application.image.ImageUploadApplicationService;

import java.util.Map;

@Tag(name = "이미지 업로드 API", description = "AWS S3 이미지 업로드")
@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageUploadApplicationService imageUploadApplicationService;

    @Operation(summary = "이미지 파일 업로드", description = "Multipart 이미지를 S3에 업로드하고 영구 URL을 반환합니다.")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        String url = imageUploadApplicationService.upload(file);
        return ResponseEntity.ok(Map.of("imageUrl", url));
    }
}
