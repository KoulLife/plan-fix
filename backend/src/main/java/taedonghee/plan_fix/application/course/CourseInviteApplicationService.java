package taedonghee.plan_fix.application.course;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import taedonghee.plan_fix.domain.course.CourseModel;
import taedonghee.plan_fix.domain.course.CourseRepository;
import taedonghee.plan_fix.domain.course.CourseStatus;
import taedonghee.plan_fix.infrastructure.course.*;
import taedonghee.plan_fix.infrastructure.user.UserJpaRepository;
import taedonghee.plan_fix.support.error.CoreException;
import taedonghee.plan_fix.support.error.ErrorType;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseInviteApplicationService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int INVITE_VALID_DAYS = 7;
    private final CourseRepository courseRepository;
    private final CourseInviteJpaRepository inviteRepository;
    private final CourseMemberJpaRepository memberRepository;
    private final UserJpaRepository userRepository;

    @Transactional
    public CourseInviteResult createInvite(Long ownerId, Long courseId, CourseMemberRole memberRole, String frontendBaseUrl) {
        CourseModel course = activeCourse(courseId);
        course.ensureOwner(ownerId);
        if (memberRole == null || memberRole == CourseMemberRole.OWNER) {
            throw new CoreException(ErrorType.BAD_REQUEST, "초대 권한은 VIEWER 또는 EDITOR만 가능합니다.");
        }
        OffsetDateTime now = OffsetDateTime.now();
        String token = newToken();
        CourseInviteJpaEntity invite = inviteRepository.save(CourseInviteJpaEntity.builder()
                .courseId(courseId).createdByUserId(ownerId).token(token).memberRole(memberRole)
                .createdAt(now).expiresAt(now.plusDays(INVITE_VALID_DAYS)).build());
        return CourseInviteResult.from(invite, frontendBaseUrl.replaceAll("/$", "") + "/course-invites/" + token);
    }

    @Transactional
    public CourseInviteAcceptResult accept(Long userId, String token) {
        CourseInviteJpaEntity invite = inviteRepository.findByToken(token)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "초대 링크를 찾을 수 없습니다."));
        if (!invite.getExpiresAt().isAfter(OffsetDateTime.now())) {
            throw new CoreException(ErrorType.BAD_REQUEST, "만료된 초대 링크입니다.");
        }
        CourseModel course = activeCourse(invite.getCourseId());
        if (course.userId().equals(userId)) return new CourseInviteAcceptResult(course.courseId(), false, true);
        if (memberRepository.existsByCourseIdAndUserId(course.courseId(), userId)) return new CourseInviteAcceptResult(course.courseId(), false, true);
        memberRepository.save(CourseMemberJpaEntity.builder().courseId(course.courseId()).userId(userId)
                .role(invite.getMemberRole()).createdAt(OffsetDateTime.now()).build());
        return new CourseInviteAcceptResult(course.courseId(), true, false);
    }

    public List<CourseMemberResult> members(Long ownerId, Long courseId) {
        CourseModel course = activeCourse(courseId); course.ensureOwner(ownerId);
        List<CourseMemberResult> result = new java.util.ArrayList<>();
        result.add(userRepository.findById(course.userId()).map(u -> new CourseMemberResult(course.userId(), u.getName(), u.getUsername(), CourseMemberRole.OWNER, course.createdAt())).orElse(new CourseMemberResult(course.userId(), null, "소유자", CourseMemberRole.OWNER, course.createdAt())));
        memberRepository.findByCourseIdOrderByCreatedAtAsc(courseId).stream().map(member -> userRepository.findById(member.getUserId()).map(u -> new CourseMemberResult(member.getUserId(), u.getName(), u.getUsername(), member.getRole(), member.getCreatedAt())).orElse(new CourseMemberResult(member.getUserId(), null, "사용자 #" + member.getUserId(), member.getRole(), member.getCreatedAt()))).forEach(result::add);
        return result;
    }
    public List<PendingInviteResult> pendingInvites(Long ownerId, Long courseId) { CourseModel course = activeCourse(courseId); course.ensureOwner(ownerId); return inviteRepository.findByCourseIdOrderByCreatedAtDesc(courseId).stream().filter(i -> i.getExpiresAt().isAfter(OffsetDateTime.now())).map(i -> new PendingInviteResult(i.getToken(), i.getMemberRole(), i.getCreatedAt(), i.getExpiresAt())).toList(); }


    @Transactional
    public void removeMember(Long ownerId, Long courseId, Long memberUserId) {
        CourseModel course = activeCourse(courseId); course.ensureOwner(ownerId);
        memberRepository.deleteByCourseIdAndUserIdAndRoleIn(courseId, memberUserId, List.of(CourseMemberRole.VIEWER, CourseMemberRole.EDITOR));
    }

    @Transactional public void updateMemberRole(Long ownerId, Long courseId, Long memberUserId, CourseMemberRole role) {
        CourseModel course = activeCourse(courseId); course.ensureOwner(ownerId);
        if (role == null || role == CourseMemberRole.OWNER) throw new CoreException(ErrorType.BAD_REQUEST, "멤버 권한은 VIEWER 또는 EDITOR만 가능합니다.");
        CourseMemberJpaEntity member = memberRepository.findByCourseIdOrderByCreatedAtAsc(courseId).stream().filter(m -> m.getUserId().equals(memberUserId)).findFirst().orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "멤버를 찾을 수 없습니다."));
        member.changeRole(role);
    }

    @Transactional public void cancelInvite(Long ownerId, Long courseId, String token) {
        CourseModel course = activeCourse(courseId); course.ensureOwner(ownerId);
        CourseInviteJpaEntity invite = inviteRepository.findByToken(token).orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "초대 링크를 찾을 수 없습니다."));
        if (!invite.getCourseId().equals(courseId)) throw new CoreException(ErrorType.NOT_FOUND, "초대 링크를 찾을 수 없습니다.");
        inviteRepository.delete(invite);
    }

    public boolean isMember(Long userId, Long courseId) { return userId != null && memberRepository.existsByCourseIdAndUserId(courseId, userId); }
    public boolean canEdit(Long userId, Long courseId) { return userId != null && memberRepository.existsByCourseIdAndUserIdAndRole(courseId, userId, CourseMemberRole.EDITOR); }

    /** 로그인 전 초대 링크 화면에 보여줄 정보. 수락은 인증 후 POST로만 가능하다. */
    public CourseInvitePreview preview(String token) {
        CourseInviteJpaEntity invite = inviteRepository.findByToken(token)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "초대 링크를 찾을 수 없습니다."));
        if (!invite.getExpiresAt().isAfter(OffsetDateTime.now())) throw new CoreException(ErrorType.BAD_REQUEST, "만료된 초대 링크입니다.");
        CourseModel course = activeCourse(invite.getCourseId());
        return new CourseInvitePreview(course.courseId(), course.title(), invite.getMemberRole(), invite.getExpiresAt());
    }

    private CourseModel activeCourse(Long courseId) {
        return courseRepository.findById(courseId).filter(c -> c.status() == CourseStatus.ACTIVE)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "course not found. courseId=" + courseId));
    }
    private String newToken() {
        byte[] bytes = new byte[32];
        do { RANDOM.nextBytes(bytes); String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); if (!inviteRepository.existsByToken(token)) return token; } while (true);
    }

    public record CourseInviteResult(String token, String inviteUrl, CourseMemberRole memberRole, OffsetDateTime expiresAt) {
        static CourseInviteResult from(CourseInviteJpaEntity invite, String url) { return new CourseInviteResult(invite.getToken(), url, invite.getMemberRole(), invite.getExpiresAt()); }
    }
    public record CourseInviteAcceptResult(Long courseId, boolean joined, boolean alreadyMember) { }
    public record CourseMemberResult(Long userId, String name, String username, CourseMemberRole role, OffsetDateTime joinedAt) { }
    public record PendingInviteResult(String token, CourseMemberRole role, OffsetDateTime createdAt, OffsetDateTime expiresAt) { }
    public record CourseInvitePreview(Long courseId, String courseTitle, CourseMemberRole memberRole, OffsetDateTime expiresAt) { }
}
