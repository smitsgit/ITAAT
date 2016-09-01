package com.example.smital.itaat.data;

import com.example.smital.itaat.data.remote.GithubUserRestService;
import com.example.smital.itaat.data.remote.User;
import com.example.smital.itaat.data.remote.UsersList;

import java.io.IOException;
import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by smital on 29/08/16.
 */
public class UserRepositoryImpl implements UserRepository {
    private GithubUserRestService githubUserRestService;

    public UserRepositoryImpl(GithubUserRestService githubUserRestService) {
        this.githubUserRestService = githubUserRestService;
    }

    @Override
    public Observable<List<User>> searchUsers(final String searchTerm) {
        /*return Observable.defer(() -> githubUserRestService.searchGithubUsers(searchTerm).concatMap(
                usersList -> Observable.from(usersList.getItems())
                        .concatMap(user -> githubUserRestService.getUser(user.getLogin())).toList()))
                .retryWhen(observable -> observable.flatMap(o -> {
                    if (o instanceof IOException) {
                        return Observable.just(null);
                    }
                    return Observable.error(o);
                }));*/


        return Observable.defer(new Func0<Observable<List<User>>>() {
            @Override
            public Observable<List<User>> call() {
                return (githubUserRestService.searchGithubUsers(searchTerm)).concatMap(new Func1<UsersList, Observable<? extends User>>() {
                    @Override
                    public Observable<? extends User> call(UsersList usersList) {
                        return (Observable.from(usersList.getItems()).concatMap(new Func1<User, Observable<? extends User>>() {
                            @Override
                            public Observable<? extends User> call(User user) {
                                return (githubUserRestService.getUser(user.getLogin()));
                            }
                        }));
                    }
                }).toList();
            }
        }).retryWhen(new Func1<Observable<? extends Throwable>, Observable<?>>() {
            @Override
            public Observable<?> call(Observable<? extends Throwable> observable) {
                return observable.flatMap(new Func1<Throwable, Observable<?>>() {
                    @Override
                    public Observable<?> call(Throwable throwable) {
                        if(throwable instanceof IOException){
                            return Observable.just(null);
                        }
                        else{
                            return Observable.error(throwable);
                        }
                    }
                });
            }
        });


    }




    Observable<UsersList> getUserList(String searchTerm){
        return githubUserRestService.searchGithubUsers(searchTerm);
    }


}
