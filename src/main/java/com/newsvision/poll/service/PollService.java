package com.newsvision.poll.service;

import com.newsvision.global.Utils.TimeUtil;
import com.newsvision.global.exception.CustomException;
import com.newsvision.global.exception.ErrorCode;
import com.newsvision.poll.dto.request.CreatePollRequest;
import com.newsvision.poll.dto.request.UpdatePollRequest;
import com.newsvision.poll.dto.request.VoteRequest;
import com.newsvision.poll.dto.response.PollListResponse;
import com.newsvision.poll.dto.response.PollOptionResponse;
import com.newsvision.poll.dto.response.PollResponse;
import com.newsvision.poll.entity.Poll;
import com.newsvision.poll.entity.PollOption;
import com.newsvision.poll.entity.PollVote;
import com.newsvision.poll.repository.PollOptionRepository;
import com.newsvision.poll.repository.PollRepository;
import com.newsvision.poll.repository.PollVoteRepository;
import com.newsvision.user.entity.User;
import com.newsvision.user.service.FollowService;
import com.newsvision.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PollService {
    private final PollRepository pollRepository;
    private final UserService userService;
    private final PollOptionService pollOptionService;
    private final PollVoteService pollVoteService;
    private final FollowService followService;
    private final PollOptionRepository pollOptionRepository;
    private final PollVoteRepository pollVoteRepository;

    public Poll findById(Long id) {
        return pollRepository.findById(id).orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
    }

    public List<PollListResponse> getAllList() {
        LocalDateTime now = LocalDateTime.now();
        List<Poll> polls = pollRepository.findAllByExpiredAtAfter(now);
        return polls.stream()
                .map(PollListResponse::new)
                .toList();
    }

    public List<PollListResponse> getRecentList() {
        LocalDateTime now = LocalDateTime.now();
        Pageable topTen = PageRequest.of(0, 10);
        List<Poll> polls = pollRepository
                .findByExpiredAtAfterOrderByExpiredAtAsc(now, topTen)
                .stream().toList();
        return polls.stream()
                .map(PollListResponse::new)
                .toList();
    }

    public PollResponse getPoll(Long pollId, Long userId) {
        Poll poll = findById(pollId);
        return convertToPollResponse(poll, userId);
    }

    private PollResponse convertToPollResponse(Poll poll, Long userId) {
        boolean voted = userId != null && pollVoteService.existsByPollVote(poll.getId(), userId);
        boolean followed = userId != null && followService.existsFollow(userId, poll.getUser().getId());

        // pollOptions를 repository에서 직접 조회
        List<PollOption> pollOptions = pollOptionRepository.findByPoll(poll);
        if (pollOptions == null) {
            pollOptions = new ArrayList<>();
        }

        return PollResponse.builder()
                .id(poll.getId())
                .title(poll.getTitle())
                .createdAt(TimeUtil.formatRelativeTime(poll.getCreatedAt()))
                .expiredAt(TimeUtil.dDayCaculate(poll.getExpiredAt()))
                .userId(poll.getUser().getId())
                .nickname(poll.getUser().getNickname())
                .image(poll.getUser().getImage())
                .icon(poll.getUser().getBadge().getIcon())
                .badgeTitle(poll.getUser().getBadge().getTitle())
                .isVote(voted)
                .followed(followed)
                .pollOptions(pollOptions.stream()
                        .map(option -> PollOptionResponse.builder()
                                .id(option.getId())
                                .content(option.getContent())
                                .count(option.getCount())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    @Transactional
    public PollResponse createPoll(CreatePollRequest request, Long userId) {
        User user = userService.findByUserId(userId);
        Poll poll = Poll.builder()
                .title(request.getTitle())
                .expiredAt(request.getExpiredAt())
                .user(user)
                .build();
        Poll savedPoll = pollRepository.save(poll);

        List<PollOption> pollOptions = request.getOptions().stream()
                .map(optionContent -> PollOption.builder()
                        .content(optionContent)
                        .poll(savedPoll)
                        .count(0)
                        .build())
                .collect(Collectors.toList());
        pollOptionRepository.saveAll(pollOptions);

        return convertToPollResponse(savedPoll, userId);
    }

    @Transactional
    public PollResponse updatePoll(Long pollId, UpdatePollRequest request, Long userId) {
        Poll poll = findById(pollId);
        userService.matchUserId(userId, poll.getUser().getId());

        LocalDateTime newExpiredAt = request.getExpiredAt();
        if (newExpiredAt == null) {
            newExpiredAt = poll.getExpiredAt(); // 기존 값 유지
        } else if (newExpiredAt.isBefore(LocalDateTime.now())) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        if (request.getOptions() == null || request.getOptions().size() < 2) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        poll.updateTitle(request.getTitle());
        poll.updateExpiredAt(newExpiredAt);

        pollOptionRepository.deleteByPollId(pollId);
        poll.getPollOptions().clear();

        List<PollOption> newOptions = request.getOptions().stream()
                .map(optionContent -> PollOption.builder()
                        .content(optionContent)
                        .poll(poll)
                        .count(0)
                        .build())
                .collect(Collectors.toList());

        poll.getPollOptions().addAll(newOptions);

        pollRepository.save(poll);
        return convertToPollResponse(poll, userId);
    }

    public boolean checkVote(Long userId, Long pollId) {
        return pollVoteRepository.existsByUserIdAndPollOption_Poll_Id(userId, pollId);
    }

    @Transactional
    public void vote(VoteRequest request, Long userId) {
        User user = userService.findByUserId(userId);
        PollOption pollOption = pollOptionService.findById(request.getOptionId());
        Poll poll = pollOption.getPoll();

        if (checkVote(user.getId(), poll.getId())) {
            throw new CustomException(ErrorCode.DUPLICATE_VOTE);
        }

        PollVote pollVote = PollVote.builder()
                .user(user)
                .pollOption(pollOption)
                .build();
        pollVoteRepository.save(pollVote);

        pollOption.updateCount(pollOption.getCount() + 1);
        pollOptionRepository.save(pollOption);
    }

    @Transactional
    public void deletePoll(Long pollId, Long userId) {
        Poll poll = findById(pollId);
        userService.matchUserId(userId, poll.getUser().getId());
        pollVoteRepository.deleteByPollOption_Poll_Id(pollId);
        pollRepository.delete(poll);
    }
}