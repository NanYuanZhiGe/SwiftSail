package com.nyzg.geo.netobj;

import jakarta.annotation.Nullable;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UpdatePersonalInfoResp {

    @Nullable String backgroundUrl;

    @Nullable String layoutUrl; //image-hash1|image-hash2|image-hash3
}
