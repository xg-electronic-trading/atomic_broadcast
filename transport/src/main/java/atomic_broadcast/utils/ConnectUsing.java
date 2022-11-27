package atomic_broadcast.utils;

public enum ConnectUsing {

    /**
     * use when there is a dedicated multicast network available for transport
     */
    Multicast,

    /**
     * use when multicast not available and would like to mimic multicast via multi-destination cast.
     * i.e. the transport layer has to mimic multicast by sending message to each recipient via udp
     * unicast.
     */

    Unicast,

    /**
     * Use when app would like to send or receive messages to a sequencer on the same host.
     * e.g. when latency critical apps need to be on same host as sequencer.
     * e.g. Smart Order Router
     */
    Ipc,

    /**
     * Use when a client app would like to tail a recording/journal/queue instead of subscription to a live
     * network subscription and avoid participating in flow and congestion control.
     *
     * e.g. slow consumers such as view-servers, db-writers (e.g. influx and kdb)
     */

    Journal
}
