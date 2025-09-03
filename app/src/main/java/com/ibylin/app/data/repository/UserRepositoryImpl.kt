package com.ibylin.app.data.repository

import com.ibylin.app.data.local.UserDao
import com.ibylin.app.data.model.User
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao
) : UserRepository {
    
    override fun getUsers(): Flow<List<User>> = userDao.getAllUsers()
    
    override suspend fun getUserById(id: Int): User? = userDao.getUserById(id)
    
    override suspend fun insertUser(user: User) = userDao.insertUser(user)
    
    override suspend fun deleteUser(user: User) = userDao.deleteUser(user)
}
