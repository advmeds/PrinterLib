package com.advmeds.printerlib

public interface PrinterServiceDelegate {

    fun onStateChanged(state: State)

    public enum class State {
        /** we're doing nothing */
        NONE,

        /** now initiating an outgoing connection */
        CONNECTING,

        /** now connected to a remote device */
        CONNECTED;
    }
}