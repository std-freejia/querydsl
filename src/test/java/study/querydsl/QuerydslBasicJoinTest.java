package study.querydsl;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import java.util.List;

import static com.querydsl.jpa.JPAExpressions.*;
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

    @PersistenceUnit
    EntityManagerFactory emf;

    /** fetch join */
    @Test
    public void fetchJoinNo(){ // 패치 조인 사용 안한 예
        em.flush();
        em.clear();

        // Member 의 필드인 Team은 LAZY로 되어 있다.
        // -> Member 조회할 때 Team은 프록시 객체로만 가져온다. Team의 데이터에 접근하는 시점에 실제로 DB에 쿼리를 실행한다.

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        // isLoaded() 캐시에 로딩된 엔티티인지 여부를 알려준다.
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam()); // "Team 이 로딩됬니?"
        assertThat(loaded).as("패치 조인 미적용").isFalse();
    }

    @Test
    public void fetchJoinUse(){ /**  패치 조인 사용한 예 */
        em.flush();
        em.clear(); // 캐시 초기화

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        // isLoaded() 캐시에 로딩된 엔티티인지 여부를 알려준다.
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam()); // "Team 이 로딩됬니?"
        assertThat(loaded).as("패치 조인 적용").isTrue(); // Team 까지 로딩 됨.
    }

    /** 서브 쿼리
     *
     * 예) 나이가 가장 많은 회원 조회
     * */
    @Test
    public void subQuery() throws Exception{

        // member 별칭을 따로 줘야 할 때 별칭 다른 객체를 생성
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();
        /* 나이가 가장 많은 사람의 나이를 먼저 구한다.
        *  서브쿼리:  JPAExpressions.select(memberSub.age.max()).from(memberSub)  */

        assertThat(result).extracting("age").containsExactly(40);
    }

    @Test /** 서브쿼리 goe(greater or equal) 사용:  나이가 평균 이상인 회원 조회 */
    public void subQueryGoe() throws Exception{

        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age").containsExactly(30, 40);
    }

    @Test /** 서브쿼리 in : 10살 초과인 회원 */
    public void subQueryIn() throws Exception{

        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10)) // 10살 초과.
                ))
                .fetch();

        assertThat(result).extracting("age").containsExactly(20, 30, 40);
    }


    @Test /** select 절 서브쿼리 : 이름, 평균나이 조회   */
    public void selectSubQuery() throws Exception{

        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = queryFactory
                .select(member.username,
                        select(memberSub.age.avg())
                            .from(memberSub))
                .from(member)
                .fetch();

        /** [ from 절의 서브쿼리 한계 ]
         * from 절의 서브쿼리(인라인 뷰)는 지원하지 않는다.
         * where, select 등 다른 절에서 서브쿼리 가능하다.
         * 해결방안 : 서브쿼리를 join으로 변경한다. (대부분 가능하다.) 또는 쿼리를 2번 분리해서 실행한다.
         * */

        for (Tuple tuple : result) {
            System.out.println("tuple.get(member.username) = " + tuple.get(member.username));
            System.out.println("tuple = " + tuple.get(select(memberSub.age.avg()).from(memberSub)));
        }
    }

    /** Case문 : select, where, order by 절에서 사용. */
    @Test
    public void basicCase(){ // 단순한 경우 : 10살, 20살, 그 외.

        List<String> result = queryFactory
                            .select(member.age
                                    .when(10).then("열살")
                                    .when(20).then("스무살")
                                    .otherwise("기타"))
                            .from(member)
                            .fetch();
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void complexCase(){
        /** 복잡한 경우 : 0~20일때, 21~30일때, 나머지,.
         *  그렇지만, DB에서는 최소한의 그룹핑과 필터링을 해오고, 아래 같은 case와 같은 로직은 프로그래밍 하는 것을 권장한다. */
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타"))
                .from(member)
                .fetch();
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    /** 상수 */
    @Test
    public void constant(){
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple); // 출력값 [username, A]
        }
    }

    /** 상수, 문자 더하기 */
    @Test
    public void concat(){ // 출력하고 싶은값 : username_age
        // age는 문자열이니까 stringValue() 메서드로 문자열변환

        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();
        for (String s : result) {
            System.out.println("s = " + s); // s = member1_10
        }
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
