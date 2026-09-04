package taedonghee.plan_fix.application.image;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import taedonghee.plan_fix.infrastructure.s3.S3Properties;
import taedonghee.plan_fix.support.error.CoreException;
import taedonghee.plan_fix.support.error.ErrorType;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageUploadApplicationService {

    private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png", "webp", "gif");
    private static final long MAX_FILE_SIZE = 15 * 1024 * 1024; // 15MB

    private final ObjectProvider<S3Client> s3ClientProvider;
    private final S3Properties s3Properties;

    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "업로드할 파일이 비어 있습니다.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new CoreException(ErrorType.BAD_REQUEST, "파일 크기는 15MB 이하만 가능합니다.");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = extractExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new CoreException(ErrorType.BAD_REQUEST, "지원하지 않는 이미지 형식입니다. (jpg, jpeg, png, webp, gif 지원)");
        }

        S3Client s3Client = s3ClientProvider.getIfAvailable();
        if (s3Client == null || s3Properties.bucket() == null || s3Properties.bucket().isBlank()) {
            throw new CoreException(ErrorType.INTERNAL_ERROR, "AWS S3 설정(버킷 또는 인증 키)이 아직 완료되지 않았습니다. application-secret.yml을 확인해주세요.");
        }

        String key = "boards/" + UUID.randomUUID() + "." + extension;
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(s3Properties.bucket())
                    .key(key)
                    .contentType(file.getContentType() != null ? file.getContentType() : "image/" + extension)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            String url = String.format("https://%s.s3.%s.amazonaws.com/%s",
                    s3Properties.bucket(), s3Properties.region(), key);
            log.info("S3 이미지 업로드 성공: {}", url);
            return url;
        } catch (IOException e) {
            log.error("S3 파일 스트림 읽기 오류", e);
            throw new CoreException(ErrorType.INTERNAL_ERROR, "파일 업로드 중 오류가 발생했습니다.");
        }
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "jpg";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
}
