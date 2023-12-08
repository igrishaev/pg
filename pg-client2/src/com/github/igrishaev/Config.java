package com.github.igrishaev;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

public record Config (
        String user,
        String database,
        String password,
        int port,
        String host,
        int protocolVersion,
        Map<String, String> pgParams,
        boolean binaryEncode,
        boolean binaryDecode,
        boolean useSSL,
        boolean SOKeepAlive,
        boolean SOTCPnoDelay,
        int inStreamBufSize,
        int outStreamBufSize) {

    public static class Builder {
        private final String user;
        private final String database;
        private String password = "";
        private int port = Const.PG_PORT;
        private String host = Const.PG_HOST;
        private int protocolVersion = Const.PROTOCOL_VERSION;
        private final Map<String, String> pgParams = new HashMap<>();
        private boolean binaryEncode = false;
        private boolean binaryDecode = false;
        private boolean useSSL = false;
        private boolean SOKeepAlive = true;
        private boolean SOTCPnoDelay = true;
        private int inStreamBufSize = Const.IN_STREAM_BUF_SIZE;
        private int outStreamBufSize = Const.OUT_STREAM_BUF_SIZE;

        public Builder(@NotNull final String user, @NotNull final String database) {
            this.user = user;
            this.database = database;
            this.pgParams.put("client_encoding", Const.CLIENT_ENCODING);
            this.pgParams.put("application_name", Const.APP_NAME);
        }

        public Builder protocolVersion(final int protocolVersion) {
            this.protocolVersion = protocolVersion;
            return this;
        }

        public Builder binaryEncode(final boolean binaryEncode) {
            this.binaryEncode = binaryEncode;
            return this;
        }

        public Builder binaryDecode(final boolean binaryDecode) {
            this.binaryDecode = binaryEncode;
            return this;
        }

        public Builder useSSL(final boolean useSSL) {
            this.useSSL = useSSL;
            return this;
        }

        public Builder pgParams(@NotNull final Map<String, String> pgParams) {
            this.pgParams.putAll(pgParams);
            return this;
        }

        public Builder pgParam(@NotNull final String param, @NotNull final String value) {
            this.pgParams.put(param, value);
            return this;
        }
        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder password (@NotNull final String password) {
            this.password = password;
            return this;
        }

        public Builder host(@NotNull final String host) {
            this.host = host;
            return this;
        }

        public Builder SOKeepAlive(final boolean SOKeepAlive) {
            this.SOKeepAlive = SOKeepAlive;
            return this;
        }

        public Builder SOTCPnoDelay(final boolean SOTCPnoDelay) {
            this.SOTCPnoDelay = SOTCPnoDelay;
            return this;
        }

        public Builder inStreamBufSize(final int inStreamBufSize) {
            this.inStreamBufSize = inStreamBufSize;
            return this;
        }

        public Builder outStreamBufSize(final int outStreamBufSize) {
            this.outStreamBufSize = outStreamBufSize;
            return this;
        }

        public Config build() {
            return new Config(
                    this.user,
                    this.database,
                    this.password,
                    this.port,
                    this.host,
                    this.protocolVersion,
                    Collections.unmodifiableMap(this.pgParams),
                    this.binaryEncode,
                    this.binaryDecode,
                    this.useSSL,
                    this.SOKeepAlive,
                    this.SOTCPnoDelay,
                    this.inStreamBufSize,
                    this.outStreamBufSize
            );
        }
    }
}
