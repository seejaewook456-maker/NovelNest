package org.example.domain.novel.repository;

import org.example.domain.novel.entity.Novel;
import org.example.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NovelRepository extends JpaRepository<Novel, Long> {

    List<Novel> findAllByUser(User user);
}
