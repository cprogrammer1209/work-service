package com.projecty.work.external.broker.dto;

import java.util.Map;

/**
 * One message for the broker.
 * @param topic where it goes
 * @param key events with the same key stay in order
 * @param value the message body (JSON)
 * @param headers small metadata so consumers can filter without parsing the body
 */
public record BrokerMessageDTO(String topic, String key, String value, Map<String, String> headers) {
}
