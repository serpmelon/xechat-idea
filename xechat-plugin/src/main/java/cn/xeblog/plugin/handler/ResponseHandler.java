package cn.xeblog.plugin.handler;

import cn.xeblog.commons.entity.Response;
import cn.xeblog.plugin.factory.MessageHandlerFactory;
import lombok.AllArgsConstructor;

/**
 * @author anlingyi
 * @date 2020/8/19
 */
@AllArgsConstructor
public class ResponseHandler {

    private Response response;

    public void exec() {
        MessageHandlerFactory.INSTANCE.produce(response.getType()).handle(response);
    }

}