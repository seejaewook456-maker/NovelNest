package org.example.domain.worldsetting.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.domain.novel.entity.Novel;
import org.example.global.common.BaseEntity;

@Entity
@Table(name = "world_settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorldSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "novel_id", nullable = false)
    private Novel novel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorldSettingCategory category;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Builder
    private WorldSetting(Novel novel, WorldSettingCategory category, String title, String content) {
        this.novel = novel;
        this.category = category;
        this.title = title;
        this.content = content;
    }

    public void update(WorldSettingCategory category, String title, String content) {
        this.category = category;
        this.title = title;
        this.content = content;
    }
}
