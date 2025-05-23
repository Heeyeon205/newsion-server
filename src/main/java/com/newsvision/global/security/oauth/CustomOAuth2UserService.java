package com.newsvision.global.security.oauth;


import com.newsvision.user.entity.User;
import com.newsvision.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    @Value("${custom.default-image-url}")
    private String defaultProfileImage;

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(request);
        String registrationId = request.getClientRegistration().getRegistrationId();

        Map<String, Object> attributes = oAuth2User.getAttributes();

        OAuth2UserInfo userInfo = switch (registrationId) {
            case "google" -> new GoogleUserInfo(attributes);
            case "kakao" -> new KakaoUserInfo(attributes);
            case "naver" -> new NaverUserInfo(attributes);
            default -> throw new IllegalArgumentException("지원하지 않는 소셜 로그인입니다: " + registrationId);
        };

        String providerId = userInfo.getProviderId();
        String username = registrationId + "_" + providerId;
        String nickname = "Social_User_" + UUID.randomUUID().toString().substring(0, 6);
        String email = userInfo.getEmail();

        User user = userRepository.findByUsernameAndIsDeletedFalse(username)
                .orElseGet(() -> userRepository.save(User.builder()
                        .username(username)
                        .password("SOCIAL_LOGIN_PASSWORD")
                        .email(email)
                        .nickname(nickname)
                        .provider(User.Provider.valueOf(registrationId.toUpperCase()))
                        .providerId(providerId)
                        .image(defaultProfileImage)
                        .role(User.Role.ROLE_USER)
                        .isDeleted(false)
                        .build()));

        return new CustomOAuth2User(user, attributes);
    }
}
