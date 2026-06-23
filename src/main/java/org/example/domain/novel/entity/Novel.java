package org.example.domain.novel.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.domain.user.entity.User;
import org.example.global.common.BaseEntity;

@Entity
@Table(name = "novels")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Novel extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String genre;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder
    private Novel(User user, String title, String genre, String description) {
        this.user = user;
        this.title = title;
        this.genre = genre;
        this.description = description;
    }

    public void update(String title, String genre, String description) {
        this.title = title;
        this.genre = genre;
        this.description = description;
    }
}
