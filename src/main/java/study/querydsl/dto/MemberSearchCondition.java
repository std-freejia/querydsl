package study.querydsl.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MemberSearchCondition {
    // 회원명, 팀명, 나이

    private String username;
    private String teamName;
    private Integer ageGoe; // 나이가 크거나 같거나.
    private Integer ageLoe; // 나이가 작거나 같거나.

    public MemberSearchCondition(String username, String teamName, Integer ageGoe, Integer ageLoe) {
        this.username = username;
        this.teamName = teamName;
        this.ageGoe = ageGoe;
        this.ageLoe = ageLoe;
    }
}
