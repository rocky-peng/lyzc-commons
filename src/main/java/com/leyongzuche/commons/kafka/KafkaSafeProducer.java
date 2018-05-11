package com.leyongzuche.commons.kafka;

import com.leyongzuche.commons.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * @author pengqingsong
 * 12/10/2017
 */
@Slf4j
public class KafkaSafeProducer {

    private KafkaTemplate kafkaTemplate;

    public KafkaSafeProducer(KafkaTemplate kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * 发送消息
     *
     * @param topic 向哪个topic发送数据
     * @param data  发送的数据
     */
    public void send(String topic, Object data) {
        try {
            kafkaTemplate.send(topic, data);
        } catch (Throwable e) {
            log.error("发送消息到Kafka失败,data:【" + JsonUtils.serialize(data) + "】", e);
        }
    }
}
