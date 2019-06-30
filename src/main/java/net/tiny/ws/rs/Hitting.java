package net.tiny.ws.rs;

class Hitting<T> implements Comparable<Hitting<T>> {

    public static Hitting<Void> NOT_HIT = new Hitting<Void>();
    private Integer hit = -1;
    private final T target;

    private Hitting() {
        this.target = null;
    }

    public Hitting(int compare) {
        this(compare, null);
    }

    public Hitting(T target) {
        this(0, target);
    }

    public Hitting(int hit, T target) {
        this.hit = hit;
        this.target = target;
    }

    public T getTarget() {
        return target;
    }

    public <M> M getTarget(Class<M> classType) {
        return classType.cast(target);
    }

    public int getHit() {
        return this.hit;
    }

    public boolean hit() {
        return (null != target);
    }

    @Override
    public int compareTo(Hitting<T> target) {
        return hit.compareTo(target.hit);
    }
}