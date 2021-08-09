package study.querydsl;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.extern.slf4j.Slf4j;
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
