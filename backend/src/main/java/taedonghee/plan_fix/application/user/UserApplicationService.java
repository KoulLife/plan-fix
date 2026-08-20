package taedonghee.plan_fix.application.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import taedonghee.plan_fix.domain.user.PasswordEncryptor;
import taedonghee.plan_fix.domain.user.UserCredentialModel;
import taedonghee.plan_fix.domain.user.UserCredentialRepository;
import taedonghee.plan_fix.domain.user.UserModel;
import taedonghee.plan_fix.domain.user.UserRepository;
import taedonghee.plan_fix.support.error.CoreException;
import taedonghee.plan_fix.support.error.ErrorType;

import java.util.List;

/**
 * 사용자 Application Service
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserApplicationService {

    private final UserRepository userRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final PasswordEncryptor passwordEncryptor;

    /**
     * 사용자 생성 처리
     */
    @Transactional
    public UserResult create(UserCommand.Create command) {
        validateUniqueUsername(command.username());
        validateUniqueEmail(command.email());
        validateUniqueLoginId(command.loginId());

        UserModel savedUser = userRepository.save(UserModel.create(command.username(), command.email()));
        String encryptedPassword = command.password() == null ? null : passwordEncryptor.encrypt(command.password());
        userCredentialRepository.save(UserCredentialModel.create(savedUser.getId(), command.loginId(), encryptedPassword));

        return UserResult.from(savedUser);
    }

    /**
     * 사용자 프로필 수정 처리
     */
    @Transactional
    public UserResult update(Long id, UserCommand.Update command) {
        UserModel user = getOrThrow(id);
        if (!user.getUsername().equals(command.username())) {
            validateUniqueUsername(command.username());
        }
        if (command.email() != null && !command.email().equals(user.getEmail())) {
            validateUniqueEmail(command.email());
        }
        return UserResult.from(userRepository.save(user.updateProfile(command.username(), command.email())));
    }

    /**
     * 사용자 탈퇴 상태 변경 처리
     */
    @Transactional
    public UserResult withdraw(Long id) {
        return UserResult.from(userRepository.save(getOrThrow(id).withdraw()));
    }

    /**
     * 사용자 단건 조회 처리
     */
    public UserResult get(Long id) {
        return UserResult.from(getOrThrow(id));
    }

    /**
     * 사용자 전체 조회 처리
     */
    public List<UserResult> getAll() {
        return userRepository.findAll().stream()
                .map(UserResult::from)
                .toList();
    }

    /**
     * 사용자 조회 실패 예외 처리
     */
    private UserModel getOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "User not found. id=" + id));
    }

    /**
     * username 중복 검증
     */
    private void validateUniqueUsername(String username) {
        if (userRepository.existsByUsername(username)) {
            throw new CoreException(ErrorType.CONFLICT, "username already exists. username=" + username);
        }
    }

    /**
     * email 중복 검증
     */
    private void validateUniqueEmail(String email) {
        if (email != null && userRepository.existsByEmail(email)) {
            throw new CoreException(ErrorType.CONFLICT, "email already exists. email=" + email);
        }
    }

    /**
     * login_id 중복 검증
     */
    private void validateUniqueLoginId(String loginId) {
        if (loginId != null && userCredentialRepository.existsByLoginId(loginId)) {
            throw new CoreException(ErrorType.CONFLICT, "loginId already exists. loginId=" + loginId);
        }
    }
}
