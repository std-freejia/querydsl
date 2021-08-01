package study.querydsl;

import com.querydsl.core.QueryResults;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;

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
        assertThat(qlString.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQuerydsl(){ // 컴파일타임에 오류가 잡힌다.

        // QMember m = new QMember("mem"); // "m" 변수 명에 별칭을 주는건데 별로 안중요.
        // QMember m = QMember.member; // 기본 인스턴스 이름 사용
        /** [권장하는 기본 Q-Type활용]  Member.member -> "add static import" */

        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();
        // 파라미터 바인딩을 Prepared statement 에 바인딩.
        // Prepared statement : 자주 쓰는 sql 을 미리 DB가 이해하기 쉽게 해석해 놓은 것.
        // 대량 쿼리 처리 및 SQLinjection 에 대한 약간의 방어 효과 있음
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search(){
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void searchAndParam(){ // where에 콤마로 여러개 조건 나열 가능
        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1"),
                        member.age.between(10, 30)
                )
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void resultFetch(){
        /*
        // 리스트 조회
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();

        // 단건
        Member fetchOne = queryFactory
                .selectFrom(member)
                .fetchOne();

        Member fetchFirst = queryFactory
                .selectFrom(QMember.member)
                .fetchFirst(); // limit(1).fetchOne() 과 같다.
        */

        /** fetchResults() : 페이징 정보 포함, total count 쿼리 추가 실행. */
        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .fetchResults();

        results.getTotal();
        List<Member> content = results.getResults();

        // count 쿼리 : select count(member1) from Member member1
        // 성능이 더 중요할 때는 count 쿼리를 따로 실행하는 것이 좋다.
        long total = queryFactory
                .selectFrom(member)
                .fetchCount();

    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순 (desc)
     * 2. 회원 이름 오름차순 (asc)
     * 단 , 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
     */
    @Test
    public void sort(){
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
        /* select member1
            from Member member1
            where member1.age = 1001
            order by member1.age desc, member1.username asc nulls last */
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
