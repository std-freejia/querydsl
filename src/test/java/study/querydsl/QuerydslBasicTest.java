package study.querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired EntityManager em;
    JPAQueryFactory queryFactory;

    // JPQL 작성 후 QueryDSL 로 바꾸면서 비교.
    @Test
    public void startJPQL(){ // 에러가 런타임에 발생한다.
        // member1 을 찾아라.
        Member qlString =
                em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();
        Assertions.assertThat(qlString.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQuerydsl(){ // 컴파일타임에 오류가 잡힌다.

        QMember m = new QMember("mem"); // "m" 변수 명에 별칭을 주는건데 별로 안중요.

        Member findMember = queryFactory
                .select(m)
                .from(m)
                .where(m.username.eq("member1"))
                .fetchOne();
        // 파라미터 바인딩을 Prepared statement 에 바인딩.
        // Prepared statement : 자주 쓰는 sql 을 미리 DB가 이해하기 쉽게 해석해 놓은 것.
        // 대량 쿼리 처리 및 SQLinjection 에 대한 약간의 방어 효과 있음
        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @BeforeEach // @Test 실행 전 마다 데이터 미리 세팅하기
    public void before(){

        queryFactory = new JPAQueryFactory(em); // 필드레벨에서 처리해도 동시성 문제 없다.

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");

        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }
}
