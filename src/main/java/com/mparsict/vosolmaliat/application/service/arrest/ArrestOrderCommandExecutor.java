package com.mparsict.vosolmaliat.application.service.arrest;

import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.sql.SQLException;
import java.util.function.Supplier;
import static org.springframework.http.HttpStatus.CONFLICT;

/** Retries complete transactions, never a write inside a failed transaction. */
@Component
public class ArrestOrderCommandExecutor {
    public <T> T execute(Supplier<T> command) {
        for (int attempt = 0; ; attempt++) {
            try { return command.get(); }
            catch (RuntimeException failure) {
                if (!sqliteContention(failure)) throw failure;
                if (attempt >= 5) throw new ResponseStatusException(CONFLICT,
                        "پایگاه داده در حال ثبت عملیات دیگری است؛ لحظاتی بعد دوباره تلاش کنید.");
                try { Thread.sleep(25L << attempt); }
                catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new ResponseStatusException(CONFLICT, "درخواست متوقف شد؛ وضعیت کار را بروزرسانی کنید.");
                }
            }
        }
    }
    private boolean sqliteContention(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql && (sql.getErrorCode() == 5 || sql.getErrorCode() == 6)) return true;
        }
        return false;
    }
}
