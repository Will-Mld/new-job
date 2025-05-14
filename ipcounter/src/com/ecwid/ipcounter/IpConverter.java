package com.ecwid.ipcounter;

import java.util.function.ToIntFunction;

public class IpConverter implements ToIntFunction<CharSequence> {
    @Override
    public int applyAsInt(CharSequence ipAddress) {
        int base = 0;
        int path = 0;
        for (int i = 0; i < ipAddress.length(); i++) {
            char symbol = ipAddress.charAt(i);
            if (symbol =='.') {
                base = (base<<Byte.SIZE) | path;
                path = 0;
            } else {
                path = path * 10 + symbol - '0';
            }
        }
        return (base<<Byte.SIZE) | path;
    }
}
