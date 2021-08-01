package study.querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@SpringBootTest
@Transactional
public class QuerydslBasicJoinTest { /** [기본 문법 조인 ] */

    @Autowired
    EntityManager em;
    JPAQueryFactory queryFactory; // querydsl은 jpql 빌더의 역할을 한다.

    /** 기본조인
     * 팀 A에 소속된 모든 회원 조회
     * join(조인 대상, 별칭으로 사용할 Q타입) -> join(member.team, team)
     */
    @Test
    public void join(){
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        // username만 뽑아낸다. member1과 member2만 있는지 확인한다.
        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
        /* [ 생성된 JPQL ]
        select member1
        from Member member1
        inner join member1.team as team
        where team.name = 'teamA'1 */
    }

    /** 세타조인 : 연관관계가 없어도 조인 가능하다!
     *  from 절 내에 엔티티 이름 나열.
     *
     *  나열된 테이블을 전부 가져와서 조인 한 다음, where 절로 필터링. 단, DB마다 성능최적화 하는 기본방법은 다르다.
     *  주의 : outer(외부) 조인 불가능.-> 조인 on을 사용하면 외부 조인 가능.  */
    @Test
    public void theta_join(){ // 예제 ) 회원의 이름이 팀 이름과 같은 회원을 조회하라.
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");

        /* select member1
            from Member member1, Team team
            where member1.username = team.name */
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
