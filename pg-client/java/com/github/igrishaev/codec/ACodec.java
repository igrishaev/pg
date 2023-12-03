package com.github.igrishaev.codec;

abstract class ACodec {

    String encoding = "UTF-8";
    String dateStyle = null;
    String timeZone = null;

    public void setEncoding (String encoding) {
        this.encoding = encoding;
    }

    public void setDateStyle (String dateStyle) {
        this.dateStyle = dateStyle;
    }

    public void setTimeZone (String timeZone) {
        this.timeZone = timeZone;
    }

}
