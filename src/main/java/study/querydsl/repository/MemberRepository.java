package study.querydsl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import study.querydsl.entity.Member;

import java.util.List;

/** MemberRepositoryCustom 도 상속받는다.  */
public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {

    // 쿼리메소드  select m from Member m where m.username = ?
    List<Member> findByUsername(String username);

    /**  특정한 기능 or 화면 or API에 맞춰진 조회 쿼리가 필요할 때,
     * 인터페이스 없이 구현체를 바로 만드는 방법도 좋은 방법이다.
     * 기본으로 Custom을 쓰는것이 좋지만, 설계에 따라 유연하게 레포지토리를 만들자. */
}
