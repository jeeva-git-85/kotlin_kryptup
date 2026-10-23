package com.kryptup.kryptup.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BlockchainModule {

    @Provides
    @Singleton
    fun provideWeb3j(): Web3j {
        // Public Ethereum Sepolia Testnet RPC Node
        return Web3j.build(HttpService("https://ethereum-sepolia-rpc.publicnode.com"))
    }
}
