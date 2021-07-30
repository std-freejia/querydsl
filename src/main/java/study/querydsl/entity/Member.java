package study.querydsl.entity;

import lombok.*;

import javax.persistence.*;

@Entity
@Getter @Setter // 실무에서는 setter 사용 지양하기.
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA는 기본생성자 필요함
@ToString(of={"id", "username", "age"})
public class Member {

    @Id @GeneratedValue
    @Column(name="member_id")
    private Long id;
    private String username;
    private int age;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id") // 외래키 컬럼 이름
    private Team team;

    public Member(String username){
        this(username, 0);
    }

    public Member(String username, int age){
        this(username, age, null);
    }

    public Member(String username, int age, Team team){
        this.username = username;
        this.age = age;
        if(team != null){
            changeTeam(team);
        }
    }

    public void changeTeam(Team team) { // 양방향 연관관계라서 필요함
        this.team = team;
        team.getMembers().add(this);
    }
}
