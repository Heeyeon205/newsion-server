package com.newsvision.news.controller;

import com.newsvision.global.exception.ApiResponse;
import com.newsvision.news.dto.response.GptNewsSummaryResponse;
import com.newsvision.news.entity.News;
import com.newsvision.news.repository.GptNewsRepository;
import com.newsvision.news.repository.NewsRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/gpt-news")
@Tag(name = "GPT 뉴스 컨트롤러", description = "GPT 기반 뉴스 요약 API")
public class GptNewsController {

    private final NewsRepository newsRepository;
    private final GptNewsRepository gptNewsRepository;

    @Operation(
            summary = "최근 뉴스 GPT 요약 리스트",
            description = "3일 이내 등록된 관리자 뉴스 중 상위 10개를 GPT로 요약하여 반환합니다. 요약이 없을 경우 기본 문구를 표시합니다."
    )
    @GetMapping("/main-summary")
    public ResponseEntity<ApiResponse<List<GptNewsSummaryResponse>>> getMainNewsSummaries() {
        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);
        List<News> newsList = newsRepository.findTopNewsByAdminOnly(threeDaysAgo, PageRequest.of(0, 10));

        List<GptNewsSummaryResponse> response = newsList.stream()
                .map(news -> gptNewsRepository.findByNewsId(news.getId())
                        .map(gptNews -> new GptNewsSummaryResponse(news.getId(), gptNews.getImage(), gptNews.getTitle(), gptNews.getSummary()))
                        .orElse(new GptNewsSummaryResponse(news.getId(), news.getImage(), news.getTitle(), "요약이 존재하지 않습니다."))
                ).toList();

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
