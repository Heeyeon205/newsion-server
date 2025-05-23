package com.newsvision.mypage.dto.response;

import com.newsvision.global.Utils.TimeUtil;
import com.newsvision.news.entity.News;
import com.newsvision.user.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserScrapListResponse {
    private Long newsId;
    private String image;
    private String categoryName;
    private String title;
    private String nickname;
    private String createAt;

    public static UserScrapListResponse from(News news) {
        return UserScrapListResponse.builder()
                .newsId(news.getId())
                .image(news.getImage())
                .categoryName(news.getCategory().getName())
                .title(news.getTitle())
                .nickname(news.getUser().getNickname())
                .createAt(TimeUtil.formatRelativeTime(news.getCreatedAt()))
                .build();
    }
}
