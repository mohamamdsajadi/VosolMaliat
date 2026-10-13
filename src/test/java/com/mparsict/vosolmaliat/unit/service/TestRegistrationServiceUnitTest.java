package com.mparsict.vosolmaliat.unit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mparsict.vosolmaliat.application.dto.request.TestRegistrationRequest;
import com.mparsict.vosolmaliat.application.dto.response.TestRegistrationResponse;
import com.mparsict.vosolmaliat.application.service.TestRegistrationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TestRegistrationServiceUnitTest {

    @TempDir
    Path tempDirectory;

    @Test
    void writesAllSubmittedFieldsIncludingPlainTextPasswordToConfiguredTestFile() throws Exception {
        Path output = tempDirectory.resolve("registrations.txt");
        TestRegistrationService service = new TestRegistrationService(
                new ObjectMapper().findAndRegisterModules(),
                output.toString()
        );
        TestRegistrationRequest request = request("A7!safe-test", "A7!safe-test");

        TestRegistrationResponse response = service.register(request);

        assertThat(response.saved()).isTrue();
        assertThat(Files.readString(output))
                .contains("\"firstName\":\"سارا\"")
                .contains("\"nationalCode\":\"0012345678\"")
                .contains("\"phoneNumber\":\"09121234567\"")
                .contains("\"password\":\"A7!safe-test\"")
                .contains("\"confirmationPassword\":\"A7!safe-test\"");
    }

    @Test
    void rejectsCommonOrSequentialPasswords() {
        TestRegistrationService service = service();

        assertThatThrownBy(() -> service.register(request("qwerty123", "qwerty123")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("بیش از حد ساده");
    }

    @Test
    void rejectsMismatchedConfirmationPassword() {
        TestRegistrationService service = service();

        assertThatThrownBy(() -> service.register(request("A7!safe-test", "different7!")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("یکسان نیستند");
    }

    private TestRegistrationService service() {
        return new TestRegistrationService(
                new ObjectMapper().findAndRegisterModules(),
                tempDirectory.resolve("registrations.txt").toString()
        );
    }

    private TestRegistrationRequest request(String password, String confirmationPassword) {
        return new TestRegistrationRequest(
                "سارا",
                "احمدی",
                "0012345678",
                "09121234567",
                password,
                confirmationPassword
        );
    }
}
