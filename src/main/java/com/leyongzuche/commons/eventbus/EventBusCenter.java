package com.leyongzuche.commons.eventbus;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.leyongzuche.commons.utils.JsonUtils;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * @author pengqingsong
 * 27/01/2018
 */
@Slf4j
public class EventBusCenter implements InitializingBean {

    @Setter
    private Executor executor;

    @Setter
    private List<Object> observers;

    private EventBus eventBus;

    public EventBusCenter(Executor executor, List<Object> observers) {
        this.executor = executor;
        this.observers = observers;
        init();
    }

    public EventBusCenter() {

    }

    private void init() {
        eventBus = new AsyncEventBus(this.executor);
        for (Object observer : observers) {
            eventBus.register(observer);
        }
    }


    public void publishEvent(BaseEvent event) {
        try {
            eventBus.post(event);
        } catch (Exception e) {
            log.error("发布事件失败[event:" + JsonUtils.serialize(event) + "]", e);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        init();
    }
}
