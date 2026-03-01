package com.nyzg.geo.netobj;

import jakarta.annotation.Nullable;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@NoArgsConstructor
public class PersonalInfo {
    @Nullable String appellation;
    @Nullable String description;
    short gender;

    @Nullable byte[] background;
    @Nullable List<byte[]> layouts;
}
