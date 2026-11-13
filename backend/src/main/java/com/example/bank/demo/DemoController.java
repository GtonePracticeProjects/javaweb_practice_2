package com.example.bank.demo;

import com.example.bank.common.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/** Endpoints that exist only so the front-end demos can show loading / cancel / retry behaviour. */
@RestController
@RequestMapping("/api/demo")
public class DemoController {

    /** Responds after {@code ms} milliseconds (max 5000). */
    @GetMapping("/slow")
    public Map<String, Object> slow(@RequestParam(defaultValue = "1500") long ms) throws InterruptedException {
        long wait = Math.max(0, Math.min(ms, 5000));
        Thread.sleep(wait);
        return Map.of("waitedMs", wait, "serverTime", LocalDateTime.now().toString());
    }

    /** Always answers with the given HTTP error status (400-599). */
    @GetMapping("/fail")
    public Map<String, Object> fail(@RequestParam(defaultValue = "500") int status) {
        HttpStatus s = status >= 400 && status <= 599 ? HttpStatus.resolve(status) : null;
        throw new BusinessException(s != null ? s : HttpStatus.INTERNAL_SERVER_ERROR,
                "데모용 오류 응답입니다. (status=" + status + ")");
    }
}
