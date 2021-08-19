package study.querydsl.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

import static org.springframework.util.StringUtils.hasText;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@Repository
public class MemberJpaRepository { // repository : 엔티티를 조회하기 위해 DB에 접근하는 계층

    private final EntityManager em; // 순수 JPA
    private final JPAQueryFactory queryFactory; // Querydsl

    public MemberJpaRepository(EntityManager em){
        this.em = em;
        this.queryFactory = new JPAQueryFactory(em);
    }

    public List<MemberTeamDto> searchByBuilder(MemberSearchCondition condition){

        // null 말고 빈문자열 "" 요게 들어오는 경우가 있다. -> StringUtils.hasText() 로 확인하자.

        BooleanBuilder builder = new BooleanBuilder();
        if (hasText(condition.getUsername())) { // command + shift + enter : 괄호자동완성
            builder.and(member.username.eq(condition.getUsername()));
        }
        if (hasText(condition.getTeamName())) {
            builder.and(team.name.eq(condition.getTeamName()));
        }
        if(condition.getAgeGoe() != null){
            builder.and(member.age.goe(condition.getAgeGoe()));
        }
        if(condition.getAgeLoe() != null){
            builder.and(member.age.loe(condition.getAgeLoe()));
        }

        return queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"), member.username, member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")
                ))
                .from(member)
                .leftJoin(member.team, team)
                .where(builder)
                .fetch();
    }

    public void save(Member member){
        em.persist(member);
    }

    public Optional<Member> findById(Long id){
        Member findMember = em.find(Member.class, id);
        return Optional.ofNullable(findMember);
    }

    public List<Member> findAll(){
        return em.createQuery("select m from Member m", Member.class)
                .getResultList();
    }

    public List<Member> findAll_Querydsl(){ // 순수 JPA를 querydsl로 바꾸자.
        return queryFactory
                .selectFrom(member)
                .fetch();
    }

    public List<Member> findByUsername(String username){
        return em.createQuery(
                "select m from Member m where m.username = :username", Member.class
        ).setParameter("username", username)
                .getResultList();
    }

    public List<Member> findByUsername_Querydsl(String username){
        return queryFactory
                .selectFrom(member)
                .where(member.username.eq(username))
                .fetch();
    }

}
