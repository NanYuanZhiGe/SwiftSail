package com.nyzg.user.netobj;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class RegisterVerifyMailReq extends RegisterUserReq{
    String code;
}
