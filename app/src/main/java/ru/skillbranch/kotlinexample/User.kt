package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting
import java.lang.IllegalArgumentException
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom

class User private constructor(
    private val firstName: String,
    private val lastName: String?,
    email:String? = null,
    rawPhone:String? = null,
    meta : Map<String, Any>? = null
)
{

    val userInfo : String
    private val fullName : String
    get() = listOfNotNull(firstName,lastName)
        .joinToString (" ")
        .capitalize()

    private val initials:String
        get() = listOfNotNull(firstName,lastName)
            .map { it.first().toUpperCase() }
            .joinToString (" ")

    private var phone:String? = null
        set(value) {
            field = value?.replace("[^+\\d]".toRegex(), "")
        }

    private var _login:String? = null

    internal var login:String
        set(value) {
            _login = value.toLowerCase()
        }

        get() = _login!!

    private val salt :String by lazy {
        ByteArray(16).also { SecureRandom().nextBytes(it) }.toString()
    }
    private lateinit var passwordHash:String

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    var accessCode: String? = null

    //for email
    constructor(
        firstName: String,
        lastName: String?,
        email: String?,
        password:String
    ) : this(firstName, lastName, email = email, meta = mapOf("auth" to "password")){
        println("Secondary mail constructor")
        passwordHash = encrypt(password)


    }

    //for phone
    constructor(
        firstName: String,
        lastName: String?,
        rawPhone: String
    ) : this(firstName, lastName, rawPhone = rawPhone, meta = mapOf("auth" to "sms")){
        println("Secondary phone constructor")
        val code :String = generateAccessCode()
        passwordHash = encrypt(code)
        accessCode = code
        sendAccessCodeToUse(rawPhone, code)
    }


    init {
        println("First init block, primary constructor was called")

        check(!firstName.isBlank()){"FirstName must be not blank"}
        check(email.isNullOrBlank() || rawPhone.isNullOrBlank()) {"Email or phone must be not blank"}

        userInfo = """
        firstName:$firstName
        lastName:$lastName
        login:$login
        fullName:$fullName
        initials:$initials
        email:$email
        phone:$phone
        meta $meta
        
    """.trimIndent()

    }

    fun checkPassword(pass:String) = encrypt(pass) == passwordHash

    fun changePassword(oldPass:String, newPass:String){
        if(checkPassword(oldPass)) passwordHash = encrypt(newPass)
        else throw IllegalArgumentException("The entered password does not match the current password")
    }

   // private fun encrypt(password: String): String = password.md5() //don't do that
    private fun encrypt(password: String): String = salt.plus(password).md5() //good

    private fun String.md5() : String{
        val md: MessageDigest = MessageDigest.getInstance("MD5")
        val digest: ByteArray = md.digest(toByteArray())
        val hexString:String = BigInteger(1, digest).toString(16)
        return  hexString.padStart(32, '0')

    }
    private fun generateAccessCode(): String {
        val possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return StringBuilder().apply {
            repeat(6){
                (possible.indices).random().also { index ->
                    append(possible[index])
                }
            }
        }.toString()
    }
    private fun sendAccessCodeToUse(phone: String?, code: String) {
        println("..... sending access code $code on $phone")
    }

    companion object Factory{
        fun makeUser(
            fullName: String,
            email: String? = null,
            password: String? = null,
            phone:String? = null
        ): User{
            //if phone not null we will create new User
            val(firstName:String, lastName: String?) = fullName.fullNameToPair()
            return when{
                !phone.isNullOrBlank() -> User(firstName, lastName)
                email.isNullOrBlank() && !password.isNullOrBlank() -> User(firstName, lastName, email, password)
                else -> throw IllegalArgumentException("Email or phone must be not null or blank")

            }
        }
    }


}

private fun String.fullNameToPair(): Pair<String, String?> {
    //In pair we find value and return this value, else send a exception about contain last name and first name
    return this.split("")
        .filter { it.isNotBlank() }
        .run {
            when(size){
                1 -> first() to null
                2 -> first() to last()
                else -> throw IllegalArgumentException("Fullname must contain only first name" +
                "and lastname, current split result ${this@fullNameToPair}")
            }
        }


}
