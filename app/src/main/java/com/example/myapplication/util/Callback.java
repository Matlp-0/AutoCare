package com.example.myapplication.util;

/** Callback simples para operações assíncronas do repositório. */
public interface Callback<T> {

    void onResult(T result);
}
