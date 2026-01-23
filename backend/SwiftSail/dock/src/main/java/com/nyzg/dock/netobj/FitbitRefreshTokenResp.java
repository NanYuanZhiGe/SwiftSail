package com.nyzg.dock.netobj;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
//fitbit的json的返回格式是下划线格式，这里为了代码的一致性需求，转化为驼峰
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class FitbitRefreshTokenResp {
    String accessToken;
    int expiresIn;
    String refreshToken;
    String tokenType;//固定为 “Bearer”
    String userId;
}
