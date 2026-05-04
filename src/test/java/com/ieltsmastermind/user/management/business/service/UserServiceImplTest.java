package com.ieltsmastermind.user.management.business.service;

import com.ieltsmastermind.common.query.IncludeSpec;
import com.ieltsmastermind.user.management.domain.dto.UserCreateRequestDto;
import com.ieltsmastermind.user.management.domain.dto.UserResponseDto;
import com.ieltsmastermind.user.management.domain.dto.UserUpdateRequestDto;
import com.ieltsmastermind.user.management.domain.entity.User;
import com.ieltsmastermind.user.management.persistence.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private IncludeSpec includes;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void create_whenEmailAndPhoneAreUnique_shouldSaveUserAndReturnUserId() {
        UserCreateRequestDto request = createRequest();

        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("0123456789")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId("user-1");
            return user;
        });

        UserResponseDto result = userService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo("user-1");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getEmail()).isEqualTo("john@example.com");
        assertThat(savedUser.getPhoneNumber()).isEqualTo("0123456789");
        assertThat(savedUser.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(savedUser.getFirstname()).isEqualTo("John");
        assertThat(savedUser.getLastname()).isEqualTo("Doe");
        assertThat(savedUser.getCountry()).isEqualTo("Vietnam");
        assertThat(savedUser.getTimezone()).isEqualTo("Asia/Ho_Chi_Minh");
        assertThat(savedUser.getAvatarUrl()).isEqualTo("avatar.png");
        assertThat(savedUser.getIsActive()).isTrue();
        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(savedUser.getLastLoginAt()).isNull();

        verify(userRepository).existsByEmail("john@example.com");
        verify(userRepository).existsByPhoneNumber("0123456789");
        verify(passwordEncoder).encode("Password123");
    }

    @Test
    void create_whenPhoneNumberIsNull_shouldSkipPhoneDuplicateCheck() {
        UserCreateRequestDto request = createRequest();
        request.setPhoneNumber(null);

        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId("user-1");
            return user;
        });

        UserResponseDto result = userService.create(request);

        assertThat(result.getUserId()).isEqualTo("user-1");

        verify(userRepository).existsByEmail("john@example.com");
        verify(userRepository, never()).existsByPhoneNumber(anyString());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void create_whenEmailAlreadyExists_shouldThrowRuntimeException() {
        UserCreateRequestDto request = new UserCreateRequestDto();
        request.setEmail("john@example.com");
        request.setPassword("Password123");

        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.create(request)
        );

        assertThat(exception.getMessage()).isEqualTo("Email already in use: john@example.com");

        verify(userRepository).existsByEmail("john@example.com");
        verify(userRepository, never()).existsByPhoneNumber(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void create_whenPhoneAlreadyExists_shouldThrowRuntimeException() {
        UserCreateRequestDto request = new UserCreateRequestDto();
        request.setEmail("john@example.com");
        request.setPhoneNumber("0123456789");
        request.setPassword("Password123");

        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("0123456789")).thenReturn(true);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.create(request)
        );

        assertThat(exception.getMessage()).isEqualTo("Phone already in use: 0123456789");

        verify(userRepository).existsByEmail("john@example.com");
        verify(userRepository).existsByPhoneNumber("0123456789");
        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void getById_whenUserExists_shouldReturnUserWithAllIncludedFields() {
        String userId = "user-1";
        User user = fullUser(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        mockIncludes(
                "email",
                "phonenumber",
                "isactive",
                "createdat",
                "lastloginat",
                "role",
                "firstname",
                "lastname",
                "dateofbirth",
                "gender",
                "country",
                "timezone",
                "avatarurl",
                "targetband",
                "targetlisteningband",
                "targetreadingband",
                "targetwritingband",
                "targetspeakingband",
                "examdate"
        );

        UserResponseDto result = userService.getById(userId, includes);

        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getEmail()).isEqualTo("john@example.com");
        assertThat(result.getPhoneNumber()).isEqualTo("0123456789");
        assertThat(result.getIsActive()).isTrue();
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getLastLoginAt()).isNotNull();
        assertThat(result.getRole()).isEqualTo("Learner");
        assertThat(result.getFirstname()).isEqualTo("John");
        assertThat(result.getLastname()).isEqualTo("Doe");
        assertThat(result.getCountry()).isEqualTo("Vietnam");
        assertThat(result.getTimezone()).isEqualTo("Asia/Ho_Chi_Minh");
        assertThat(result.getAvatarUrl()).isEqualTo("avatar.png");

        verify(userRepository).findById(userId);
    }

    @Test
    void getById_whenNoFieldsAreIncluded_shouldReturnOnlyUserId() {
        String userId = "user-1";
        User user = fullUser(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        mockIncludes();

        UserResponseDto result = userService.getById(userId, includes);

        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getEmail()).isNull();
        assertThat(result.getFirstname()).isNull();
        assertThat(result.getLastname()).isNull();
        assertThat(result.getCountry()).isNull();

        verify(userRepository).findById(userId);
    }

    @Test
    void getById_whenUserDoesNotExist_shouldThrowRuntimeException() {
        String userId = "missing-user";

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.getById(userId, includes)
        );

        assertThat(exception.getMessage()).isEqualTo("User not found with id: missing-user");

        verify(userRepository).findById(userId);
    }

    @Test
    void getAll_shouldReturnUsersWithIncludedFields() {
        User user1 = fullUser("user-1");
        user1.setEmail("john@example.com");
        user1.setFirstname("John");

        User user2 = fullUser("user-2");
        user2.setEmail("jane@example.com");
        user2.setFirstname("Jane");

        when(userRepository.findAll()).thenReturn(List.of(user1, user2));
        mockIncludes("email", "firstname");

        List<UserResponseDto> result = userService.getAll(includes);

        assertThat(result).hasSize(2);

        assertThat(result.get(0).getUserId()).isEqualTo("user-1");
        assertThat(result.get(0).getEmail()).isEqualTo("john@example.com");
        assertThat(result.get(0).getFirstname()).isEqualTo("John");
        assertThat(result.get(0).getLastname()).isNull();

        assertThat(result.get(1).getUserId()).isEqualTo("user-2");
        assertThat(result.get(1).getEmail()).isEqualTo("jane@example.com");
        assertThat(result.get(1).getFirstname()).isEqualTo("Jane");
        assertThat(result.get(1).getLastname()).isNull();

        verify(userRepository).findAll();
    }

    @Test
    void getAll_whenRepositoryReturnsEmptyList_shouldReturnEmptyList() {
        when(userRepository.findAll()).thenReturn(List.of());

        List<UserResponseDto> result = userService.getAll(includes);

        assertThat(result).isEmpty();

        verify(userRepository).findAll();
    }

    @Test
    void update_whenUserExistsAndDataIsValid_shouldUpdateAllSupportedFieldsAndReturnResponseDto() {
        String userId = "user-1";

        User existingUser = fullUser(userId);
        existingUser.setEmail("old@example.com");
        existingUser.setPhoneNumber("0111111111");
        existingUser.setPasswordHash("old-hashed-password");

        UserUpdateRequestDto request = fullUpdateRequest();

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("0222222222")).thenReturn(false);
        when(passwordEncoder.encode("NewPassword123")).thenReturn("new-hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponseDto result = userService.update(userId, request);

        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getEmail()).isEqualTo("new@example.com");
        assertThat(result.getPhoneNumber()).isEqualTo("0222222222");
        assertThat(result.getFirstname()).isEqualTo("New");
        assertThat(result.getLastname()).isEqualTo("User");
        assertThat(result.getCountry()).isEqualTo("Australia");
        assertThat(result.getTimezone()).isEqualTo("Australia/Sydney");
        assertThat(result.getAvatarUrl()).isEqualTo("new-avatar.png");
        assertThat(result.getIsActive()).isFalse();
        assertThat(result.getRole()).isEqualTo("Tutor");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getEmail()).isEqualTo("new@example.com");
        assertThat(savedUser.getPhoneNumber()).isEqualTo("0222222222");
        assertThat(savedUser.getFirstname()).isEqualTo("New");
        assertThat(savedUser.getLastname()).isEqualTo("User");
        assertThat(savedUser.getCountry()).isEqualTo("Australia");
        assertThat(savedUser.getTimezone()).isEqualTo("Australia/Sydney");
        assertThat(savedUser.getAvatarUrl()).isEqualTo("new-avatar.png");
        assertThat(savedUser.getPasswordHash()).isEqualTo("new-hashed-password");
        assertThat(savedUser.getIsActive()).isFalse();
        assertThat(savedUser.getRole()).isEqualTo("Tutor");

        verify(userRepository).findById(userId);
        verify(userRepository).existsByEmail("new@example.com");
        verify(userRepository).existsByPhoneNumber("0222222222");
        verify(passwordEncoder).encode("NewPassword123");
    }

    @Test
    void update_whenEmailAndPhoneAreUnchanged_shouldSkipDuplicateChecks() {
        String userId = "user-1";

        User existingUser = fullUser(userId);
        existingUser.setEmail("same@example.com");
        existingUser.setPhoneNumber("0999999999");

        UserUpdateRequestDto request = new UserUpdateRequestDto();
        request.setEmail("same@example.com");
        request.setPhoneNumber("0999999999");
        request.setFirstname("Updated");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponseDto result = userService.update(userId, request);

        assertThat(result.getEmail()).isEqualTo("same@example.com");
        assertThat(result.getPhoneNumber()).isEqualTo("0999999999");
        assertThat(result.getFirstname()).isEqualTo("Updated");

        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, never()).existsByPhoneNumber(anyString());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void update_whenRequestContainsOnlyNullFields_shouldKeepExistingValuesAndStillSave() {
        String userId = "user-1";

        User existingUser = fullUser(userId);
        existingUser.setEmail("old@example.com");
        existingUser.setPhoneNumber("0111111111");
        existingUser.setFirstname("Old");
        existingUser.setLastname("Name");
        existingUser.setPasswordHash("old-hashed-password");

        UserUpdateRequestDto request = new UserUpdateRequestDto();

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponseDto result = userService.update(userId, request);

        assertThat(result.getEmail()).isEqualTo("old@example.com");
        assertThat(result.getPhoneNumber()).isEqualTo("0111111111");
        assertThat(result.getFirstname()).isEqualTo("Old");
        assertThat(result.getLastname()).isEqualTo("Name");

        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, never()).existsByPhoneNumber(anyString());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository).save(existingUser);
    }

    @Test
    void update_whenPasswordIsBlank_shouldNotEncodePassword() {
        String userId = "user-1";

        User existingUser = fullUser(userId);
        existingUser.setPasswordHash("old-hashed-password");

        UserUpdateRequestDto request = new UserUpdateRequestDto();
        request.setPassword("   ");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.update(userId, request);

        assertThat(existingUser.getPasswordHash()).isEqualTo("old-hashed-password");

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository).save(existingUser);
    }

    @Test
    void update_whenUserDoesNotExist_shouldThrowRuntimeException() {
        String userId = "missing-user";

        UserUpdateRequestDto request = new UserUpdateRequestDto();
        request.setFirstname("New");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.update(userId, request)
        );

        assertThat(exception.getMessage()).isEqualTo("User not found with id: missing-user");

        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void update_whenNewEmailAlreadyExists_shouldThrowRuntimeException() {
        String userId = "user-1";

        User existingUser = new User();
        existingUser.setUserId(userId);
        existingUser.setEmail("old@example.com");

        UserUpdateRequestDto request = new UserUpdateRequestDto();
        request.setEmail("taken@example.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.update(userId, request)
        );

        assertThat(exception.getMessage()).isEqualTo("Email already in use: taken@example.com");

        verify(userRepository).findById(userId);
        verify(userRepository).existsByEmail("taken@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void update_whenNewPhoneAlreadyExists_shouldThrowRuntimeException() {
        String userId = "user-1";

        User existingUser = new User();
        existingUser.setUserId(userId);
        existingUser.setEmail("old@example.com");
        existingUser.setPhoneNumber("0111111111");

        UserUpdateRequestDto request = new UserUpdateRequestDto();
        request.setPhoneNumber("0222222222");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByPhoneNumber("0222222222")).thenReturn(true);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.update(userId, request)
        );

        assertThat(exception.getMessage()).isEqualTo("Phone already in use: 0222222222");

        verify(userRepository).findById(userId);
        verify(userRepository).existsByPhoneNumber("0222222222");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void delete_whenUserExists_shouldClearRelatedReferencesFlushAndDeleteUser() {
        String userId = "user-1";

        User user = new User();
        user.setUserId(userId);
        user.setEmail("john@example.com");

        Object practiceSubmission = addMockRelation(user, "getPracticeSubmissions");
        Object progress = addMockRelation(user, "getPracticeContentProgresses");
        Object feedback = addMockRelation(user, "getSubmissionFeedbacks");
        Object studyPlan = addMockRelation(user, "getLearnerStudyPlans");
        Object analytics = addMockRelation(user, "getSubmissionAnalytics");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.delete(userId);

        verifySetterCalledWithNull(practiceSubmission, "setUserId");
        verifySetterCalledWithNull(practiceSubmission, "setUser");

        verifySetterCalledWithNull(progress, "setUserId");
        verifySetterCalledWithNull(progress, "setUser");

        verifySetterCalledWithNull(feedback, "setAuthor");
        verifySetterCalledWithNull(studyPlan, "setUser");
        verifySetterCalledWithNull(analytics, "setUser");

        verify(userRepository).findById(userId);
        verify(userRepository).flush();
        verify(userRepository).delete(user);
    }

    @Test
    void delete_whenUserDoesNotExist_shouldThrowRuntimeException() {
        String userId = "missing-user";

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.delete(userId)
        );

        assertThat(exception.getMessage()).isEqualTo("User not found with id: missing-user");

        verify(userRepository).findById(userId);
        verify(userRepository, never()).flush();
        verify(userRepository, never()).delete(any(User.class));
    }

    private UserCreateRequestDto createRequest() {
        UserCreateRequestDto request = new UserCreateRequestDto();

        request.setEmail("john@example.com");
        request.setPhoneNumber("0123456789");
        request.setPassword("Password123");
        request.setFirstname("John");
        request.setLastname("Doe");
        request.setCountry("Vietnam");
        request.setTimezone("Asia/Ho_Chi_Minh");
        request.setAvatarUrl("avatar.png");
        request.setRole("Learner");

        setIfPresent(request, "setTargetBand", 7.5);
        setIfPresent(request, "setExamDate", LocalDate.of(2026, 6, 1));

        return request;
    }

    private UserUpdateRequestDto fullUpdateRequest() {
        UserUpdateRequestDto request = new UserUpdateRequestDto();

        request.setEmail("new@example.com");
        request.setPhoneNumber("0222222222");
        request.setFirstname("New");
        request.setLastname("User");
        request.setCountry("Australia");
        request.setTimezone("Australia/Sydney");
        request.setAvatarUrl("new-avatar.png");
        request.setRole("Tutor");
        request.setPassword("NewPassword123");
        request.setIsActive(false);

        setIfPresent(request, "setGender", "MALE");
        setIfPresent(request, "setDateOfBirth", LocalDate.of(2000, 1, 1));
        setIfPresent(request, "setTargetBand", 8.0);
        setIfPresent(request, "setTargetListeningBand", 8.0);
        setIfPresent(request, "setTargetReadingBand", 8.0);
        setIfPresent(request, "setTargetWritingBand", 7.5);
        setIfPresent(request, "setTargetSpeakingBand", 7.5);
        setIfPresent(request, "setExamDate", LocalDate.of(2026, 6, 1));

        return request;
    }

    private User fullUser(String userId) {
        User user = new User();

        user.setUserId(userId);
        user.setEmail("john@example.com");
        user.setPhoneNumber("0123456789");
        user.setFirstname("John");
        user.setLastname("Doe");
        user.setCountry("Vietnam");
        user.setTimezone("Asia/Ho_Chi_Minh");
        user.setAvatarUrl("avatar.png");
        user.setRole("Learner");
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        user.setLastLoginAt(LocalDateTime.of(2026, 1, 2, 10, 0));

        setIfPresent(user, "setGender", "MALE");
        setIfPresent(user, "setDateOfBirth", LocalDate.of(2000, 1, 1));
        setIfPresent(user, "setTargetBand", 7.5);
        setIfPresent(user, "setTargetListeningBand", 7.5);
        setIfPresent(user, "setTargetReadingBand", 7.5);
        setIfPresent(user, "setTargetWritingBand", 7.5);
        setIfPresent(user, "setTargetSpeakingBand", 7.5);
        setIfPresent(user, "setExamDate", LocalDate.of(2026, 6, 1));

        return user;
    }

    private void mockIncludes(String... fieldsToInclude) {
        Set<String> includedFields = Set.copyOf(Arrays.asList(fieldsToInclude));

        when(includes.has(anyString())).thenAnswer(invocation -> {
            String fieldName = invocation.getArgument(0);
            return includedFields.contains(fieldName);
        });
    }

    private void setIfPresent(Object target, String setterName, Object value) {
        Method setter = Arrays.stream(target.getClass().getMethods())
                .filter(method -> method.getName().equals(setterName))
                .filter(method -> method.getParameterCount() == 1)
                .findFirst()
                .orElse(null);

        if (setter == null) {
            return;
        }

        try {
            Object coercedValue = coerceValue(setter.getParameterTypes()[0], value);
            setter.invoke(target, coercedValue);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to call " + setterName, exception);
        }
    }

    private Object coerceValue(Class<?> targetType, Object value) {
        if (value == null) {
            return null;
        }

        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }

        if (targetType == String.class) {
            return String.valueOf(value);
        }

        if (targetType == Boolean.class || targetType == boolean.class) {
            return Boolean.valueOf(String.valueOf(value));
        }

        if (targetType == Integer.class || targetType == int.class) {
            return 1;
        }

        if (targetType == Long.class || targetType == long.class) {
            return 1L;
        }

        if (targetType == Double.class || targetType == double.class) {
            return 7.5;
        }

        if (targetType == Float.class || targetType == float.class) {
            return 7.5F;
        }

        if (targetType == BigDecimal.class) {
            return BigDecimal.valueOf(7.5);
        }

        if (targetType == LocalDate.class) {
            return LocalDate.of(2026, 6, 1);
        }

        if (targetType == LocalDateTime.class) {
            return LocalDateTime.of(2026, 1, 1, 10, 0);
        }

        if (targetType.isEnum()) {
            Object[] constants = targetType.getEnumConstants();
            return constants.length > 0 ? constants[0] : null;
        }

        return value;
    }

    @SuppressWarnings("unchecked")
    private Object addMockRelation(User user, String getterName) {
        try {
            Method getter = User.class.getMethod(getterName);
            Object collectionObject = getter.invoke(user);

            if (!(collectionObject instanceof Collection<?>)) {
                return null;
            }

            Class<?> relationClass = resolveCollectionItemType(getter);

            if (relationClass == null) {
                return null;
            }

            Object relationMock = mock(relationClass);

            ((Collection<Object>) collectionObject).add(relationMock);

            return relationMock;
        } catch (Exception exception) {
            return null;
        }
    }

    private Class<?> resolveCollectionItemType(Method getter) {
        Type genericReturnType = getter.getGenericReturnType();

        if (!(genericReturnType instanceof ParameterizedType parameterizedType)) {
            return null;
        }

        Type firstArgument = parameterizedType.getActualTypeArguments()[0];

        if (firstArgument instanceof Class<?>) {
            return (Class<?>) firstArgument;
        }

        if (firstArgument instanceof ParameterizedType nestedType
                && nestedType.getRawType() instanceof Class<?>) {
            return (Class<?>) nestedType.getRawType();
        }

        return null;
    }

    private void verifySetterCalledWithNull(Object relationMock, String setterName) {
        if (relationMock == null) {
            return;
        }

        Method setter = Arrays.stream(relationMock.getClass().getMethods())
                .filter(method -> method.getName().equals(setterName))
                .filter(method -> method.getParameterCount() == 1)
                .findFirst()
                .orElse(null);

        if (setter == null) {
            return;
        }

        try {
            setter.invoke(verify(relationMock), new Object[]{null});
        } catch (Exception exception) {
            throw new RuntimeException("Failed to verify " + setterName, exception);
        }
    }
}
