package study.querydsl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import study.querydsl.entity.Member;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long> {

    // 쿼리메소드  select m from Member m where m.username = ?
    List<Member> findByUsername(String username);


}
