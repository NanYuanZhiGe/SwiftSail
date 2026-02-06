package com.nyzg.common;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

class LinearHashMap<K, V> {
    private int size = 0;
    private int cap;
    Object[] list;
    int[] positionList;

    public LinearHashMap(int initCap) {
        this.cap = initCap;
        list = new Object[initCap];
        positionList = new int[initCap];
        Arrays.fill(positionList, -1);
    }

    public LinearHashMap() {
        this.cap = 4;
        list = new Object[4];
        positionList = new int[4];
        Arrays.fill(positionList, -1);
    }

    @SuppressWarnings({"unchecked"})
    synchronized public void put(K k, V v) {
        if (size == cap) {//需要扩容
            int newCap = cap << 1;
            Object[] tempList = new Object[newCap];
            int[] newPositionList = new int[newCap];
            Arrays.fill(newPositionList, -1);
            int count = 0;
            for (int i = 0; i < cap; ++i) {
                int oldIndex = positionList[i];
                Object o = list[oldIndex];
                Pair<K, V> pair = (Pair<K, V>) o;
                int kHashCode = pair.getA().hashCode();
                int newIndex = kHashCode & (newCap - 1);
                for (int j = 0; j < newCap; ++j) {
                    int tempIndex = j + newIndex;
                    if (tempIndex >= newCap) {
                        tempIndex &= (newCap - 1);
                    }
                    if (tempList[tempIndex] == null) {
                        tempList[tempIndex] = o;
                        newPositionList[count] = tempIndex;
                        ++count;
                        break;
                    }
                }
            }
            positionList = newPositionList;
            list = tempList;
            cap = newCap;
        }
        int hashCode = k.hashCode();
        int index = hashCode & (cap - 1);
        int insertIndex = 0;
        for (int i = 0; i < cap; ++i) {
            int tempIndex = i + index;
            if (tempIndex >= cap) {
                tempIndex &= (cap - 1);
            }
            if (list[tempIndex] == null) {
                list[tempIndex] = new Pair<>(k, v);
                insertIndex = tempIndex;
                break;
            }
            Pair<K, V> pair = (Pair<K, V>) list[tempIndex];
            if (pair.getA().equals(k)) {
                pair.setB(v);
                return;
            }
        }
        //更新position，这个元素的position就是size
        positionList[size] = insertIndex;
        ++size;
    }

    @SuppressWarnings({"unchecked"})
    synchronized public void remove(K k) {
        int index = k.hashCode() & (cap - 1);
        for (int i = 0; i < cap; ++i) {
            int tempIndex = i + index;
            if (tempIndex >= cap) {
                tempIndex &= (cap - 1);
            }
            Pair<K, V> pair = (Pair<K, V>) list[tempIndex];
            if (pair == null) {
                continue;
            }
            if (pair.getA().equals(k)) {
                list[tempIndex] = null;
                --size;
                //所有元素往前移动
                for (int j = 0; j < cap; ++j) {
                    if (positionList[j] == tempIndex) {
                        int replaceIndex = j;
                        for (int l = j + 1; l < cap; ++l) {
                            positionList[replaceIndex] = positionList[l];
                            replaceIndex = l;
                            if (positionList[l] < 0) {
                                break;
                            }
                            if (l == cap - 1) {
                                positionList[l] = -1;
                            }
                        }
                        break;
                    }
                }
                return;
            }
        }
    }

    @SuppressWarnings({"unchecked"})
    synchronized public V getByKey(K k) {
        int index = k.hashCode() & (cap - 1);
        for (int i = 0; i < cap; ++i) {
            int tempIndex = i + index;
            if (tempIndex >= cap) {
                tempIndex &= (cap - 1);
            }
            Pair<K, V> pair = (Pair<K, V>) list[tempIndex];
            if (pair == null) {//从未添加过
                return null;
            }
            if (pair.getA().equals(k)) {
                return pair.getB();
            }
        }
        return null;
    }

    @SuppressWarnings({"unchecked"})
    synchronized public V getByIndex(int index) {
        if (index < 0 || index >= cap) {
            return null;
        }
        Pair<K, V> pair = (Pair<K, V>) this.list[index];
        return pair != null ? pair.getB() : null;
    }

    @SuppressWarnings({"unchecked"})
    synchronized public V getByPosition(int position) {
        if (position < 0 || position >= cap) {
            return null;
        }
        int index = positionList[position];
        if (index == -1) {
            return null;
        }
        return ((Pair<K, V>) list[index]).getB();
    }

    synchronized public void clear() {
        this.size = 0;
        Arrays.fill(list, null);
        Arrays.fill(positionList, -1);
    }
}