package com.mparsict.vosolmaliat.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TestRegistrationRequest(
        @NotBlank(message = "نام الزامی است")
        @Size(max = 80, message = "نام نباید بیشتر از ۸۰ کاراکتر باشد")
        String firstName,

        @NotBlank(message = "نام خانوادگی الزامی است")
        @Size(max = 100, message = "نام خانوادگی نباید بیشتر از ۱۰۰ کاراکتر باشد")
        String lastName,

        @NotBlank(message = "کد ملی الزامی است")
        @Pattern(regexp = "\\d{10}", message = "کد ملی باید دقیقاً ۱۰ رقم باشد")
        String nationalCode,

        @NotBlank(message = "شماره موبایل الزامی است")
        @Pattern(regexp = "09\\d{9}", message = "شماره موبایل باید ۱۱ رقم و با ۰۹ شروع شود")
        String phoneNumber,

        @NotBlank(message = "رمز عبور الزامی است")
        @Size(min = 6, max = 128, message = "رمز عبور باید بین ۶ تا ۱۲۸ کاراکتر باشد")
        String password,

        @NotBlank(message = "تکرار رمز عبور الزامی است")
        String confirmationPassword
) {
}
