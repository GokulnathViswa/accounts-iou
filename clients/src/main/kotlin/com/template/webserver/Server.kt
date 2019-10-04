package com.template.webserver

import net.corda.client.jackson.JacksonSupport
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.Banner
import org.springframework.boot.SpringApplication
import org.springframework.boot.WebApplicationType.SERVLET
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter

/**
 * Our Spring Boot application.
 */
@SpringBootApplication
open class Server {
    /**
     * Spring Bean that binds a Corda Jackson object-mapper to HTTP message types used in Spring.
     */
    @Bean
    open fun mappingJackson2HttpMessageConverter(@Autowired rpcConnection: NodeRPCConnection): MappingJackson2HttpMessageConverter {
        val mapper = JacksonSupport.createDefaultMapper(rpcConnection.proxy)
        val converter = MappingJackson2HttpMessageConverter()
        converter.objectMapper = mapper
        return converter
    }
}

/**
 * Starts our Spring Boot application.
 */
fun main(args: Array<String>) {
    val app = SpringApplication(Server::class.java)
    app.setBannerMode(Banner.Mode.OFF)
    app.isWebEnvironment = true

//    app.run(*args)
    app.run("--server.port=10070", "--config.rpc.host=localhost", "--config.rpc.port=10006", "--config.rpc.username=user1", "--config.rpc.password=test")
    app.run("--server.port=10071", "--config.rpc.host=localhost", "--config.rpc.port=10009", "--config.rpc.username=user1", "--config.rpc.password=test")
//    app.run("--server.port=10072", "--config.rpc.host=localhost", "--config.rpc.port=10012", "--config.rpc.username=user1", "--config.rpc.password=test")
//    app.run("--server.port=10073", "--config.rpc.host=localhost", "--config.rpc.port=10015", "--config.rpc.username=user1", "--config.rpc.password=test")
//    app.run("--server.port=10073", "--config.rpc.host=localhost", "--config.rpc.port=10015", "--config.rpc.username=user1", "--config.rpc.password=test")
}

