package id.nearme.app.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import id.nearme.app.domain.repository.ChatRepository
import id.nearme.app.domain.repository.ChatRepositoryImpl
import id.nearme.app.domain.repository.PostRepository
import id.nearme.app.domain.repository.PostRepositoryImpl
import id.nearme.app.domain.repository.UserRepository
import id.nearme.app.domain.repository.UserRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return Firebase.auth
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return Firebase.firestore
    }

    @Provides
    @Singleton
    fun providePostRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth,
        userRepository: UserRepository
    ): PostRepository {
        return PostRepositoryImpl(firestore, auth, userRepository)
    }

    @Provides
    @Singleton
    fun provideUserRepository(
        auth: FirebaseAuth
    ): UserRepository {
        return UserRepositoryImpl(auth)
    }
    
    @Provides
    @Singleton
    fun provideChatRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth,
        userRepository: UserRepository
    ): ChatRepository {
        return ChatRepositoryImpl(firestore, auth, userRepository)
    }
}