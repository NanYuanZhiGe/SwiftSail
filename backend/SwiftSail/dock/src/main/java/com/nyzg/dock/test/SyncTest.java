package com.nyzg.dock.test;

import com.nyzg.common.netobj.HttpResp;
import com.nyzg.dock.controller.SyncController;
import com.nyzg.dock.netobj.GetDataDayReq;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

@SpringBootTest
@Slf4j
public class SyncTest {
    @Resource
    SyncController syncController;
    @Test
    public void testGetDay() {
        GetDataDayReq getDataDayReq=new GetDataDayReq();
        getDataDayReq.setDay(LocalDate.now().toEpochDay());
        HttpResp resp=syncController.getDataDay("1994999817200902144", getDataDayReq);
        log.info(resp.toString());
    }
}
