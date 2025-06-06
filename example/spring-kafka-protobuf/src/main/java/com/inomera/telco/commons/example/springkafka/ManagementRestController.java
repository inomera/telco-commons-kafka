package com.inomera.telco.commons.example.springkafka;

import com.inomera.telco.commons.springkafka.consumer.KafkaMessageConsumer;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * @author Serdar Kuzucu
 */
@RestController
@RequiredArgsConstructor
public class ManagementRestController {
    private static final Logger LOG = LoggerFactory.getLogger(ManagementRestController.class);

    private final ApplicationContext applicationContext;

    @GetMapping(value = "start-produce")
    public void startProtobuf() {
        LOG.info("start protobuf called");
        final SomeSender someSender = (SomeSender) applicationContext.getBean("someSender");
        someSender.publishRandomText();
    }

    @GetMapping(value = "start-consume")
    public void start() {
        LOG.info("start called");

        final KafkaMessageConsumer consumer1 = (KafkaMessageConsumer) applicationContext.getBean("consumer");
        consumer1.start();

        LOG.info("start completed");
    }

    @GetMapping(value = "stop-consume")
    public void stop() {
        LOG.info("stop called");

        final KafkaMessageConsumer consumer1 = (KafkaMessageConsumer) applicationContext.getBean("consumer");
        consumer1.stop();

        LOG.info("stop completed");
    }

    @GetMapping(value = "start-bulk-consume")
    public void startBulk() {
        LOG.info("start bulk called");

        final KafkaMessageConsumer bulkConsumer = (KafkaMessageConsumer) applicationContext.getBean("bulkConsumer");
        bulkConsumer.start();

        LOG.info("start bulk completed");
    }

    @GetMapping(value = "stop-bulk-consume")
    public void stopBulk() {
        LOG.info("stop bulk called");

        final KafkaMessageConsumer bulkConsumer = (KafkaMessageConsumer) applicationContext.getBean("bulkConsumer");
        bulkConsumer.stop();

        LOG.info("stop bulk completed");
    }
}
