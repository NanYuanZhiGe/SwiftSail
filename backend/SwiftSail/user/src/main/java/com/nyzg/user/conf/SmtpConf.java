package com.nyzg.user.conf;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class SmtpConf {
    @Value("${spring.mail.username}")
    String mailSender;
}
