package ru.skillbranch.kotlinexample

import android.provider.ContactsContract

object UserHolder {
    private val map = mutableMapOf<String, User >()

    fun registerUser(
        fullName:String,
        email: String,
        password: String
    ): User{
        return User.makeUser(fullName, email = email, password = password)
            .also { user -> map[user.login] = user }
    }

    fun loginUser(login:String, password: String): String? {
        return map[login.trim()]?.run{
            if (checkPassword(password)) this.userInfo
            else null
        }
    }

    fun registerUserByPhone(
        fullName: String,
        rawPhone: String
    ) : User{
        return User.makeUser(fullName,rawPhone)
    }

    fun  requestAccessCode(login: String, password: String
    ) : String? {
        return map[login.trim()]?.run{
            if (checkPassword(password)) this.userInfo
            else null
        }
    }
}