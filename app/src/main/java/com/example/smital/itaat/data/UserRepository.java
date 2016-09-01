package com.example.smital.itaat.data;

import com.example.smital.itaat.data.remote.User;

import java.util.List;

import rx.Observable;

/**
 * Created by smital on 29/08/16.
 */
public interface UserRepository {
    Observable<List<User>> searchUsers(String searchTerm);
}
