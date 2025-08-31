package com.totrackit;

import io.micronaut.runtime.Micronaut;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;

@OpenAPIDefinition(
    info = @Info(
        title = "ToTrackIt API",
        version = "0.1",
        description = "Open-source SaaS platform for tracking and monitoring asynchronous processes",
        license = @License(name = "Apache 2.0", url = "https://www.apache.org/licenses/LICENSE-2.0"),
        contact = @Contact(url = "https://github.com/plein/ToTrackIt", name = "ToTrackIt")
    ),
    servers = {
        @Server(url = "http://localhost:8080", description = "Local development server")
    }
)
public class Application {

    public static void main(String[] args) {
        Micronaut.run(Application.class, args);
    }
}