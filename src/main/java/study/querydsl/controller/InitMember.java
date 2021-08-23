package study.querydsl.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Profile("local") // application.yml: 스프링 부트 실행 시, local이라는 이름의 프로파일로 실행된다.
@Component
@RequiredArgsConstructor
public class InitMember { // 로컬에서 톰캣실행해서 동작할 때 데이터 넣고싶은 것 작성.

    private final InitMemberService initMemberService;

    @PostConstruct
    public void init(){
        initMemberService.init();
        // @PostConstruct 와 @Transactional 은 분리 해줘야 한다!
    }

    @Component
    static class InitMemberService{
        @PersistenceContext
        private EntityManager em;

        @Transactional // 데이터 초기화하는 로직 넣기
        public void init(){
            Team teamA = new Team("teamA");
            Team teamB = new Team("teamB");
            em.persist(teamA);
            em.persist(teamB);

            for (int i = 0; i < 100; i++){
                Team selectedTeam = i % 2 == 0 ? teamA : teamB;
                em.persist(new Member("member" + i, i, selectedTeam));
            }
        }
    }
}
