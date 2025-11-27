package com.nyzg.swiftsail.dbobj;


import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "userTable")
public class User implements Parcelable {
    @PrimaryKey
    long id;
    String nickName;
    String email;

    String token;

    public User() {
    }

    protected User(Parcel in) {
        id = in.readLong();
        nickName = in.readString();
        email = in.readString();
        token = in.readString();
    }

    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            User user = new User();
            user.id = in.readLong();
            user.nickName = in.readString();
            user.email = in.readString();
            user.token = in.readString();
            return user;
        }

        @Override
        public User[] newArray(int i) {
            return new User[i];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }


    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        parcel.writeLong(id);
        parcel.writeString(nickName);
        parcel.writeString(email);
        parcel.writeString(token);
    }

    @NonNull
    @Override
    public String toString() {
        return "{" + id + "," + nickName + "," + email + "," + token + "}";
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}