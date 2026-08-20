package taedonghee.plan_fix.domain.user;

import java.util.List;
import java.util.Optional;

/**
 * 사용자 Repository
 */
public interface UserRepository {

    /**
     * 사용자 저장
     */
    UserModel save(UserModel user);

    /**
     * 사용자 단건 조회
     */
    Optional<UserModel> findById(Long id);

    /**
     * 사용자 전체 조회
     */
    List<UserModel> findAll();

    /**
     * username 존재 여부 조회
     */
    boolean existsByUsername(String username);

    /**
     * email 존재 여부 조회
     */
    boolean existsByEmail(String email);
}
