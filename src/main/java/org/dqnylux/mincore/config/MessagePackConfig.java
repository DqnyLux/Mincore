package org.dqnylux.mincore.config;

import org.dqnylux.mincore.config.models.MessagePackCosmetic;

import java.util.LinkedHashMap;
import java.util.Map;

public class MessagePackConfig extends MincoreConfig {

    public Map<String, MessagePackCosmetic> items = new LinkedHashMap<>();
}
