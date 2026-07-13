package org.dqnylux.mincore.listeners;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerServerData;

/**
 * Desactiva el handshake de firma de chat del cliente y fuerza
 * enforceSecureChat(false) - se registra vía el EventManager de PacketEvents,
 * no vía el PluginManager de Bukkit, porque opera a nivel de paquete crudo.
 */
public class AntiSignatureListener extends PacketListenerAbstract {

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CHAT_SESSION_UPDATE
                || event.getPacketType() == PacketType.Play.Client.CHAT_ACK) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.SERVER_DATA) {
            WrapperPlayServerServerData wrapper = new WrapperPlayServerServerData(event);
            wrapper.setEnforceSecureChat(false);
        }
    }
}
