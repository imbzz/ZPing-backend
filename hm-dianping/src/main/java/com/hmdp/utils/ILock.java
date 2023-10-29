package com.hmdp.utils;

/**
 * @author imbzz
 * @Date 2023/10/24 11:40
 */
public interface ILock {

    boolean tryLock(long timeOutSec);

    void unlock();
}
