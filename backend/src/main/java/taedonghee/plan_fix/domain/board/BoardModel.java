package taedonghee.plan_fix.domain.board;

import taedonghee.plan_fix.support.error.CoreException;
import taedonghee.plan_fix.support.error.ErrorType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 게시글 Model
 */
public class BoardModel {

    private static final int TITLE_MAX_LENGTH = 150;
    private static final int CONTENT_MAX_LENGTH = 20000;
    private static final int THUMBNAIL_MAX_LENGTH = 500;
    private static final int IMAGE_MAX_COUNT = 20;

    // 에디터 HTML 중 script, 이벤트 핸들러, javascript URL처럼 위험한 입력을 차단한다
    private static final Pattern UNSAFE_CONTENT_PATTERN = Pattern.compile(
            "(?is)<\\s*(script|iframe|object|embed)|\\son\\w+\\s*=|javascript\\s*:"
    );

    private final Long boardId;
    private final Long courseId;
    private final Long userId;
    private final String title;
    private final String content;
    private final String thumbnail;
    private final BoardStatus status;
    private final long viewCount;
    private final long likeCount;
    private final long commentCount;
    private final List<BoardImageModel> images;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    /**
     * 게시글 생성 및 복원 시 공통으로 사용하는 생성자
     */
    private BoardModel(Long boardId, Long courseId, Long userId, String title, String content, String thumbnail,
                       BoardStatus status, long viewCount, long likeCount, long commentCount,
                       List<BoardImageModel> images, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        validate(courseId, userId, title, content, thumbnail, viewCount, likeCount, commentCount, images);
        this.boardId = boardId;
        this.courseId = courseId;
        this.userId = userId;
        this.title = normalizeRequired(title);
        this.content = normalizeRequired(content);
        this.images = List.copyOf(images == null ? List.of() : images);
        this.thumbnail = normalizeThumbnail(thumbnail, this.images);
        this.status = status == null ? BoardStatus.ACTIVE : status;
        this.viewCount = viewCount;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 신규 게시글 생성
     */
    public static BoardModel create(Long userId, Long courseId, String title, String content, String thumbnail,
                                    List<BoardImageModel> images) {
        OffsetDateTime now = OffsetDateTime.now();
        return new BoardModel(null, courseId, userId, title, content, thumbnail, BoardStatus.ACTIVE,
                0L, 0L, 0L, images, now, now);
    }

    /**
     * 저장된 게시글 정보를 기반으로 BoardModel 복원
     * DB에 이미 저장되어 있던 Board 데이터를 다시 도메인 모델로 복원할 때 사용
     */
    public static BoardModel reconstruct(Long boardId, Long courseId, Long userId, String title, String content,
                                         String thumbnail, BoardStatus status, long viewCount, long likeCount,
                                         long commentCount, List<BoardImageModel> images,
                                         OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        return new BoardModel(boardId, courseId, userId, title, content, thumbnail, status,
                viewCount, likeCount, commentCount, images, createdAt, updatedAt);
    }

    /**
     * 게시글 제목, 내용, 연결 코스, 이미지 목록 수정
     */
    public BoardModel update(Long courseId, String title, String content, String thumbnail,
                             List<BoardImageModel> images) {
        ensureActive();
        return new BoardModel(boardId, courseId, userId, title, content, thumbnail, status,
                viewCount, likeCount, commentCount, images, createdAt, OffsetDateTime.now());
    }

    /**
     * 게시글 삭제 상태 변경
     */
    public BoardModel delete() {
        if (status == BoardStatus.DELETED) {
            return this;
        }
        return new BoardModel(boardId, courseId, userId, title, content, thumbnail, BoardStatus.DELETED,
                viewCount, likeCount, commentCount, images, createdAt, OffsetDateTime.now());
    }

    /**
     * 요청 사용자가 게시글 작성자인지 검증
     */
    public void ensureOwner(Long requestedUserId) {
        if (!userId.equals(requestedUserId)) {
            throw new CoreException(ErrorType.FORBIDDEN, "board access denied. boardId=" + boardId);
        }
    }

    /**
     * 삭제된 게시글 수정 방지
     */
    private void ensureActive() {
        if (status == BoardStatus.DELETED) {
            throw new CoreException(ErrorType.CONFLICT, "deleted board. boardId=" + boardId);
        }
    }

    /**
     * 게시글 필수값, 길이, 에디터 HTML, 이미지 목록 검증
     */
    private void validate(Long courseId, Long userId, String title, String content, String thumbnail,
                          long viewCount, long likeCount, long commentCount, List<BoardImageModel> images) {
        if (courseId != null && courseId < 1) {
            throw new CoreException(ErrorType.BAD_REQUEST, "courseId must be positive.");
        }
        if (userId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "userId is required.");
        }
        if (title == null || title.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "title is required.");
        }
        if (title.strip().length() > TITLE_MAX_LENGTH) {
            throw new CoreException(ErrorType.BAD_REQUEST, "title must be 150 characters or less.");
        }
        if (content == null || content.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "content is required.");
        }
        if (content.strip().length() > CONTENT_MAX_LENGTH) {
            throw new CoreException(ErrorType.BAD_REQUEST, "content must be 20000 characters or less.");
        }
        if (UNSAFE_CONTENT_PATTERN.matcher(content).find()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "content contains unsafe html.");
        }
        if (thumbnail != null && thumbnail.strip().length() > THUMBNAIL_MAX_LENGTH) {
            throw new CoreException(ErrorType.BAD_REQUEST, "thumbnail must be 500 characters or less.");
        }
        if (viewCount < 0 || likeCount < 0 || commentCount < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "counts must not be negative.");
        }
        if (images != null) {
            if (images.size() > IMAGE_MAX_COUNT) {
                throw new CoreException(ErrorType.BAD_REQUEST, "images must be 20 or less.");
            }
            if (images.stream().anyMatch(Objects::isNull)) {
                throw new CoreException(ErrorType.BAD_REQUEST, "images must not contain null.");
            }
            long distinctImageCount = images.stream().map(BoardImageModel::imageUrl).distinct().count();
            if (distinctImageCount != images.size()) {
                throw new CoreException(ErrorType.BAD_REQUEST, "images must not contain duplicates.");
            }
        }
    }

    private String normalizeRequired(String value) {
        return value.strip();
    }

    private String normalizeThumbnail(String thumbnail, List<BoardImageModel> images) {
        if (thumbnail == null || thumbnail.isBlank()) {
            // 대표 이미지가 없으면 첫 번째 본문 이미지를 대표 이미지로 사용한다
            return images.isEmpty() ? null : images.getFirst().imageUrl();
        }
        return thumbnail.strip();
    }

    public Long boardId() { return boardId; }
    public Long courseId() { return courseId; }
    public Long userId() { return userId; }
    public String title() { return title; }
    public String content() { return content; }
    public String thumbnail() { return thumbnail; }
    public BoardStatus status() { return status; }
    public long viewCount() { return viewCount; }
    public long likeCount() { return likeCount; }
    public long commentCount() { return commentCount; }
    public List<BoardImageModel> images() { return images; }
    public OffsetDateTime createdAt() { return createdAt; }
    public OffsetDateTime updatedAt() { return updatedAt; }
}
