package com.nyzg.swiftsail.bean;

import com.nyzg.swiftsail.netobj.HttpResp;
import java.util.function.Consumer;

import okhttp3.Response;

public class GlobalFunction {
    public static void handleNetResp(Response response, Runnable onFail, Consumer<HttpResp> onSuccess) {
        if (response == null) {
            GlobalToast.SERVER_NOT_RESPONSE.run();
            onFail.run();
            return;
        }
        if (!response.isSuccessful()) {
            GlobalToast.RESPONSE_ERROR.accept(response.code());
            onFail.run();
            response.close();
            return;
        }
        try {
            HttpResp httpResp = JsonSerializer.deSerialize(response.body() != null ? response.body().string() : null, HttpResp.class);
            if (httpResp == null || !httpResp.isSuccess()) {
                GlobalToast.RESPONSE_NOT_SUCCESS.accept(httpResp == null ? null : httpResp.getMessage());
                onFail.run();
                response.close();
                return;
            }
            onSuccess.accept(httpResp);
        } catch (Exception e) {
            GlobalToast.CONTENT_UNACCEPTABLE.run();
        } finally {
            onFail.run();
            response.close();
        }
    }

}