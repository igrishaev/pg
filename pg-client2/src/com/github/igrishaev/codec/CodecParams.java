package com.github.igrishaev.codec;

public class CodecParams {

    public String clientEncoding = "UTF-8";
    public String serverEncoding = "UTF-8";
    public String timeZone = "UTC";
    public String dateStyle = "";
    public boolean integerDatetime = true;

    public static CodecParams standard () {
        return new CodecParams();
    }

}
