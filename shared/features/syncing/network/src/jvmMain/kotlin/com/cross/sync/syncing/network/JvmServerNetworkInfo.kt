package com.cross.sync.syncing.network

import java.net.InetAddress
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.NetworkInterface

object NetworkUtils {
    fun getIpAddress(): String {
        return getLanIpAddresses().firstOrNull() ?: "127.0.0.1"
    }

    fun getLanIpAddresses(): List<String> {
        return runCatching {
            NetworkInterface.getNetworkInterfaces()
                .toList()
                .asSequence()
                .filter { it.isUsableLanInterface() }
                .sortedWith(compareByDescending<NetworkInterface> { it.isPreferredLanInterface() }
                    .thenBy { it.index })
                .flatMap { iface ->
                    iface.inetAddresses.toList().asSequence()
                        .filterIsInstance<Inet4Address>()
                        .filter { it.isUsableLanAddress() }
                        .map { it.hostAddress }
                }
                .distinct()
                .toList()
        }.getOrDefault(emptyList())
    }

    fun getLinkLocalIPv6Addresses(): List<String> {
        val addresses = mutableListOf<String>()

        try {
            // Получаем все сетевые интерфейсы
            val interfaces = NetworkInterface.getNetworkInterfaces().toList()

            for (intf in interfaces) {
                // Отфильтровываем:
                // 1. Неактивные (down)
                // 2. Петлевые (localhost/127.0.0.1)
                // 3. Виртуальные (обычно они нам не нужны, но для VPN иногда приходится фильтровать жестче)
                if (!intf.isUp || intf.isLoopback) continue

                // Перебираем IP адреса на этом интерфейсе
                val inetAddresses = intf.inetAddresses.toList()

                for (addr in inetAddresses) {
                    // Нам нужен только IPv6 и только Link-Local (fe80::...)
                    if (addr is Inet6Address && addr.isLinkLocalAddress) {

                        // addr.hostAddress может вернуть что-то типа "fe80:0:0:0:abcd:1234:5678:ef90%en0"
                        // %en0 — это Scope ID (имя интерфейса), оно важно!
                        val fullAddress = addr.hostAddress

                        // Иногда в адресе есть лишние данные, чистим если нужно,
                        // но для Link-Local Scope ID (%en0) часто обязателен.
                        addresses.add(fullAddress)

                        println("Found Interface: ${intf.displayName} -> $fullAddress")
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return addresses
    }

    private fun NetworkInterface.isUsableLanInterface(): Boolean {
        if (!isUp || isLoopback || isPointToPoint || isVirtual) return false

        val id = listOf(name, displayName)
            .joinToString(" ")
            .lowercase()

        val blockedNames = listOf(
            "utun",
            "tun",
            "tap",
            "ppp",
            "ipsec",
            "wg",
            "wireguard",
            "tailscale",
            "zerotier",
            "zt",
            "docker",
            "bridge",
            "vbox",
            "vmnet",
            "awdl",
            "llw"
        )

        return blockedNames.none { it in id }
    }

    private fun NetworkInterface.isPreferredLanInterface(): Boolean {
        val id = listOf(name, displayName)
            .joinToString(" ")
            .lowercase()

        return listOf("en0", "en1", "wi-fi", "wifi", "wlan", "ethernet", "eth").any { it in id }
    }

    private fun Inet4Address.isUsableLanAddress(): Boolean {
        if (isAnyLocalAddress || isLoopbackAddress || isLinkLocalAddress || isMulticastAddress) {
            return false
        }

        val bytes = address.map { it.toInt() and 0xff }
        val first = bytes[0]
        val second = bytes[1]

        return first == 10 ||
            first == 192 && second == 168 ||
            first == 172 && second in 16..31
    }
}

class JvmServerNetworkInfo : ServerNetworkInfo {
    override fun addresses() = NetworkUtils.getLanIpAddresses()
    override fun name() = resolveServerName()
}

private fun resolveServerName(): String {
    val localHostName = runCatching { InetAddress.getLocalHost().hostName }.getOrNull()
    return localHostName
        ?.substringBefore('.')
        ?.takeIf { it.isNotBlank() }
        ?: "Mac"
}

