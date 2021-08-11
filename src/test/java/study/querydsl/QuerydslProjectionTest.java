package study.querydsl;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.extern.slf4j.Slf4j;
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
import javax.persistence.TypedQuery;

import java.util.List;

import static study.querydsl.entity.QMember.member;

@Slf4j
@SpringBootTest
@Transactional
public class QuerydslProjectionTest { /** [중급 문법] */

    @Autowired
    EntityManager em;
    JPAQueryFactory queryFactory;

    @Test
    public void simpleProjection(){ // 이름만 가져올 때.
        // 프로젝션 대상이 하나일 때, 단순하다.
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("username = " + s);
        }
    }

    @Test
    public void tupleProjection(){ // 이름과 나이. 두가지를 가져올 때.
        /**
         * querydsl이 제공하는 Tuple.
         * Tuple을 레포지토리 계층 안에서 쓰는건 괜찮은데, 서비스나 비즈니스 로직이 알면 좋지 않음!!
         * 왜냐하면 Tuple을 모든 계층에서 다루면, 의존성을 만들어내는 것이기 때문임.
         * Tuple에 담긴 데이터를 레포지토리에서 서비스로 던질때,  DTO로 변환해서 나가게 하자.
         */

        List<Tuple> tupleResult = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();
        for (Tuple tuple : tupleResult) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }
    }

    @Test
    public void findDtoByJPQL(){ // JPQL 의 new Operation 문법
        List<MemberDto> result = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age)" +
                " from Member m", MemberDto.class)
                .getResultList();
        /**
         * DTO의 생성자가 꼭 있어야 함.
         * DTO의 package 이름을 장황하게 모두 적어야해서 지저분함.
         */
    }

    /** Querydsl이 Bean을 생성하는 방식 3가지. DTO 반환할 때 사용한다.
     * 1) 프로퍼티 접근
     * 2) 필드 직접 접근
     * 3) 생성자 사용
     * */
    @Test
    public void findDtoBySetter(){
        /** 기본생성자 필요 -> Projections.bean(MemberDto.class 여기서 필요.
         * querydsl이 MemberDto 클래스의 기본생성자를 필요로 한다. 따라서 Dto에 @NoArgsConstructor 붙이기.
         * 값 세팅할 때 setter 를 이용한다. */
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }
    @Test
    public void findDtoByField(){
        /** getter, setter 없이 fields 에 값을 직접 준다. */

        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findUserDto(){
        /** 엔티티의 필드 명과, Dto의 필드 명이 다를 때!
         * .as("Dto필드명") 을 통해 필드 명을 명시해준다.
         *
         * [서브쿼리 ]
         * ExpressionUtils.as(쿼리, "칼럼명")*/

        QMember memberSub = new QMember("memberSub");

        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),

                        ExpressionUtils.as(JPAExpressions
                        .select(memberSub.age.max())
                                .from(memberSub), "age"
                        )
                ))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    @Test
    public void findDtoByConstructor(){
        /**  Projections.constructor  */
        List<UserDto> result = queryFactory
                .select(Projections.constructor(UserDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    @Test
    public void findDtoByQueryProjection(){
        /** 1) DTO의 생성자에 @QueryProjection 를 붙인다.
         *  2) ./gredlew compileQuerydsl 후에 QMemberDto 생성 확인
         *  컴파일러로 타입을 체크할 수 있으므로 가장 안전한 방법이다.
         * */
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
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
