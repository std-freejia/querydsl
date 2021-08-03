package study.querydsl;

import com.querydsl.core.Tuple;
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

    /**  조인 on절
     * 1. 조인 대상 필터링
     * 2. 연관관계 없는 엔티티 외부 조인
     *
     * 예) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     * JPQL: select m, t from Member left join m.team t on t.name = 'teamA'
     */

    @Test
    public void join_on_filtering(){
        List<Tuple> leftJoinresult = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                .fetch();
        /**  leftJoin 이니까 member는 일단 다 가지고 오고, team은 'teamA'만 가져와서 조인.
         select 가 여러가지 타입으로 조회하니까 Tuple 타입으로 패치한다.
         leftJoin 쓸 때만 on 절을 사용하고, 내부조인 사용 시 where 를 쓰자. */

        for (Tuple tuple : leftJoinresult) {
            System.out.println("tuple = " + tuple);
        }
        /* 외부 조인 leftJoin 결과
        tuple = [Member(id=3, username=member1, age=10), Team(id=1, name=teamA)]
        tuple = [Member(id=4, username=member2, age=20), Team(id=1, name=teamA)]
        tuple = [Member(id=5, username=member3, age=30), null]
        tuple = [Member(id=6, username=member4, age=40), null]
         */

        List<Tuple> innerJoinResult = queryFactory
                .select(member, team)
                .from(member)
                .join(member.team, team).on(team.name.eq("teamA"))
                .fetch();
        for (Tuple tuple : innerJoinResult) {
            System.out.println("tuple = " + tuple);
        }
        /*  내부 조인 join 결과 : where 절로 필터링 하는 것과 결과가 동일.
        tuple = [Member(id=3, username=member1, age=10), Team(id=1, name=teamA)]
        tuple = [Member(id=4, username=member2, age=20), Team(id=1, name=teamA)]
         */
    }

    /** 조인 on절 : 연관관계 없는 엔티티 외부 조인
     * 예) 회원의 이름이 팀 이름과 같은 대상을 외부 조인 하라.
     * */
    @Test
    public void join_on_no_relation(){
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();
        /** 특징: from(member).leftJoin(team)
         * 아이디로 매칭하지 않는다.
         * on 절의 member.username으로만 조인한다.(member.team 아이디와 team 이렇게 조인하지 않는게 특징) */

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
        /*
        tuple = [Member(id=3, username=member1, age=10), null]
        tuple = [Member(id=4, username=member2, age=20), null]
        tuple = [Member(id=5, username=member3, age=30), null]
        tuple = [Member(id=6, username=member4, age=40), null]
        tuple = [Member(id=7, username=teamA, age=0), Team(id=1, name=teamA)]
        tuple = [Member(id=8, username=teamB, age=0), Team(id=2, name=teamB)]
        tuple = [Member(id=9, username=teamC, age=0), null]
         */
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
