package com.umbrellapoint.service;

import com.umbrellapoint.config.JwtTokenProvider;
import com.umbrellapoint.dto.auth.*;
import com.umbrellapoint.entity.User;
import com.umbrellapoint.entity.UserCredit;
import com.umbrellapoint.exception.BusinessException;
import com.umbrellapoint.exception.ResourceNotFoundException;
import com.umbrellapoint.repository.UserCreditRepository;
import com.umbrellapoint.repository.UserRepository;
import com.umbrellapoint.util.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final UserCreditRepository userCreditRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       UserCreditRepository userCreditRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.userCreditRepository = userCreditRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("用户名已存在");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("邮箱已被注册");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user = userRepository.save(user);
        logger.info("用户注册成功: {}", user.getUsername());

        UserCredit credit = new UserCredit();
        credit.setUserId(user.getId());
        credit.setScore(100);
        credit.setOverdueCount(0);
        userCreditRepository.save(credit);

        String token = jwtTokenProvider.generateToken(user.getUsername());
        return new LoginResponse(token, convertToDto(user));
    }

    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsernameOrEmail(), request.getPassword())
        );

        User user = userRepository.findByUsername(request.getUsernameOrEmail())
                .orElseGet(() -> userRepository.findByEmail(request.getUsernameOrEmail())
                        .orElseThrow(() -> new ResourceNotFoundException("用户", "用户名/邮箱", request.getUsernameOrEmail())));

        String token = jwtTokenProvider.generateToken(user.getUsername());
        logger.info("用户登录成功: {}", user.getUsername());
        return new LoginResponse(token, convertToDto(user));
    }

    public UserDto getCurrentUser() {
        String username = SecurityUtils.getCurrentUsername();
        if (username == null) {
            throw new BusinessException(401, "用户未登录");
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户", "用户名", username));
        return convertToDto(user);
    }

    public Long getCurrentUserId() {
        String username = SecurityUtils.getCurrentUsername();
        if (username == null) {
            return null;
        }
        return userRepository.findByUsername(username).map(User::getId).orElse(null);
    }

    @Transactional
    public UserDto updateCurrentUser(UpdateUserRequest request) {
        String username = SecurityUtils.getCurrentUsername();
        if (username == null) {
            throw new BusinessException(401, "用户未登录");
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户", "用户名", username));

        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new BusinessException("用户名已存在");
            }
            user.setUsername(request.getUsername());
        }
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new BusinessException("邮箱已被注册");
            }
            user.setEmail(request.getEmail());
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }

        user = userRepository.save(user);
        logger.info("用户信息更新成功: {}", user.getUsername());
        return convertToDto(user);
    }

    private UserDto convertToDto(User user) {
        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getAvatar(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
