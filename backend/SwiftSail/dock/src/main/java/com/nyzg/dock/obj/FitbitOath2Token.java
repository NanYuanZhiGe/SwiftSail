package com.nyzg.dock.obj;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;

@Data
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class FitbitOath2Token {
    String accessToken;
    int expiresIn;
    String refreshToken;
    //fitbit返回的scope的形式类似如下：“settings temperature activity”
    //需要拆成多个小字符串，方便使用
    @JsonDeserialize(using = ScopeDecode.class)
    String[] scope;
    String tokenType;
    String userId;

    private static class ScopeDecode extends JsonDeserializer<String[]> {
        @Override
        public String[] deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
            String encodedValue = jsonParser.getValueAsString();
            if (encodedValue == null) {
                return null;
            }
            return encodedValue.split(" ");
        }
    }
}
