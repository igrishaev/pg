package com.github.igrishaev;

import java.util.Map;
import javax.net.ssl.SSLContext;

public record Config (
        String user,
        String database,
        String password,
        int port,
        String host,
        int protocolVersion,
        Map<String, String> pgParams,
        Boolean binaryEncode,
        Boolean binaryDecode,
        Boolean isSSL,
        SSLContext sslContext,
        Map<String, Object> socketOptions) {
    
}
