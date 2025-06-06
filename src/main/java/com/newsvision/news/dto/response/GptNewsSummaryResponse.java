package com.newsvision.news.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GptNewsSummaryResponse {
        private Long id;
        private String image;
        private String title;
        private String summary;
}
