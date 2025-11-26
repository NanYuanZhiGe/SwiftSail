package com.nyzg.dock.controller;

import com.nyzg.common.ss_utils.EncryptThreadSafe;
import com.nyzg.common.ss_utils.Tuple;
import com.nyzg.dock.service.FitbitWebApiService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@Slf4j
public class FitbitCallbackController {
    @Resource
    FitbitWebApiService fitbitWebApiService;

    @GetMapping("/oauth/code")
    public Tuple<String, String, String> getCode() {
        return EncryptThreadSafe.getBase64UrlSha256RandomString();
    }

    @GetMapping("/oauth/callback/fitbit")
    public void fitbitCallback(@RequestParam("code") String code) {
        log.info(code);
        fitbitWebApiService.getTokenAsync(
                "23TR5X",
                "bca864e095b403f3474a173e95bbe847",
                code,
                "a3rnf93hv2gjr3nsjennuh2gd3nc9tisnahdrgnwpc8u46iuukggbau9guffftjj"
        ).thenAccept(o -> {
            o.ifPresent(token -> {
                fitbitWebApiService.getHeartRateZones(
                        token.getAccessToken(),
                        token.getUserId(),
                        LocalDate.now()
                ).thenAccept(log::info);
            });
        });
    }
}
