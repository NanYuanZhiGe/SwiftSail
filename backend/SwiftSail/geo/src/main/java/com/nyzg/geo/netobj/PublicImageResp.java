package com.nyzg.geo.netobj;


import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PublicImageResp {
    String fileName;


    public PublicImageResp(String fileName) {
        this.fileName = fileName;
    }
}

