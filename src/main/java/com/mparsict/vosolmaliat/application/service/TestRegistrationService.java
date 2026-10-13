package com.mparsict.vosolmaliat.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mparsict.vosolmaliat.application.dto.request.TestRegistrationRequest;
import com.mparsict.vosolmaliat.application.dto.response.TestRegistrationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@ConditionalOnProperty(name = "app.test.registration.enabled", havingValue = "true")
public class TestRegistrationService {

    private static final Object FILE_LOCK = new Object();
    private static final Pattern LETTER = Pattern.compile("[A-Za-z\\p{IsArabic}]");
    private static final Pattern DIGIT = Pattern.compile("\\d");
    private static final Pattern REPEATED = Pattern.compile("^(.)\\1{5,}$");
    private static final Set<String> COMMON_PASSWORDS = Set.of(
            "123456", "1234567", "12345678", "123456789", "1234567890",
            "password", "password1", "qwerty", "qwerty123", "abcdef",
            "111111", "000000", "instagram", "admin123"
    );

    private final ObjectMapper objectMapper;
    private final Path outputPath;

    public TestRegistrationService(
            ObjectMapper objectMapper,
            @Value("${app.test.registration.output-path:./temp/test-registrations.txt}") String outputPath) {
        this.objectMapper = objectMapper;
        this.outputPath = Path.of(outputPath).toAbsolutePath().normalize();
    }

    public TestRegistrationResponse register(TestRegistrationRequest request) {
        validatePassword(request);
        OffsetDateTime registeredAt = OffsetDateTime.now(ZoneOffset.UTC);

        Map<String, Object> record = new LinkedHashMap<>();
        record.put("registeredAt", registeredAt);
        record.put("firstName", request.firstName().trim());
        record.put("lastName", request.lastName().trim());
        record.put("nationalCode", request.nationalCode());
        record.put("phoneNumber", request.phoneNumber());
        // Intentionally plain text for this explicitly enabled local test utility only.
        record.put("password", request.password());
        record.put("confirmationPassword", request.confirmationPassword());

        try {
            appendRecord(record);
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "ذخیره اطلاعات آزمایشی انجام نشد",
                    exception
            );
        }

        return new TestRegistrationResponse(true, "ثبت‌نام آزمایشی با موفقیت ذخیره شد", registeredAt);
    }

    private void validatePassword(TestRegistrationRequest request) {
        String password = request.password();
        if (!password.equals(request.confirmationPassword())) {
            throw badRequest("رمز عبور و تکرار آن یکسان نیستند");
        }
        if (password.isBlank() || password.chars().anyMatch(Character::isWhitespace)) {
            throw badRequest("رمز عبور نباید شامل فاصله باشد");
        }
        if (!LETTER.matcher(password).find() || !DIGIT.matcher(password).find()) {
            throw badRequest("رمز عبور باید حداقل یک حرف و یک عدد داشته باشد");
        }

        String normalized = password.toLowerCase(Locale.ROOT);
        if (COMMON_PASSWORDS.contains(normalized)
                || REPEATED.matcher(normalized).matches()
                || containsSimpleSequence(normalized)) {
            throw badRequest("این رمز عبور بیش از حد ساده یا قابل حدس است");
        }

        String firstName = request.firstName().trim().toLowerCase(Locale.ROOT);
        String lastName = request.lastName().trim().toLowerCase(Locale.ROOT);
        if ((!firstName.isEmpty() && normalized.contains(firstName))
                || (!lastName.isEmpty() && normalized.contains(lastName))
                || normalized.contains(request.nationalCode())
                || normalized.contains(request.phoneNumber())) {
            throw badRequest("رمز عبور نباید شامل اطلاعات شخصی شما باشد");
        }
    }

    private boolean containsSimpleSequence(String password) {
        return password.contains("123456")
                || password.contains("654321")
                || password.contains("abcdef")
                || password.contains("qwerty");
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private void appendRecord(Map<String, Object> record) throws IOException {
        synchronized (FILE_LOCK) {
            Path parent = outputPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            String line = objectMapper.writeValueAsString(record) + System.lineSeparator();
            Files.writeString(
                    outputPath,
                    line,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        }
    }
}
