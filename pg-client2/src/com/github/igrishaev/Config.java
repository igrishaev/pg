package com.github.igrishaev;

import clojure.lang.IFn;
import clojure.core$identity;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.Objects;

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
        int outStreamBufSize,
        IFn fnNotification,
        IFn fnProtocolVersion,
        IFn fnNotice
) {

    public static Builder builder (final String user, final String database) {
        return new Builder(user, database);
    }

    public static Config standard (final String user, final String database) {
        return builder(user, database).build();
    }

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
        private IFn fnNotification = new core$identity();
        private IFn fnProtocolVersion = new core$identity();
        private IFn fnNotice = new core$identity();

        public Builder(final String user, final String database) {
            this.user = Objects.requireNonNull(user);
            this.database = Objects.requireNonNull(database);
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

        public Builder fnNotification(final IFn fnNotification) {
            this.fnNotification = Objects.requireNonNull(fnNotification);
            return this;
        }

        public Builder fnNotice(final IFn fnNotice) {
            this.fnNotice = Objects.requireNonNull(fnNotice);
            return this;
        }

        public Builder fnProtocolVersion(final IFn fnProtocolVersion) {
            this.fnProtocolVersion = Objects.requireNonNull(fnProtocolVersion);
            return this;
        }

        public Builder binaryDecode(final boolean binaryDecode) {
            this.binaryDecode = binaryDecode;
            return this;
        }

        public Builder useSSL(final boolean useSSL) {
            this.useSSL = useSSL;
            return this;
        }

        public Builder pgParams(final Map<String, String> pgParams) {
            this.pgParams.putAll(pgParams);
            return this;
        }

        public Builder pgParam(final String param, final String value) {
            this.pgParams.put(param, value);
            return this;
        }
        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder password (final String password) {
            this.password = password;
            return this;
        }

        public Builder host(final String host) {
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
                    this.outStreamBufSize,
                    this.fnNotification,
                    this.fnProtocolVersion,
                    this.fnNotice
            );
        }
    }
}
