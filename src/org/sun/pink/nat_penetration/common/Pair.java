package org.sun.pink.nat_penetration.common;

/**
 * @author: SuricSun
 * @date: 2022/7/18
 */
public class Pair<T0, T1> {

    public T0 $0 = null;
    public T1 $1 = null;

    public Pair() {

    }

    public Pair(T0 cpnt0, T1 cpnt1) {

        this.$0 = cpnt0;
        this.$1 = cpnt1;
    }

    @Override
    public String toString() {
        return "Pair{" +
                "$0=" + $0 +
                ", $1=" + $1 +
                '}';
    }
}
