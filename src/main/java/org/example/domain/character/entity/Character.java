package org.example.domain.character.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.domain.novel.entity.Novel;
import org.example.global.common.BaseEntity;

@Entity
@Table(name = "characters")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Character extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "novel_id", nullable = false)
    private Novel novel;

    @Column(nullable = false)
    private String name;

    @Column
    private String role;

    @Column
    private Integer age;

    @Column(columnDefinition = "TEXT")
    private String personality;

    @Column(columnDefinition = "TEXT")
    private String speechStyle;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder
    private Character(Novel novel, String name, String role, Integer age,
                      String personality, String speechStyle, String description) {
        this.novel = novel;
        this.name = name;
        this.role = role;
        this.age = age;
        this.personality = personality;
        this.speechStyle = speechStyle;
        this.description = description;
    }

    public void update(String name, String role, Integer age,
                       String personality, String speechStyle, String description) {
        this.name = name;
        this.role = role;
        this.age = age;
        this.personality = personality;
        this.speechStyle = speechStyle;
        this.description = description;
    }
}
