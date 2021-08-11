package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;

@Slf4j
@SpringBootTest
@Transactional
public class QuerydslDynamicQueryTest {

    /** [중급 문법]
     *  동적 쿼리 - BooleanBuilder 사용
     *  동적 쿼리 - Where 다중 파라미터 사용
     *
     * */

    @Autowired
    EntityManager em;
    JPAQueryFactory queryFactory;

    @Test
    public void dynamicQuery_BooleanBuilder(){
        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> result = searchMember1(usernameParam, ageParam); // 파라미터 2개 넣음!
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond){

        BooleanBuilder builder = new BooleanBuilder(); /** 생성자에 초기값 지정 가능 */
        if(usernameCond != null){
            builder.and(member.username.eq(usernameCond));
        }
        if(ageCond != null){
            builder.and(member.age.eq(ageCond));
        }

        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    @Test
    public void dynamicQuery_WhereParam(){
        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> result = searchMember2(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    /** 동적 쿼리 - [ Where 다중 파라미터 사용 ] */
    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        // 쿼리 where 절이 null 이면 무시된다.

        return queryFactory
                .selectFrom(member)
                // .where(usernameEq(usernameCond), ageEq(ageCond))
                .where(allEq(usernameCond, ageCond))
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) {
        /** 조건 마다 메서드를 빼 놓는 것이 장점이 많다. 조건들을 여러 방식으로 조립할 때 재사용하기 좋다.
         쿼리 where 절이 null 이면 무시된다. */
        return (usernameCond != null) ? member.username.eq(usernameCond):null;
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond):null;
    }

    // 조립하려면 각 조건 메서드들을 BooleanExpression 쓰기
    private BooleanExpression allEq(String usernameCond, Integer ageCond){
        /** 단, 조립할 때, null 처리에 주의하자.*/
        return usernameEq(usernameCond).and(ageEq(ageCond));
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