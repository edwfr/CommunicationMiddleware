package com.edoardo.bt_lib.util;

public class Bool {

    private Bool() {
    }

    public static boolean toBool(final int i) {
        return i == 1;
    }

    // 1 - TRUE
    // 0 - FALSE
    public static int fromBool(final boolean b) {
        return b ? 1 : 0;
    }

}
