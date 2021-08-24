package study.querydsl.repository;

import com.querydsl.core.QueryResults;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.Member;

import javax.persistence.EntityManager;
import java.util.List;

import static org.springframework.util.StringUtils.countOccurrencesOf;
import static org.springframework.util.StringUtils.hasText;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

/** [중요] Custom이 붙은 인터페이스를 구현하는 클래스의 네이밍 규칙
 *  MemberRepository 라는 인터페이스 이름을 그대로 가저가자.
 *  이 MemberRepository라는 문자열 뒤에 반드시 "Impl"을 꼭 붙여야한다.
 * */
public class MemberRepositoryImpl implements MemberRepositoryCustom{

    private final JPAQueryFactory queryFactory; // QueryDSL을 쓰기 위함.

    public MemberRepositoryImpl(EntityManager em){
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public List<MemberTeamDto> search(MemberSearchCondition condition){ // DTO 로 프로젝션.
        return queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"), member.username, member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")
                ))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .fetch();
    }

    @Override
    public Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable) {
        // offset: 몇 번을 스킵하고, 몇 번째부터 시작할 것이다.
        // limit : 한 페이지에, 최대 몇 개까지 보여줄 것이다.
        // fetchResult() 를 쓰면, count쿼리 한 번, content 쿼리 한 번, 이렇게 쿼리를 2번 실행한다.

        QueryResults<MemberTeamDto> results = queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"), member.username, member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")
                ))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetchResults();

        List<MemberTeamDto> content = results.getResults(); // 실제 데이터를 꺼내서 content 에 저장
        long total = results.getTotal(); // 총 데이터 개수를 저장

        return new PageImpl<>(content, pageable, total);
    }

    /** total count 쿼리를 분리했을 때 이득
     *  조인이 필요없거나 간단한게 조회해도 count 쿼리를 실행할 수 있는 경우가 있다.
     *  웬만하면 count 쿼리는 분리해두는 것이 좋다.  */
    @Override
    public Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable) {

        // content만 가져오는 쿼리
        List<MemberTeamDto> content =         List<MemberTeamDto> content = queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"), member.username, member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")
                ))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // count만 가져오는 쿼리
        JPAQuery<Member> countQuery = queryFactory
                .select(member)
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                );
        // countQuery.fetchCount() 를 호출해야 카운트 쿼리가 실행된다.

        // count 쿼리를 실행할 필요가 없을때는, countQuery.fetchCount()를 실행하지 않는다.
        // return PageableExecutionUtils.getPage(content, pageable, () -> countQuery.fetchCount() );
        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchCount );

    }

    private BooleanExpression usernameEq(String username) {
        return hasText(username) ? member.username.eq(username) : null;
    }
    private BooleanExpression teamNameEq(String teamName) {
        return hasText(teamName) ? team.name.eq(teamName) : null;
    }

    private BooleanExpression ageGoe(Integer ageGoe) {
        return ageGoe != null ? member.age.goe(ageGoe) : null;
    }

    private BooleanExpression ageLoe(Integer ageLoe) {
        return ageLoe != null ? member.age.loe(ageLoe) : null;
    }
}
