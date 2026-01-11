package com.blog.mcp

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class BlogMcpClientApplication

fun main(args: Array<String>) {
    runApplication<BlogMcpClientApplication>(*args)
}
