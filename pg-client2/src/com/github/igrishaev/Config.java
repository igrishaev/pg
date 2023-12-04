package com.github.igrishaev;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import javax.net.ssl.SSLContext;

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
        boolean iseSSL,
        boolean SOKeepAlive,
        boolean SOTCPnoDelay,
        int inStreamBufSize,
        int outStreamBufSize

) {

    public static class Builder {
        private final String user;
        private final String database;
        private String password = "";
        private int port = Const.PG_PORT;
        private String host = Const.PG_HOST;
        private int protocolVersion = Const.PROTOCOL_VERSION;
        private Map<String, String> pgParams = new HashMap<>();
        private boolean binaryEncode = false;
        private boolean binaryDecode = false;
        private boolean useSSL = false;
        private boolean SOKeepAlive = true;
        private boolean SOTCPnoDelay = true;
        private int inStreamBufSize = Const.IN_STREAM_BUF_SIZE;
        private int outStreamBufSize = Const.OUT_STREAM_BUF_SIZE;

        public Builder(String user, String database) {
            this.user = user;
            this.database = database;
        }

        public Builder protocolVersion(int protocolVersion) {
            this.protocolVersion = protocolVersion;
            return this;
        }

        public Builder binaryEncode(boolean binaryEncode) {
            this.binaryEncode = binaryEncode;
            return this;
        }

        public Builder binaryDecode(boolean binaryDecode) {
            this.binaryDecode = binaryEncode;
            return this;
        }

        public Builder useSSL(boolean useSSL) {
            this.useSSL = useSSL;
            return this;
        }

        public Builder pgParams(Map<String, String> pgParams) {
            this.pgParams.putAll(pgParams);
            return this;
        }

        public Builder pgParam(String param, String value) {
            this.pgParams.put(param, value);
            return this;
        }
        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder password (String password) {
            this.password = password;
            return this;
        }

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder SOKeepAlive(boolean SOKeepAlive) {
            this.SOKeepAlive = SOKeepAlive;
            return this;
        }

        public Builder SOTCPnoDelay(boolean SOTCPnoDelay) {
            this.SOTCPnoDelay = SOTCPnoDelay;
            return this;
        }

        public Builder inStreamBufSize(int inStreamBufSize) {
            this.inStreamBufSize = inStreamBufSize;
            return this;
        }

        public Builder outStreamBufSize(int outStreamBufSize) {
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
