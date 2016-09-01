package com.example.smital.itaat.data;

import com.example.smital.itaat.data.remote.GithubUserRestService;
import com.example.smital.itaat.data.remote.User;
import com.example.smital.itaat.data.remote.UsersList;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.adapter.rxjava.HttpException;
import rx.Observable;
import rx.observers.TestSubscriber;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.junit.Assert.*;

/**
 * Created by smital on 30/08/16.
 */
public class UserRepositoryImplTest {

    private static final String USER_LOGIN_RIGGAROO = "riggaroo";
    private static final String USER_LOGIN_2_REBECCA = "rebecca";

    @Mock
    GithubUserRestService githubUserRestService;
    private UserRepository userRepository;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        userRepository = new UserRepositoryImpl(githubUserRestService);
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void searchUsers_200OkResponse_InvokesCorrectApiCalls() {
        //Given
        when(githubUserRestService.searchGithubUsers(anyString())).thenReturn(Observable.just(githubUserList()));
        when(githubUserRestService.getUser(anyString()))
                .thenReturn(Observable.just(user1FullDetails()), Observable.just(user2FullDetails()));

        //When
        TestSubscriber<List<User>> subscriber = new TestSubscriber<>();
        userRepository.searchUsers(USER_LOGIN_RIGGAROO).subscribe(subscriber);

        //Then
        subscriber.awaitTerminalEvent();
        subscriber.assertNoErrors();

        List<List<User>> onNextEvents = subscriber.getOnNextEvents();
        List<User> users = onNextEvents.get(0);
        Assert.assertEquals(USER_LOGIN_RIGGAROO, users.get(0).getLogin());
        Assert.assertEquals(USER_LOGIN_2_REBECCA, users.get(1).getLogin());
        verify(githubUserRestService).searchGithubUsers(USER_LOGIN_RIGGAROO);
        verify(githubUserRestService).getUser(USER_LOGIN_RIGGAROO);
        verify(githubUserRestService).getUser(USER_LOGIN_2_REBECCA);
    }


    private UsersList githubUserList() {
        User user = new User(USER_LOGIN_RIGGAROO);
        User user2 = new User(USER_LOGIN_2_REBECCA);

        List<User> githubUsers = new ArrayList<>();
        githubUsers.add(user);
        githubUsers.add(user2);
        UsersList usersList = new UsersList(githubUsers);
        return usersList;
    }

    private User user1FullDetails() {
        User user = new User(USER_LOGIN_RIGGAROO, "Rigs Franks", "avatar_url", "Bio1");
        return user;
    }

    private User user2FullDetails() {
        User user = new User(USER_LOGIN_2_REBECCA, "Rebecca Franks", "avatar_url2", "Bio2");
        return user;
    }

    @Test
    public void searchUsers_GetUserIOExceptionThenSuccess_SearchUsersRetried() {
        //Given
        when(githubUserRestService.searchGithubUsers(anyString())).thenReturn(Observable.just(githubUserList()));
        when(githubUserRestService.getUser(anyString()))
                .thenReturn(getIOExceptionError(), Observable.just(user1FullDetails()),
                        Observable.just(user2FullDetails()));

        //When
        TestSubscriber<List<User>> subscriber = new TestSubscriber<>();
        userRepository.searchUsers(USER_LOGIN_RIGGAROO).subscribe(subscriber);

        //Then
        subscriber.awaitTerminalEvent();
        subscriber.assertNoErrors();

        verify(githubUserRestService, times(2)).searchGithubUsers(USER_LOGIN_RIGGAROO);

        verify(githubUserRestService, times(2)).getUser(USER_LOGIN_RIGGAROO);
        verify(githubUserRestService).getUser(USER_LOGIN_2_REBECCA);
    }


    @Test
    public void searchUsers_OtherHttpError_SearchTerminatedWithError() {
        //Given
        when(githubUserRestService.searchGithubUsers(anyString())).thenReturn(get403ForbiddenError());

        //When
        TestSubscriber<List<User>> subscriber = new TestSubscriber<>();
        userRepository.searchUsers(USER_LOGIN_RIGGAROO).subscribe(subscriber);

        //Then
        subscriber.awaitTerminalEvent();
        subscriber.assertError(HttpException.class);

        verify(githubUserRestService).searchGithubUsers(USER_LOGIN_RIGGAROO);

        verify(githubUserRestService, never()).getUser(USER_LOGIN_RIGGAROO);
        verify(githubUserRestService, never()).getUser(USER_LOGIN_2_REBECCA);
    }


    private Observable getIOExceptionError() {
        return Observable.error(new IOException());
    }

    private Observable<UsersList> get403ForbiddenError() {
        return Observable.error(new HttpException(
                Response.error(403, ResponseBody.create(MediaType.parse("application/json"), "Forbidden"))));

    }

}